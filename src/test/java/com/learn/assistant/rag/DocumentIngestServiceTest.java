package com.learn.assistant.rag;

import com.learn.assistant.config.RagConfig;
import com.learn.assistant.properties.RagProperties;
import com.learn.assistant.service.FileIngest;
import com.learn.assistant.service.IngestResult;
import com.learn.assistant.service.LibraryFile;
import com.learn.assistant.service.LibraryRemoval;
import com.learn.assistant.tool.ProjectPaths;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentIngestServiceTest {

    @Test
    void returnsZeroWhenNoPdf() {
        PdfDocumentSource source = mock(PdfDocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.load()).thenReturn(SourceRead.documents(List.of()));
        DocumentIngestService service = service(List.of(source), vectorStore, mock(JdbcTemplate.class));

        IngestResult result = service.ingest();

        assertEquals(0, result.added());
        assertEquals(0, result.skipped());
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void splitsAndStoresDocuments() {
        PdfDocumentSource source = mock(PdfDocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.load()).thenReturn(SourceRead.documents(
                List.of(new Document("第一句。第二句。第三句。第四句。第五句。"))));
        TokenTextSplitter splitter = splitter();
        DocumentIngestService service = new DocumentIngestService(List.of(source), splitter, vectorStore,
                mock(JdbcTemplate.class), new PgVectorStoreProperties(), contents -> Set.of(), null);

        int count = service.ingest().added();

        assertEquals(splitter.apply(List.of(new Document("第一句。第二句。第三句。第四句。第五句。"))).size(), count);
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == count));
    }

    @Test
    void mergesEveryDocumentSource() {
        DocumentSource first = mock(DocumentSource.class);
        DocumentSource second = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(first.load()).thenReturn(SourceRead.documents(List.of(new Document("第一句。第二句。"))));
        when(second.load()).thenReturn(SourceRead.documents(List.of(new Document("第三句。第四句。第五句。"))));
        TokenTextSplitter splitter = splitter();
        DocumentIngestService service = service(List.of(first, second), vectorStore, mock(JdbcTemplate.class));

        int count = service.ingest().added();

        List<Document> expected = splitter.apply(List.of(
                new Document("第一句。第二句。"),
                new Document("第三句。第四句。第五句。")));
        assertEquals(expected.size(), count);
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == count));
    }

    @Test
    void clearsVectorTableWithoutTouchingOtherSql() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update("DELETE FROM public.vector_store")).thenReturn(4);
        DocumentIngestService service = service(List.of(), mock(VectorStore.class), jdbcTemplate);

        assertEquals(4, service.clear());
        verify(jdbcTemplate).update("DELETE FROM public.vector_store");
    }

    @Test
    void rejectsUnsafeVectorTableName() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PgVectorStoreProperties properties = new PgVectorStoreProperties();
        properties.setTableName("vector_store;drop");
        DocumentIngestService service = new DocumentIngestService(List.of(), splitter(), mock(VectorStore.class),
                jdbcTemplate, properties, contents -> Set.of(), null);

        assertThrows(IllegalArgumentException.class, service::clear);
        verify(jdbcTemplate, never()).update(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deletesRowsThatAlreadyShareTheSameText() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        String sql = "DELETE FROM public.vector_store AS extra USING public.vector_store AS kept "
                + "WHERE extra.content = kept.content AND extra.ctid > kept.ctid";
        when(jdbcTemplate.update(sql)).thenReturn(3);
        DocumentSource source = mock(DocumentSource.class);
        when(source.load()).thenReturn(SourceRead.documents(List.of()));
        DocumentIngestService service = service(List.of(source), mock(VectorStore.class), jdbcTemplate);

        IngestResult result = service.ingest();

        assertEquals(0, result.added());
        verify(jdbcTemplate).update(sql);
    }

    @Test
    void keepsOneCopyWhenTheSameTextAppearsTwice() {
        DocumentSource source = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.load()).thenReturn(SourceRead.documents(
                List.of(new Document("同一段资料。"), new Document("同一段资料。"))));
        DocumentIngestService service = new DocumentIngestService(List.of(source), wideSplitter(), vectorStore,
                mock(JdbcTemplate.class), new PgVectorStoreProperties(), contents -> Set.of(), null);

        IngestResult result = service.ingest();

        assertEquals(1, result.added());
        assertEquals(1, result.skipped());
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == 1
                && "同一段资料。".equals(chunks.get(0).getText())
                && ChunkFingerprint.hash("同一段资料。").equals(chunks.get(0).getMetadata().get(ChunkFingerprint.METADATA_KEY))
                && chunks.get(0).getId().equals(ChunkFingerprint.stamp(new Document("同一段资料。")).getId())));
    }

    @Test
    void skipsTextAlreadyStoredFromAnotherDocument() {
        DocumentSource source = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.load()).thenReturn(SourceRead.documents(
                List.of(new Document("已经在库里。"), new Document("这次是新的。"))));
        DocumentIngestService service = new DocumentIngestService(List.of(source), wideSplitter(), vectorStore,
                mock(JdbcTemplate.class), new PgVectorStoreProperties(), contents -> Set.of("已经在库里。"), null);

        IngestResult result = service.ingest();

        assertEquals(1, result.added());
        assertEquals(1, result.skipped());
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == 1
                && "这次是新的。".equals(chunks.get(0).getText())));
    }

    @Test
    void replacesOlderChunksFromTheSameFile() {
        DocumentSource source = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(source.load()).thenReturn(SourceRead.documents(List.of(new Document("同一段资料。",
                Map.of(PdfDocumentSource.SOURCE_FILE, "课程/笔记.pdf")))));
        when(jdbcTemplate.update(contains("source_file"), eq("课程/笔记.pdf"))).thenReturn(2);
        DocumentIngestService service = new DocumentIngestService(List.of(source), wideSplitter(), vectorStore,
                jdbcTemplate, new PgVectorStoreProperties(), contents -> Set.of(), null);

        IngestResult result = service.ingest();

        assertEquals(1, result.added());
        verify(jdbcTemplate).update(contains("source_file"), eq("课程/笔记.pdf"));
    }

    @Test
    void reportsAddedSkippedAndFailedFiles() {
        DocumentSource source = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(source.load()).thenReturn(new SourceRead(
                List.of(new Document("这次是新的。", Map.of(PdfDocumentSource.SOURCE_FILE, "新.pdf")),
                        new Document("已经在库里。", Map.of(PdfDocumentSource.SOURCE_FILE, "旧.pdf"))),
                List.of(new SourceRead.Failure("坏.pdf", "无法读取"))));
        when(jdbcTemplate.update(contains("source_file"), eq("新.pdf"))).thenReturn(0);
        when(jdbcTemplate.update(contains("source_file"), eq("旧.pdf"))).thenReturn(0);
        DocumentIngestService service = new DocumentIngestService(List.of(source), wideSplitter(), vectorStore,
                jdbcTemplate, new PgVectorStoreProperties(), contents -> Set.of("已经在库里。"), null);

        IngestResult result = service.ingest();

        assertEquals(1, result.added());
        assertEquals(1, result.skipped());
        assertEquals(FileIngest.FAILED, result.files().get(0).status());
        assertEquals("坏.pdf", result.files().get(0).file());
        assertEquals(FileIngest.ADDED, file(result, "新.pdf").status());
        assertEquals(FileIngest.SKIPPED, file(result, "旧.pdf").status());
        assertEquals("内容已在资料库中", file(result, "旧.pdf").message());
        verify(jdbcTemplate, never()).update(contains("source_file"), eq("坏.pdf"));
    }

    @Test
    void skipsAFileWhoseChunksAreUnchanged() {
        DocumentSource source = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(source.load()).thenReturn(SourceRead.documents(List.of(new Document("同一段资料。",
                Map.of(PdfDocumentSource.SOURCE_FILE, "课程/笔记.pdf")))));
        when(jdbcTemplate.query(contains("chunkHash"), any(RowMapper.class), eq("课程/笔记.pdf")))
                .thenReturn(List.of(ChunkFingerprint.hash("同一段资料。")));
        DocumentIngestService service = new DocumentIngestService(List.of(source), wideSplitter(), vectorStore,
                jdbcTemplate, new PgVectorStoreProperties(), contents -> Set.of(), null);

        IngestResult result = service.ingest();

        assertEquals(0, result.added());
        assertEquals(1, result.skipped());
        assertEquals(FileIngest.SKIPPED, result.files().get(0).status());
        assertEquals("内容没有变化", result.files().get(0).message());
        verify(vectorStore, never()).add(any());
        verify(jdbcTemplate, never()).update(contains("source_file"), eq("课程/笔记.pdf"));
    }

    @Test
    void rejectsAFileThatIsNotPdf(@TempDir Path tempDir) {
        DocumentIngestService service = libraryService(tempDir, mock(VectorStore.class), mock(JdbcTemplate.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.store("笔记.txt", "%PDF-1.4".getBytes(StandardCharsets.US_ASCII)));

        assertEquals("只接受 PDF 文件", exception.getMessage());
    }

    @Test
    void keepsThePreviousFileWhenTheReplacementCannotBeRead(@TempDir Path tempDir) throws Exception {
        Path library = tempDir.resolve("pdf");
        Files.createDirectories(library);
        Files.writeString(library.resolve("笔记.pdf"), "old");
        VectorStore vectorStore = mock(VectorStore.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DocumentIngestService service = libraryService(tempDir, vectorStore, jdbcTemplate);

        IngestResult result = service.store("笔记.pdf", "%PDF-not-a-real-pdf".getBytes(StandardCharsets.US_ASCII));

        assertEquals(FileIngest.FAILED, result.files().get(0).status());
        assertEquals("old", Files.readString(library.resolve("笔记.pdf")));
        verify(vectorStore, never()).add(any());
        verify(jdbcTemplate, never()).update(contains("source_file"), eq("笔记.pdf"));
    }

    @Test
    void storesAReadablePdf(@TempDir Path tempDir) throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Library file.");
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            pdf = out.toByteArray();
        }
        VectorStore vectorStore = mock(VectorStore.class);
        DocumentIngestService service = libraryService(tempDir, vectorStore, mock(JdbcTemplate.class));

        IngestResult result = service.store("上传.pdf", pdf);

        assertEquals(FileIngest.ADDED, result.files().get(0).status());
        assertTrue(result.added() > 0);
        assertTrue(Files.exists(tempDir.resolve("pdf").resolve("上传.pdf")));
        verify(vectorStore).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void listsImportedFiles() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(contains("GROUP BY"), any(RowMapper.class)))
                .thenReturn(List.of(new LibraryFile("课程/笔记.pdf", 3, 8)));
        DocumentIngestService service = service(List.of(), mock(VectorStore.class), jdbcTemplate);

        List<LibraryFile> files = service.list();

        assertEquals(1, files.size());
        assertEquals("课程/笔记.pdf", files.get(0).file());
        assertEquals(3, files.get(0).pageCount());
        assertEquals(8, files.get(0).chunkCount());
    }

    @Test
    void deletesOneFileFromTheLibraryAndTheVectorRows(@TempDir Path tempDir) throws Exception {
        Path root = tempDir;
        Path pdf = root.resolve("pdf").resolve("笔记.pdf");
        Path classes = root.resolve("target").resolve("classes").resolve("pdf").resolve("笔记.pdf");
        Files.createDirectories(pdf.getParent());
        Files.createDirectories(classes.getParent());
        Files.writeString(pdf, "keep");
        Files.writeString(classes, "copy");
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(contains("source_file"), eq("笔记.pdf"))).thenReturn(4);
        PdfDocumentSource source = new PdfDocumentSource(new RootedPaths(root));
        DocumentIngestService service = new DocumentIngestService(List.of(source), wideSplitter(),
                mock(VectorStore.class), jdbcTemplate, new PgVectorStoreProperties(), contents -> Set.of(), source);

        LibraryRemoval removal = service.delete("笔记.pdf");

        assertEquals("笔记.pdf", removal.file());
        assertEquals(4, removal.deletedCount());
        assertTrue(removal.fileRemoved());
        assertFalse(Files.exists(pdf));
        assertFalse(Files.exists(classes));
        assertTrue(Files.exists(Path.of("src/main/resources/pdf/1.pdf")));
    }

    private static FileIngest file(IngestResult result, String name) {
        for (FileIngest file : result.files()) {
            if (name.equals(file.file())) {
                return file;
            }
        }
        throw new AssertionError("没有这份资料: " + name);
    }

    private static DocumentIngestService libraryService(Path root, VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        PdfDocumentSource source = new PdfDocumentSource(new RootedPaths(root));
        return new DocumentIngestService(List.of(source), wideSplitter(), vectorStore, jdbcTemplate,
                new PgVectorStoreProperties(), contents -> Set.of(), source);
    }

    private static DocumentIngestService service(List<DocumentSource> sources, VectorStore vectorStore,
            JdbcTemplate jdbcTemplate) {
        return new DocumentIngestService(sources, splitter(), vectorStore, jdbcTemplate, new PgVectorStoreProperties(),
                contents -> Set.of(), null);
    }

    private static TokenTextSplitter splitter() {
        return splitter(8);
    }

    private static TokenTextSplitter wideSplitter() {
        return splitter(500);
    }

    private static TokenTextSplitter splitter(int chunkSize) {
        RagProperties properties = new RagProperties();
        properties.getChunk().setChunkSize(chunkSize);
        properties.getChunk().setMinChunkSizeChars(1);
        properties.getChunk().setMinChunkLengthToEmbed(1);
        return new RagConfig().tokenTextSplitter(properties);
    }

    private static final class RootedPaths extends ProjectPaths {

        private final Path root;

        private RootedPaths(Path root) {
            super("pdf", "res");
            this.root = root;
        }

        @Override
        public Path root() {
            return root;
        }
    }
}
