package com.nkd.medicare.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface PaymentService {
    String createVnPayPayment(HttpServletRequest request, Integer appointmentID, String email);
    void updateVNPaymentComplete(HttpServletRequest request);
    void updateVnPaymentFailed(HttpServletRequest request);
    List<String> getPaymentHistory(String email, String page);
}
