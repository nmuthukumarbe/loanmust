package com.loanmust.loan.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Stores uploaded loan documents (KYC, income proof, property papers, MODD receipts).
 *
 * Every key is namespaced under its tenant: {@code tenant-{id}/{folder}/{uuid}-{name}}, the
 * same key format the reference platform uses for Cloudflare R2. This implementation writes
 * to local disk under {@code app.storage.local-dir}; an S3/R2 backend can replace the three
 * methods below without touching any caller. Mount that directory as a volume in Docker.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path root;

    public StorageService(@Value("${app.storage.local-dir:storage-data}") String localDir) {
        this.root = Paths.get(localDir).toAbsolutePath().normalize();
        log.info("Document storage at {}", root);
    }

    public String upload(Long tenantId, String folder, String originalFilename, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("No file content to store.");
        }
        String safeName = originalFilename == null || originalFilename.isBlank()
                ? "file" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String tenant = tenantId == null ? "tenant-shared" : "tenant-" + tenantId;
        String key = tenant + "/" + folder + "/" + UUID.randomUUID() + "-" + safeName;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store " + safeName, e);
        }
        return key;
    }

    public byte[] download(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Document not found: " + key, e);
        }
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete {}: {}", key, e.getMessage());
        }
    }

    /** True when the key belongs to the given tenant - guards every download. */
    public boolean belongsTo(String key, Long tenantId) {
        return key != null && tenantId != null && key.startsWith("tenant-" + tenantId + "/");
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return p;
    }
}
