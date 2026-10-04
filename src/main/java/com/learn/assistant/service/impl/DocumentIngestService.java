package com.learn.assistant.service.impl;

import com.learn.assistant.library.FileStoreRouter;
import com.learn.assistant.library.LibraryRemoval;
import com.learn.assistant.rag.etl.FileIngest;
import com.learn.assistant.rag.etl.IngestResult;
import com.learn.assistant.rag.vectorstore.LibraryFile;
import com.learn.assistant.service.DocumentIngestor;
import com.learn.assistant.rag.etl.e.DocumentSource;
import com.learn.assistant.rag.etl.e.ExtractReader;
import com.learn.assistant.rag.etl.e.PdfExtract;
import com.learn.assistant.rag.etl.e.SourceRead;
import com.learn.assistant.rag.etl.l.LoadRouter;
import com.learn.assistant.rag.etl.l.VectorStoreLoad;
import com.learn.assistant.rag.etl.t.TokenChunkTransform;
import com.learn.assistant.rag.etl.t.TransformRouter;
import com.learn.assistant.rag.vectorstore.ChunkFingerprint;
import com.learn.assistant.rag.vectorstore.PgVectorCatalog;
import com.learn.assistant.rag.vectorstore.StoredChunkLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DocumentIngestService implements DocumentIngestor {

  private static final Logger log = LoggerFactory.getLogger(DocumentIngestService.class);

  private final List<DocumentSource> documentSources;

  private final TransformRouter transformRouter;

  private final LoadRouter loadRouter;

  private final PgVectorCatalog vectorCatalog;

  private final StoredChunkLookup storedChunkLookup;

  private final FileStoreRouter files;

  private final ExtractReader extracts;

  private final Object storeLock = new Object();

  public DocumentIngestService(
      List<DocumentSource> documentSources,
      TokenTextSplitter tokenTextSplitter,
      VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      PgVectorStoreProperties vectorStoreProperties,
      StoredChunkLookup storedChunkLookup,
      FileStoreRouter files,
      ExtractReader extracts) {
    this.documentSources = List.copyOf(documentSources);
    this.transformRouter = new TransformRouter(new TokenChunkTransform(tokenTextSplitter));
    this.loadRouter = new LoadRouter(new VectorStoreLoad(vectorStore));
    this.vectorCatalog = new PgVectorCatalog(jdbcTemplate, vectorStoreProperties);
    this.storedChunkLookup = storedChunkLookup;
    this.files = files;
    this.extracts = extracts;
  }

  @Override
  public int clear() {
    synchronized (storeLock) {
      int deleted = vectorCatalog.clear();
      log.info("已清空向量 {} 条", deleted);
      return deleted;
    }
  }

  @Override
  public List<LibraryFile> list() {
    synchronized (storeLock) {
      return vectorCatalog.listFiles();
    }
  }

  @Override
  public LibraryRemoval delete(String file) {
    String relative = files.normalize(file);
    synchronized (storeLock) {
      int deleted = vectorCatalog.deleteBySourceFile(relative);
      boolean removed = deleteFile(relative);
      log.info("已删除资料 {}，向量 {} 条", relative, deleted);
      return new LibraryRemoval(relative, deleted, removed);
    }
  }

  @Override
  public IngestResult store(String path, byte[] content) {
    if (content == null || content.length == 0) {
      throw new IllegalArgumentException("文件是空的");
    }
    files.check(path, content);
    String relative = files.normalize(path);
    synchronized (storeLock) {
      Path temp = createTemp();
      try {
        Files.write(temp, content);
        SourceRead read = extracts.read(relative, new FileSystemResource(temp));
        if (!read.failures().isEmpty()) {
          SourceRead.Failure failure = read.failures().get(0);
          return new IngestResult(
              0, 0, List.of(FileIngest.failed(failure.file(), failure.message())));
        }
        files.place(temp, relative);
        temp = null;
        FileIngest outcome = ingestPages(relative, read.documents());
        log.info("已保存 {}", relative);
        return new IngestResult(outcome.added(), outcome.skipped(), List.of(outcome));
      } catch (IOException exception) {
        throw new IllegalStateException("保存 PDF 失败");
      } finally {
        deleteTemp(temp);
      }
    }
  }

  @Override
  public IngestResult ingest() {
    synchronized (storeLock) {
      int removed = vectorCatalog.collapseDuplicates();
      if (removed > 0) {
        log.info("已删掉库里重复的 {} 条", removed);
      }
      List<Document> documents = new ArrayList<>();
      List<FileIngest> files = new ArrayList<>();
      for (DocumentSource source : documentSources) {
        SourceRead batch = source.load();
        documents.addAll(batch.documents());
        for (SourceRead.Failure failure : batch.failures()) {
          files.add(FileIngest.failed(failure.file(), failure.message()));
        }
      }
      Map<String, List<Document>> grouped = new LinkedHashMap<>();
      List<Document> unnamed = new ArrayList<>();
      for (Document document : documents) {
        String file = sourceFile(document);
        if (file.isBlank()) {
          unnamed.add(document);
        } else {
          grouped.computeIfAbsent(file, key -> new ArrayList<>()).add(document);
        }
      }
      int added = 0;
      int skipped = 0;
      for (Map.Entry<String, List<Document>> entry : grouped.entrySet()) {
        FileIngest outcome = ingestPages(entry.getKey(), entry.getValue());
        added += outcome.added();
        skipped += outcome.skipped();
        files.add(outcome);
      }
      if (!unnamed.isEmpty()) {
        IngestResult stored = storeNewChunks(transformRouter.apply(unnamed));
        added += stored.added();
        skipped += stored.skipped();
      }
      if (documents.isEmpty() && files.isEmpty()) {
        log.info("没有可导入的文档");
      } else {
        log.info("已导入 {} 段，跳过 {} 段，失败 {} 份", added, skipped, failedCount(files));
      }
      return new IngestResult(added, skipped, files);
    }
  }

  private FileIngest ingestPages(String file, List<Document> pages) {
    List<Document> chunks = transformRouter.apply(pages);
    List<String> hashes = distinctHashes(chunks);
    if (sameChunks(file, hashes)) {
      return FileIngest.skipped(file, hashes.size(), "内容没有变化");
    }
    vectorCatalog.deleteBySourceFile(file);
    IngestResult stored = storeNewChunks(chunks);
    if (stored.added() > 0) {
      return FileIngest.added(file, stored.added(), stored.skipped());
    }
    String message = stored.skipped() > 0 ? "内容已在资料库中" : "没有可导入的文本";
    return FileIngest.skipped(file, stored.skipped(), message);
  }

  private IngestResult storeNewChunks(List<Document> chunks) {
    List<String> texts = new ArrayList<>();
    for (Document chunk : chunks) {
      if (chunk.getText() != null && !chunk.getText().isBlank()) {
        texts.add(chunk.getText());
      }
    }
    Set<String> seen = new HashSet<>();
    for (String text : storedChunkLookup.findPresent(texts)) {
      if (text != null) {
        seen.add(ChunkFingerprint.hash(text));
      }
    }
    List<Document> fresh = new ArrayList<>();
    int skipped = 0;
    for (Document chunk : chunks) {
      String text = chunk.getText();
      if (text == null || text.isBlank()) {
        skipped++;
        continue;
      }
      if (!seen.add(ChunkFingerprint.hash(text))) {
        skipped++;
        continue;
      }
      fresh.add(ChunkFingerprint.stamp(chunk));
    }
    if (!fresh.isEmpty()) {
      loadRouter.accept(fresh);
    }
    return new IngestResult(fresh.size(), skipped, List.of());
  }

  private boolean sameChunks(String file, List<String> hashes) {
    return normalizeHashes(vectorCatalog.hashesOf(file)).equals(normalizeHashes(hashes));
  }

  private boolean deleteFile(String relative) {
    try {
      return files.delete(relative);
    } catch (IOException exception) {
      throw new IllegalStateException("删除 PDF 失败");
    }
  }

  private static Path createTemp() {
    try {
      return Files.createTempFile("learn-pdf-", ".pdf");
    } catch (IOException exception) {
      throw new IllegalStateException("保存 PDF 失败");
    }
  }

  private static void deleteTemp(Path temp) {
    if (temp == null) {
      return;
    }
    try {
      Files.deleteIfExists(temp);
    } catch (IOException ignored) {
      // 副本已经写入资料目录，临时文件留给系统清理
    }
  }

  private static List<String> distinctHashes(List<Document> chunks) {
    Set<String> seen = new HashSet<>();
    List<String> hashes = new ArrayList<>();
    for (Document chunk : chunks) {
      String text = chunk.getText();
      if (text == null || text.isBlank()) {
        continue;
      }
      String hash = ChunkFingerprint.hash(text);
      if (seen.add(hash)) {
        hashes.add(hash);
      }
    }
    return hashes;
  }

  private static List<String> normalizeHashes(List<String> hashes) {
    List<String> copy = new ArrayList<>();
    if (hashes == null) {
      return copy;
    }
    for (String hash : hashes) {
      if (hash != null && !hash.isBlank()) {
        copy.add(hash);
      }
    }
    Collections.sort(copy);
    return copy;
  }

  private static String sourceFile(Document document) {
    Object value = document.getMetadata().get(PdfExtract.SOURCE_FILE);
    return value instanceof String file ? file : "";
  }

  private static int failedCount(List<FileIngest> files) {
    int count = 0;
    for (FileIngest file : files) {
      if (FileIngest.FAILED.equals(file.status())) {
        count++;
      }
    }
    return count;
  }
}
