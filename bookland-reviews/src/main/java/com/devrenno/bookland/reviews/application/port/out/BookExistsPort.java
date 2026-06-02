package com.devrenno.bookland.reviews.application.port.out;

import java.util.UUID;

public interface BookExistsPort {
    boolean exists(UUID bookId);
}
