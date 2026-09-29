package com.learn.assistant.controller;

import com.learn.assistant.domain.vo.DocumentDeleteResponse;
import com.learn.assistant.domain.vo.DocumentIngestResponse;
import com.learn.assistant.domain.vo.FileIngestResponse;
import com.learn.assistant.domain.vo.LibraryFileResponse;
import com.learn.assistant.domain.vo.VectorClearResponse;
import com.learn.assistant.service.DocumentIngestor;
import com.learn.assistant.service.FileIngest;
import com.learn.assistant.service.IngestResult;
import com.learn.assistant.service.LibraryFile;
import com.learn.assistant.service.LibraryRemoval;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestor documentIngestor;

    public DocumentController(DocumentIngestor documentIngestor) {
        this.documentIngestor = documentIngestor;
    }

    @GetMapping
    public List<LibraryFileResponse> list() {
        List<LibraryFileResponse> files = new ArrayList<>();
        for (LibraryFile file : documentIngestor.list()) {
            files.add(new LibraryFileResponse(file.file(), file.pageCount(), file.chunkCount()));
        }
        return files;
    }

    @PostMapping
    public DocumentIngestResponse ingest() {
        return response(documentIngestor.ingest());
    }

    @PostMapping(path = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentIngestResponse upload(@RequestPart("file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件是空的");
        }
        return response(documentIngestor.store(uploadName(path, file.getOriginalFilename()), file.getBytes()));
    }

    @DeleteMapping("/files")
    public DocumentDeleteResponse delete(@RequestParam("file") String file) {
        LibraryRemoval removal = documentIngestor.delete(file);
        return new DocumentDeleteResponse(removal.file(), removal.deletedCount(), removal.fileRemoved());
    }

    @DeleteMapping
    public VectorClearResponse clear() {
        return new VectorClearResponse(documentIngestor.clear());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "请求不正确";
        }
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private static DocumentIngestResponse response(IngestResult result) {
        List<FileIngestResponse> files = new ArrayList<>();
        for (FileIngest file : result.files()) {
            files.add(new FileIngestResponse(file.file(), file.status(), file.added(), file.skipped(), file.message()));
        }
        return new DocumentIngestResponse(result.added(), result.skipped(), files);
    }

    private static String uploadName(String path, String original) {
        String raw = path == null || path.isBlank() ? original : path;
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        raw = raw.trim().replace('\\', '/');
        if (path == null || path.isBlank()) {
            int slash = raw.lastIndexOf('/');
            if (slash >= 0) {
                raw = raw.substring(slash + 1);
            }
        }
        return raw;
    }
}
