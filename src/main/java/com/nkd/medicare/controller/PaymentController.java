package com.nkd.medicare.controller;

import com.nkd.medicare.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/user/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${spring.client.port}")
    private String clientPort;

    @GetMapping("/vn-pay")
    public ResponseEntity<?> pay(HttpServletRequest request, @RequestParam("appointmentID") String appointmentID, @RequestParam("email") String email) {
        String paymentURL;
        try {
            paymentURL = paymentService.createVnPayPayment(request, Integer.parseInt(appointmentID), email);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok(paymentURL);
    }

    @GetMapping("/vn-pay-callback")
    public ResponseEntity<?> payCallbackHandler(HttpServletRequest request) {
        String status = request.getParameter("vnp_ResponseCode");

        if (status.equals("00")) {
            paymentService.updateVNPaymentComplete(request);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:" + clientPort + "/payment/success")).build();
        } else {
            paymentService.updateVnPaymentFailed(request);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:" + clientPort + "/payment/failed")).build();
        }
    }

    @GetMapping("/payment-history")
    public List<String> getPaymentHistory(@RequestParam("email") String email, @RequestParam(value = "page", required = false, defaultValue = "1") String page) {
        return paymentService.getPaymentHistory(email, page);
    }
}