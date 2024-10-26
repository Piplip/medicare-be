package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.enums.StaffStaffType;
import com.nkd.medicare.service.UserService;
import com.nkd.medicare.tables.records.FeedbackRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final DSLContext context;

    @Override
    public String findDoctor(String name, String department, String primaryLanguage, String specialization, String gender, String pageSize, String pageNumber) {
        Condition condition = DSL.trueCondition();

        if(department != null && !department.isEmpty() && !department.equals("default")){
            condition = condition.and(DEPARTMENT.NAME.eq(department));
        }
        if(primaryLanguage != null && !primaryLanguage.isEmpty() && !primaryLanguage.equals("default")){
            condition = condition.and(PERSON.PRIMARY_LANGUAGE.eq(primaryLanguage));
        }
        if(specialization != null && !specialization.isEmpty() && !specialization.equals("default")){
            condition = condition.and(SPECIALIZATION.NAME.eq(specialization));
        }
        if(gender != null && !gender.isEmpty()){
            condition = condition.and(PERSON.GENDER.eq(gender));
        }

        return context.select(STAFF.STAFF_ID, PERSON.PERSON_ID, STAFF.STAFF_IMAGE, STAFF.DEPARTMENT_ID, PERSON.FIRST_NAME,
                        PERSON.LAST_NAME, PERSON.PHONE_NUMBER, PERSON.GENDER, PERSON.PRIMARY_LANGUAGE, DEPARTMENT.NAME, DEPARTMENT.LOCATION, SPECIALIZATION.NAME,
                        SPECIALIZATION.DESCRIPTION, STAFF.STAFF_TYPE)
                .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                .leftJoin(STAFF_SPECIALIZATION).on(STAFF.STAFF_ID.eq(STAFF_SPECIALIZATION.STAFF_ID))
                .join(SPECIALIZATION).on(STAFF_SPECIALIZATION.SPECIALIZATION_ID.eq(SPECIALIZATION.SPECIALIZATION_ID)))
                .where (
                    (PERSON.FIRST_NAME.like("%" + name + "%").or(PERSON.LAST_NAME.like("%" + name + "%")))
                    .and(STAFF.STAFF_TYPE.eq(StaffStaffType.DOCTOR))
                    .and(condition))
                .limit(Integer.parseInt(pageSize))
                .offset(((Integer.parseInt(pageNumber) - 1) * Integer.parseInt(pageSize)))
                .fetch()
                .formatJSON();
    }

    @Override
    public String findDoctorWithID(String staffID) {
        return Objects.requireNonNull(context.select(STAFF.STAFF_ID, STAFF.STAFF_IMAGE, STAFF.DEPARTMENT_ID, DEPARTMENT.NAME,
                                PERSON.FIRST_NAME, PERSON.LAST_NAME, PERSON.PHONE_NUMBER, PERSON.PRIMARY_LANGUAGE
                                , DEPARTMENT.LOCATION, SPECIALIZATION.NAME, SPECIALIZATION.DESCRIPTION)
                        .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                                .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                                .leftJoin(STAFF_SPECIALIZATION).on(STAFF.STAFF_ID.eq(STAFF_SPECIALIZATION.STAFF_ID))
                                .join(SPECIALIZATION).on(STAFF_SPECIALIZATION.SPECIALIZATION_ID.eq(SPECIALIZATION.SPECIALIZATION_ID)))
                        .where(STAFF.STAFF_ID.eq(Integer.parseInt(staffID)))
                        .fetchOne())
                .formatJSON();
    }

    @Override
    public String makeAppointment(AppointmentDTO appointmentDTO) {
        System.out.println("date: " + appointmentDTO.getAppointmentDate());
        System.out.println("time: " + appointmentDTO.getAppointmentTime());

        Integer patientID = context.select(PATIENT.PATIENT_ID)
                .from(ACCOUNT.join(PATIENT).on(ACCOUNT.OWNER_ID.eq(PATIENT.PATIENT_ID)))
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(appointmentDTO.getPatientEmail()))
                .fetchOneInto(Integer.class);
        if(patientID == null){
            return "Account not authenticated! Not found any patient associate with this account";
        }

        Integer departmentID = context.select(STAFF.DEPARTMENT_ID)
                .from(STAFF)
                .where(STAFF.STAFF_ID.eq(Integer.parseInt(appointmentDTO.getDoctorID())))
                .fetchOneInto(Integer.class);

        Integer appointmentID = context.insertInto(APPOINTMENT, APPOINTMENT.PATIENT_ID, APPOINTMENT.PHYSICIAN_ID, APPOINTMENT.DATE, APPOINTMENT.TIME, APPOINTMENT.DEPARTMENT_ID,
                        APPOINTMENT.REASON, APPOINTMENT.STATUS, APPOINTMENT.PHYSICIAN_REFERRED)
                .values(patientID, Integer.parseInt(appointmentDTO.getDoctorID()), appointmentDTO.getAppointmentDate().plusDays(1), appointmentDTO.getAppointmentTime(), departmentID,
                        appointmentDTO.getReason(), AppointmentStatus.SCHEDULED, (byte) (appointmentDTO.getIsReferral().equals("yes") ? 1 : 0))
                .returningResult(APPOINTMENT.APPOINTMENT_ID)
                .fetchOneInto(Integer.class);

        if(appointmentDTO.getIsReminder().equals("yes")){
            context.insertInto(APPOINTMENT_REMINDERS, APPOINTMENT_REMINDERS.APPOINTMENT_ID, APPOINTMENT_REMINDERS.TARGET_ADDRESS
                            , APPOINTMENT_REMINDERS.TARGET_TYPE, APPOINTMENT_REMINDERS.LANGUAGE, APPOINTMENT_REMINDERS.CHANNEL, APPOINTMENT_REMINDERS.STATUS)
                    .values(appointmentID, appointmentDTO.getPatientEmail(), "patient", "en", "email", "pending")
                    .execute();
        }

        assert appointmentID != null;
        return appointmentID.toString();
    }

    @Override
    public String getUserProfile(String email) {
        return Objects.requireNonNull(context.select(PERSON.FIRST_NAME, PERSON.LAST_NAME, PERSON.DATE_OF_BIRTH, PERSON.PHONE_NUMBER,
                                ADDRESS.HOUSE_NUMBER, ADDRESS.STREET, ADDRESS.DISTRICT, ADDRESS.CITY, ACCOUNT.ACCOUNT_EMAIL)
                        .from(ACCOUNT.join(PATIENT).on(ACCOUNT.OWNER_ID.eq(PATIENT.PATIENT_ID))
                                .join(PERSON).on(PATIENT.PERSON_ID.eq(PERSON.PERSON_ID))
                                .join(ADDRESS).on(PERSON.ADDRESS_ID.eq(ADDRESS.ADDRESS_ID)))
                        .where(ACCOUNT.ACCOUNT_EMAIL.eq(email))
                        .fetchAny())
                .formatJSON();
    }

    @Override
    public String getAppointmentList(String email, String status, String query, String department, String startDate, String endDate) {
        Condition condition = DSL.trueCondition();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if(department != null && !department.isEmpty() && !department.equals("default")){
            condition = condition.and(DEPARTMENT.NAME.eq(department));
        }
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
                        DEPARTMENT.NAME, PERSON.FIRST_NAME, PERSON.LAST_NAME)
                .from(ACCOUNT.join(APPOINTMENT).on(ACCOUNT.OWNER_ID.eq(APPOINTMENT.PATIENT_ID))
                        .join(STAFF).on(APPOINTMENT.PHYSICIAN_ID.eq(STAFF.STAFF_ID))
                        .join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                        .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                        .join(PAYMENT).on(APPOINTMENT.APPOINTMENT_ID.eq(PAYMENT.APPOINTMENT_ID)))
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email).and(condition))
                .fetch().formatJSON();
    }

    @Override
    public void postFeedback(FeedbackDTO feedbackDTO, String email) {
        Integer accountID = context.select(ACCOUNT.ACCOUNT_ID)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email))
                .fetchOneInto(Integer.class);

        assert accountID != null;

        FeedbackRecord feedbackRecord = new FeedbackRecord();
        feedbackRecord.setAccountId(accountID);
        feedbackRecord.setCategory(feedbackDTO.getCategory());
        feedbackRecord.setContent(feedbackDTO.getContent());
        feedbackRecord.setLevel(feedbackDTO.getLevel());
        feedbackRecord.setTimestamp(LocalDateTime.now());

        context.insertInto(FEEDBACK).set(feedbackRecord).execute();
    }

    @Override
    public String getFeedbacks(String email) {
        return context.select(FEEDBACK.FEEDBACK_ID, FEEDBACK.CATEGORY, FEEDBACK.CONTENT, FEEDBACK.LEVEL, FEEDBACK.TIMESTAMP)
                .from(ACCOUNT.join(FEEDBACK).on(ACCOUNT.ACCOUNT_ID.eq(FEEDBACK.ACCOUNT_ID)))
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email))
                .fetch().formatJSON();
    }

}
