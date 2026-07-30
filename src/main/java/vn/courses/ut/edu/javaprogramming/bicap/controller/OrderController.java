package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CreateDepositRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DepositResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> createDeposit(@Valid @RequestBody CreateDepositRequest request) {
        return ResponseEntity.ok(orderService.createDeposit(request));
    }
}