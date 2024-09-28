package com.interview.protech.controller;

import com.interview.protech.dto.ErrorResponse;
import com.interview.protech.dto.orders.OrderRequest;
import com.interview.protech.dto.orders.PayOrderRequest;
import com.interview.protech.entity.Order;
import com.interview.protech.service.IOrderService;
import com.interview.protech.service.IPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final IOrderService orderService;

    private final IPaymentService paymentService;

    @Autowired
    public OrderController(IOrderService orderService, IPaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        try {
            Order order = orderService.placeOrder(request.getUserId(), request.getProductCode(), request.getQuantity());
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Insufficient stock")) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("INSUFFICIENT_STOCK", errorMessage));
            } else if (errorMessage.contains("Product not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("PRODUCT_NOT_FOUND", errorMessage));
            } else if (errorMessage.contains("Unable to acquire lock")) {
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ErrorResponse("SERVICE_BUSY", "The service is currently busy. Please try again later."));
            } else {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("ORDER_PLACEMENT_FAILED", "An error occurred while placing the order"));
            }
        }
    }

    @PostMapping("/pay")
    public ResponseEntity<String> payOrder(@RequestBody PayOrderRequest payOrderRequest) {
        try {
            paymentService.payOrder(payOrderRequest.getOrderNumber());
            return ResponseEntity.ok("Payment successful");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Payment failed: " + e.getMessage());
        }
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderNumber) {
        Order order = orderService.getOrderDetails(orderNumber);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }
}
