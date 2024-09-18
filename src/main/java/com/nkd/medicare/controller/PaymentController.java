package com.nkd.medicare.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.nkd.medicare.service.impl.PaymentServiceimpl;
import java.net.URI;


@RestController
@RequestMapping("/api/user/appointment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentServiceimpl paymentService;
    @GetMapping("/vn-pay")
    public ResponseEntity<?> pay(HttpServletRequest request,@RequestParam(value = "appointmentID") String appointmentID) {
        String paymenturl="";
        try {
            paymenturl = paymentService.createVnPayPayment(request, Integer.parseInt(appointmentID));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(paymenturl))
                .build();
    }
    @GetMapping("/vn-pay-callback")
    public ResponseEntity<?> payCallbackHandler(HttpServletRequest request) {
        String status = request.getParameter("vnp_ResponseCode");
        if (status.equals("00")) {
            paymentService.updateVnPaymentComplete(request);
            return ResponseEntity.ok("OK còn tiền");
//            return ResponseEntity.status(HttpStatus.FOUND)
//                    .location(URI.create())
//                    .build();
        } else {
            paymentService.updateVnPaymentFailed(request);
            return ResponseEntity.ok("OK hết tiền");
//            return ResponseEntity.status(HttpStatus.FOUND)
//                    .location(URI.create())
//                    .build();
        }
    }
}
