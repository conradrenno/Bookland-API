package com.devrenno.bookland.inventory.api.mapper;

import com.devrenno.bookland.inventory.api.dto.response.InventoryEntryApiResponse;
import com.devrenno.bookland.inventory.api.dto.response.LowStockBookApiResponse;
import com.devrenno.bookland.inventory.application.dto.InventoryEntryResponse;
import com.devrenno.bookland.inventory.application.dto.LowStockBookResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryApiMapper {

    InventoryEntryApiResponse toApiResponse(InventoryEntryResponse response);

    LowStockBookApiResponse toApiResponse(LowStockBookResponse response);
}
