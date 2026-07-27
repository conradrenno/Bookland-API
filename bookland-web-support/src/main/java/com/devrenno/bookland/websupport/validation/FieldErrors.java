package com.devrenno.bookland.websupport.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates the {@code errors} member of a validation problem+json: field name → messages.
 *
 * <p>One field can break several constraints at once (a password can be both too short and missing
 * a digit), so the values are lists — a flat map would silently drop all but one.
 */
final class FieldErrors {

    /**
     * Key for errors that belong to the payload as a whole rather than to one field, so a client
     * rendering the map field-by-field has somewhere well-known to put them.
     */
    static final String OBJECT_LEVEL = "_";

    private final Map<String, List<String>> errors = new LinkedHashMap<>();

    void add(String field, String message) {
        errors.computeIfAbsent(field == null || field.isBlank() ? OBJECT_LEVEL : field,
                key -> new ArrayList<>()).add(message);
    }

    Map<String, List<String>> asMap() {
        return errors;
    }

    boolean isEmpty() {
        return errors.isEmpty();
    }

    int fieldCount() {
        return errors.size();
    }

    String summary() {
        if (isEmpty()) {
            return "The request payload is invalid";
        }
        return "Validation failed for " + fieldCount() + (fieldCount() == 1 ? " field" : " fields")
                + ": " + String.join(", ", errors.keySet());
    }
}
