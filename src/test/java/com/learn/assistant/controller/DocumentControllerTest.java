package com.learn.assistant.controller;

import com.learn.assistant.service.DocumentIngestor;
import com.learn.assistant.service.FileIngest;
import com.learn.assistant.service.IngestResult;
import com.learn.assistant.service.LibraryFile;
import com.learn.assistant.service.LibraryRemoval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentIngestor documentIngestor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(documentIngestor))
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void listsImportedFiles() throws Exception {
        when(documentIngestor.list()).thenReturn(List.of(new LibraryFile("课程/笔记.pdf", 3, 8)));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].file").value("课程/笔记.pdf"))
                .andExpect(jsonPath("$[0].pageCount").value(3))
                .andExpect(jsonPath("$[0].chunkCount").value(8));
    }

    @Test
    void ingestNamesEachFile() throws Exception {
        when(documentIngestor.ingest()).thenReturn(new IngestResult(2, 1, List.of(
                FileIngest.added("新.pdf", 2, 0),
                FileIngest.skipped("旧.pdf", 1, "内容没有变化"),
                FileIngest.failed("坏.pdf", "无法读取"))));

        mockMvc.perform(post("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addedCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.files[0].status").value("added"))
                .andExpect(jsonPath("$.files[1].file").value("旧.pdf"))
                .andExpect(jsonPath("$.files[2].status").value("failed"))
                .andExpect(jsonPath("$.files[2].message").value("无法读取"));
    }

    @Test
    void uploadsAPdfAndCanTargetAnExistingPath() throws Exception {
        when(documentIngestor.store(eq("课程/笔记.pdf"), any()))
                .thenReturn(new IngestResult(1, 0, List.of(FileIngest.added("课程/笔记.pdf", 1, 0))));
        MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

        mockMvc.perform(multipart("/api/documents/files").file(file).param("path", "课程/笔记.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].file").value("课程/笔记.pdf"))
                .andExpect(jsonPath("$.files[0].status").value("added"));
    }

    @Test
    void rejectsAnEmptyUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/documents/files").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文件是空的"));
    }

    @Test
    void deletesOneFile() throws Exception {
        when(documentIngestor.delete("课程/笔记.pdf")).thenReturn(new LibraryRemoval("课程/笔记.pdf", 4, true));

        mockMvc.perform(delete("/api/documents/files").param("file", "课程/笔记.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file").value("课程/笔记.pdf"))
                .andExpect(jsonPath("$.deletedCount").value(4))
                .andExpect(jsonPath("$.fileRemoved").value(true));
        verify(documentIngestor).delete("课程/笔记.pdf");
    }
}
