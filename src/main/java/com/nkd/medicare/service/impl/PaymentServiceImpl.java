package com.nkd.medicare.service.impl;

import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.enums.PaymentTransactionStatus;
import com.nkd.medicare.tables.records.PaymentRecord;
import com.nkd.medicare.utils.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import com.nkd.medicare.config.VNPAYConfig;
import com.nkd.medicare.service.PaymentService;
import java.time.LocalDateTime;
import java.util.*;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

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
        vnpParamsMap.put("vnp_IpAddr", VNPayUtils.getIpAddress(request));

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setPaymentDate(LocalDateTime.now());
        paymentRecord.setPaymentMethod("Card");
        paymentRecord.setAppointmentId(appointmentID);
        paymentRecord.setAmount(((short) (amount / 100000)));
        paymentRecord.setTransactionStatus(PaymentTransactionStatus.PENDING);
        context.insertInto(PAYMENT).set(paymentRecord).execute();

        String queryURL = VNPayUtils.getPaymentURL(vnpParamsMap, true);
        String hashData = VNPayUtils.getPaymentURL(vnpParamsMap, false);
        String vnpSecureHash = VNPayUtils.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryURL += "&vnp_SecureHash=" + vnpSecureHash;

        return vnPayConfig.getVnp_PayUrl() + "?" + queryURL;
    }

    public void updateVNPaymentComplete(HttpServletRequest request){
        String appointmentID = request.getParameter("vnp_TxnRef");
        String transactionID = request.getParameter("vnp_TransactionNo");

        context.update(APPOINTMENT)
                .set(APPOINTMENT.STATUS, AppointmentStatus.CONFIRMED)
                .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.valueOf(appointmentID)))
                .execute();

        context.update(PAYMENT)
                .set(PAYMENT.TRANSACTION_ID, transactionID)
                .set(PAYMENT.TRANSACTION_STATUS, PaymentTransactionStatus.COMPLETED)
                .where(PAYMENT.APPOINTMENT_ID.eq(Integer.parseInt(appointmentID)))
                .execute();
    }

    public void updateVnPaymentFailed(HttpServletRequest request){
        String appointmentID = request.getParameter("vnp_TxnRef");

        context.update(APPOINTMENT)
                .set(APPOINTMENT.STATUS, AppointmentStatus.CANCELLED)
                .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.valueOf(appointmentID)))
                .execute();

        context.update(PAYMENT)
                .set(PAYMENT.TRANSACTION_STATUS, PaymentTransactionStatus.FAILED)
                .where(PAYMENT.APPOINTMENT_ID.eq(Integer.parseInt(appointmentID)))
                .execute();
    }
}