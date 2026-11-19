package com.sih.bidcompliance.service;

import com.sih.bidcompliance.exception.InvalidFileException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Stores uploaded bidder documents on the local filesystem for the MVP.
 * The storage location is configurable via APP_UPLOAD_DIR; cloud storage
 * will replace this in a later milestone.
 */
@Service
public class FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf", "image/png", "image/jpeg"
    );

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadPath, e);
        }
    }

    /**
     * Validates and saves the given file, returning the path it was stored at.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException("Only PDF, PNG and JPEG files are allowed");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path targetPath = uploadPath.resolve(storedName);

        try {
            Files.copy(file.getInputStream(), targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + originalName, e);
        }

        return targetPath.toString();
    }

    /**
     * Re-reads a previously stored file from disk as a MultipartFile, so it
     * can be re-sent to the AI service (e.g. for the "Verify / Analyze"
     * action on an already-uploaded document).
     */
    public MultipartFile readAsMultipartFile(String storedFilePath, String originalFileName) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(storedFilePath));
            String contentType = detectContentType(originalFileName);
            return new StoredFileMultipart(bytes, originalFileName, contentType);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read stored file: " + storedFilePath, e);
        }
    }

    private String detectContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    /**
     * Minimal in-memory MultipartFile implementation for re-reading a
     * stored file from disk. Avoids pulling in spring-test's
     * MockMultipartFile as a production dependency.
     */
    private static class StoredFileMultipart implements MultipartFile {
        private final byte[] bytes;
        private final String fileName;
        private final String contentType;

        StoredFileMultipart(byte[] bytes, String fileName, String contentType) {
            this.bytes = bytes;
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
