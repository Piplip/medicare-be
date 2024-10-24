package com.nkd.medicare.service.impl;

import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.nkd.medicare.Tables.*;
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
    private final DSLContext context;

    @Override
    public String getAppointmentList(String email, String status, String query, String startDate, String endDate) {
        Condition condition = DSL.trueCondition();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if(startDate != null && !startDate.equals("none")){
            if(endDate != null && !endDate.equals("none")){
                System.out.println("Start Date: " + startDate);
                System.out.println("End Date: " + endDate);
                condition = condition.and(APPOINTMENT.DATE.between(LocalDate.parse(startDate, formatter), LocalDate.parse(endDate, formatter)));
            }
            else{
                condition = condition.and(APPOINTMENT.DATE.eq(LocalDate.parse(startDate, formatter)));
            }
        }
        else{
            if(endDate != null && !endDate.equals("none")){
                condition = condition.and(APPOINTMENT.DATE.le(LocalDate.parse(endDate, formatter)));
            }
        }
        if(status != null && !status.isEmpty() && !status.equals("default")){
            if(status.equals("SCHEDULED")){
                condition = condition.and(APPOINTMENT.STATUS.eq(AppointmentStatus.SCHEDULED));
            }
            else if(status.equals("CONFIRMED")){
                condition = condition.and(APPOINTMENT.STATUS.eq(AppointmentStatus.CONFIRMED));
            }
            else if(status.equals("CANCELLED")){
                condition = condition.and(APPOINTMENT.STATUS.eq(AppointmentStatus.CANCELLED));
            }
        }
        if(query != null && !query.isEmpty()){
            condition = condition.and(PERSON.LAST_NAME.eq(query));
        }
        return context.select(APPOINTMENT.APPOINTMENT_ID, APPOINTMENT.DATE, APPOINTMENT.TIME, APPOINTMENT.REASON, APPOINTMENT.STATUS, PAYMENT.TRANSACTION_STATUS,
                        PERSON.FIRST_NAME, PERSON.LAST_NAME)
                .from(ACCOUNT.join(APPOINTMENT).on(ACCOUNT.OWNER_ID.eq(APPOINTMENT.PHYSICIAN_ID))
                        .join(PATIENT).on(APPOINTMENT.PATIENT_ID.eq(PATIENT.PATIENT_ID))
                        .join(PERSON).on(PATIENT.PERSON_ID.eq(PERSON.PERSON_ID))
                        .join(PAYMENT).on(APPOINTMENT.APPOINTMENT_ID.eq(PAYMENT.APPOINTMENT_ID)))
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email)
                        .and(condition))
                .fetch().formatJSON();
    }
}
