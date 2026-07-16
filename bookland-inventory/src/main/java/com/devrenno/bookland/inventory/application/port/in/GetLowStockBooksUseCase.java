package com.devrenno.bookland.inventory.application.port.in;

import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.dto.LowStockBook;

public interface GetLowStockBooksUseCase {
    PageResult<LowStockBook> execute(int threshold, PageQuery pageQuery);
}
