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

@RestController
@RequestMapping("/api/user/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${spring.client.port}")
    private String clientPort;

    @GetMapping("/vn-pay")
    public ResponseEntity<?> pay(HttpServletRequest request, @RequestParam(value = "appointmentID") String appointmentID) {
        String paymentURL;
        try {
            paymentURL = paymentService.createVnPayPayment(request, Integer.parseInt(appointmentID));
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
            return ResponseEntity.badRequest().body("FAILED");
        }
    }
}