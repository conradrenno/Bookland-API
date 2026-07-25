package com.devrenno.bookland.catalog.application.dto;

/**
 * Framework-free carrier for an uploaded cover image. The web layer extracts these primitives
 * from the multipart request so no Spring/servlet type crosses into the inner layers.
 */
public record UploadBookCoverCommand(
        byte[] content,
        String originalFilename,
        String contentType
) {}
