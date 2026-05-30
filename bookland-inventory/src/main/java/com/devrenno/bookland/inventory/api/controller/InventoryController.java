package com.devrenno.bookland.inventory.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    @GetMapping
    public ResponseEntity<Void> placeholder() {
        return ResponseEntity.status(501).build();
    }
}
