package com.devrenno.bookland.catalog.infrastructure.storage;

import com.devrenno.bookland.catalog.application.port.out.ImageStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Stores cover images on the local filesystem and returns a public path served by
 * {@link MediaResourceConfig}. Simple default suitable for dev / single-node deployments;
 * swap for an S3/GCS adapter in production without touching the inner layers.
 */
@Component
public class LocalImageStorageAdapter implements ImageStoragePort {

    /** Public URL prefix under which stored covers are exposed (see MediaResourceConfig). */
    public static final String PUBLIC_PATH = "/media/covers";

    private final Path storageDir;

    public LocalImageStorageAdapter(@Value("${bookland.storage.covers-location}") String location) {
        this.storageDir = Path.of(location).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not initialize cover storage at " + storageDir, e);
        }
    }

    @Override
    public String store(byte[] content, String originalFilename, String contentType) {
        String filename = UUID.randomUUID() + extensionFor(contentType, originalFilename);
        Path target = storageDir.resolve(filename);
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store cover image", e);
        }
        return PUBLIC_PATH + "/" + filename;
    }

    private static String extensionFor(String contentType, String originalFilename) {
        return switch (contentType == null ? "" : contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> {
                int dot = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
                yield dot >= 0 ? originalFilename.substring(dot) : "";
            }
        };
    }
}
