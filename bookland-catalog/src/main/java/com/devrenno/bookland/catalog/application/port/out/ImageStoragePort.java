package com.devrenno.bookland.catalog.application.port.out;

/**
 * Outbound port for persisting binary images (e.g. book covers) in an external store.
 * The infrastructure adapter writes the bytes and returns a publicly reachable URL/path,
 * keeping the binary out of the domain and the database. Framework-free.
 */
public interface ImageStoragePort {

    /**
     * Stores the given image bytes and returns a publicly reachable URL/path for it.
     *
     * @param content          the raw image bytes
     * @param originalFilename the client-provided filename (used only to derive an extension); may be null
     * @param contentType      the MIME type of the image (e.g. {@code image/png}); may be null
     * @return the public URL/path under which the stored image can be fetched
     */
    String store(byte[] content, String originalFilename, String contentType);
}
