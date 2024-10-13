package com.nkd.medicare.service;

import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {
    String createVnPayPayment(HttpServletRequest request, Integer appointmentID);
    void updateVNPaymentComplete(HttpServletRequest request);
    void updateVnPaymentFailed(HttpServletRequest request);
}
