package com.devrenno.bookland.catalog.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * The catalog's reading of the reviews module's BookRatingChanged event. Deliberately its own class:
 * what the two modules share is the JSON, not a type. Only the fields the catalog uses are mapped,
 * and unknown ones are ignored so the producer can add fields without breaking this consumer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record BookRatingChangedMessage(UUID eventId, UUID bookId, double averageRating) {
}
