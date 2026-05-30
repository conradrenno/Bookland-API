package com.devrenno.bookland.orders.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrdersController {

    @GetMapping
    public ResponseEntity<Void> placeholder() {
        return ResponseEntity.status(501).build();
    }
}
