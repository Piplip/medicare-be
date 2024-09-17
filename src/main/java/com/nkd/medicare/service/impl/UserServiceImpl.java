package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.enums.StaffStaffType;
import com.nkd.medicare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

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
                        SPECIALIZATION.DESCRIPTION)
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
        Integer patientID = context.select(PATIENT.PATIENT_ID)
                .from(ACCOUNT.join(PATIENT).on(ACCOUNT.OWNER_ID.eq(PATIENT.PATIENT_ID)))
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(appointmentDTO.getPatientEmail()))
                .fetchOneInto(Integer.class);

        if(patientID == null){
            return "Account not authenticated! Not found any patient associate with this account";
        }

        Integer appointmentID = context.insertInto(APPOINTMENT, APPOINTMENT.PATIENT_ID, APPOINTMENT.PHYSICIAN_ID, APPOINTMENT.DATE, APPOINTMENT.TIME,
                        APPOINTMENT.REASON, APPOINTMENT.STATUS, APPOINTMENT.PHYSICIAN_REFERRED)
                .values(patientID, Integer.parseInt(appointmentDTO.getDoctorID()), appointmentDTO.getAppointmentDate(), appointmentDTO.getAppointmentTime()
                        , appointmentDTO.getReason(), AppointmentStatus.SCHEDULED, (byte) (appointmentDTO.getIsReferral().equals("yes") ? 1 : 0))
                .returningResult(APPOINTMENT.APPOINTMENT_ID)
                .fetchOneInto(Integer.class);

        if(appointmentDTO.getIsReminder().equals("yes")){
            context.insertInto(APPOINTMENT_REMINDERS, APPOINTMENT_REMINDERS.APPOINTMENT_ID, APPOINTMENT_REMINDERS.TARGET_ADDRESS
                            , APPOINTMENT_REMINDERS.TARGET_TYPE, APPOINTMENT_REMINDERS.LANGUAGE, APPOINTMENT_REMINDERS.CHANNEL, APPOINTMENT_REMINDERS.STATUS)
                    .values(appointmentID, appointmentDTO.getPatientEmail(), "patient", "en", "email", "pending")
                    .execute();
        }

        return "Appointment has been scheduled successfully";
    }
}
