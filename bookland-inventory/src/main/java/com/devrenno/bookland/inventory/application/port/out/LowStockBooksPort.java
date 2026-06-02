package com.devrenno.bookland.inventory.application.port.out;

import com.devrenno.bookland.inventory.application.dto.LowStockBookInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LowStockBooksPort {
    Page<LowStockBookInfo> getLowStockBooks(int threshold, Pageable pageable);
}
