package com.devrenno.bookland.inventory.application.port.out;

import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.dto.LowStockBookInfo;

public interface LowStockBooksPort {
    PageResult<LowStockBookInfo> getLowStockBooks(int threshold, PageQuery pageQuery);
}
