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

    public String createVnPayPayment(HttpServletRequest request, Integer appointmentID, String email) {
        Long amount = Integer.parseInt(request.getParameter("amount")) * 100L;
        String bankCode = request.getParameter("bankCode");
        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig(appointmentID+"");
        vnpParamsMap.put("vnp_Amount", String.valueOf(amount));

        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }
        vnpParamsMap.put("vnp_IpAddr", VNPayUtils.getIpAddress(request));

        String queryURL = VNPayUtils.getPaymentURL(vnpParamsMap, true);
        String hashData = VNPayUtils.getPaymentURL(vnpParamsMap, false);
        String vnpSecureHash = VNPayUtils.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryURL += "&vnp_SecureHash=" + vnpSecureHash;

        String payURL = vnPayConfig.getVnp_PayUrl() + "?" + queryURL;

        Integer accountID = context.select(ACCOUNT.ACCOUNT_ID)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email))
                .fetchOne(ACCOUNT.ACCOUNT_ID);

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setPaymentDate(LocalDateTime.now());
        paymentRecord.setAccountId(accountID);
        paymentRecord.setPaymentMethod("Card");
        paymentRecord.setAppointmentId(appointmentID);
        paymentRecord.setAmount(((short) (amount / 100000)));
        paymentRecord.setTransactionStatus(PaymentTransactionStatus.PENDING);
        paymentRecord.setPaymentLink(payURL);

        context.insertInto(PAYMENT).set(paymentRecord).execute();

        return payURL;
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

    @Override
    public List<String> getPaymentHistory(String email, String page) {
        Integer accountID = context.select(ACCOUNT.ACCOUNT_ID)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email))
                .fetchOne(ACCOUNT.ACCOUNT_ID);

        var data = context.select(PAYMENT.PAYMENT_ID, PAYMENT.TRANSACTION_STATUS, PAYMENT.PAYMENT_DATE, PAYMENT.AMOUNT, PAYMENT.PAYMENT_LINK, PAYMENT.PAYMENT_METHOD)
                .from(PAYMENT)
                .where(PAYMENT.ACCOUNT_ID.eq(accountID))
                .orderBy(PAYMENT.PAYMENT_DATE.desc())
                .limit(10)
                .offset((Integer.parseInt(page) - 1) * 10)
                .fetch();

        return List.of(String.valueOf(Math.floor((double) data.size() / 10 + 1)), data.formatJSON());
    }
}