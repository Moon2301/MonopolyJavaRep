package com.game.monopoly.controller;

import com.game.monopoly.dto.PaymentUrlResponse;
import com.game.monopoly.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-url")
    public ResponseEntity<PaymentUrlResponse> createPaymentUrl(
            @RequestHeader(name = "X-Account-Id", required = true) Long accountId,
            @RequestParam("amount") Long amount,
            HttpServletRequest request
    ) {
        String paymentUrl = paymentService.createPaymentUrl(amount, accountId, request);
        return ResponseEntity.ok(new PaymentUrlResponse(paymentUrl));
    }

    @GetMapping("/return")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        boolean success = paymentService.processVnPayReturn(request);

        // Redirect the user back to the home page (main-menu)
        String redirectUrl = "/home?topupSuccess=" + success;
        return ResponseEntity.status(302).header("Location", redirectUrl).build();
    }
}