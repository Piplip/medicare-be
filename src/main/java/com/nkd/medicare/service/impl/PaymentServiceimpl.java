package com.nkd.medicare.service.impl;


import com.nkd.medicare.enums.PaymentTransactionStatus;
import com.nkd.medicare.tables.records.PaymentRecord;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import com.nkd.medicare.utils.VNPayUtil;
import com.nkd.medicare.config.VNPAYConfig;
import com.nkd.medicare.service.PaymentService;
import java.time.LocalDateTime;
import java.util.*;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceimpl implements PaymentService {

    private final DSLContext context;
    private final VNPAYConfig vnPayConfig;
    public String createVnPayPayment(HttpServletRequest request, Integer appointmentID) {

        Long amount = Integer.parseInt(request.getParameter("amount")) * 100L;
        String bankCode = request.getParameter("bankCode");
        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig(appointmentID+"");
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));

        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }

        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
        String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
        String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

        PaymentRecord paymentr = new PaymentRecord();
        paymentr.setPaymentDate(LocalDateTime.now());
        paymentr.setPaymentMethod("Card");
        paymentr.setAppointmentId(appointmentID);
        paymentr.setAmount(amount.shortValue());
        paymentr.setTransactionStatus(PaymentTransactionStatus.PENDING);
        context.insertInto(PAYMENT).set(paymentr);

        return paymentUrl;
    }
    public void updateVnPaymentComplete(HttpServletRequest request){
        String appointmentID = request.getParameter("vnp_TxnRef");
        String TransactionID = request.getHeader("vnp_TransactionNo");
        context.update(PAYMENT)
                .set(PAYMENT.TRANSACTION_ID, TransactionID)
                .set(PAYMENT.TRANSACTION_STATUS, PaymentTransactionStatus.COMPLETED)
                .where(PAYMENT.APPOINTMENT_ID.eq(Integer.parseInt(appointmentID)))
                .execute();
    }
    public void updateVnPaymentFailed(HttpServletRequest request){
        String appointmentID = request.getParameter("vnp_TxnRef");
        context.update(PAYMENT)
                .set(PAYMENT.TRANSACTION_STATUS, PaymentTransactionStatus.FAILED)
                .where(PAYMENT.APPOINTMENT_ID.eq(Integer.parseInt(appointmentID)))
                .execute();
    }
}
