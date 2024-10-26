package com.nkd.medicare.service.impl;

import com.nkd.medicare.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final DSLContext context;

    @Override
    public String fetchStaffData(String staffID) {
        System.out.println("call fetch staff data");
        return context.select(STAFF.STAFF_IMAGE)
                .from(STAFF)
                .where(STAFF.STAFF_ID.eq(Integer.valueOf(staffID)))
                .fetchOneInto(String.class);
    }

    @Override
    public String getAppointments(String staffID) {
        return context.select(APPOINTMENT.APPOINTMENT_ID, APPOINTMENT.PATIENT_ID, APPOINTMENT.REASON, APPOINTMENT.DATE, APPOINTMENT.TIME, PERSON.FIRST_NAME
                , PERSON.LAST_NAME, PERSON.DATE_OF_BIRTH, PERSON.GENDER, ADDRESS.HOUSE_NUMBER, ADDRESS.STREET, ADDRESS.DISTRICT, ADDRESS.CITY, ADDRESS.PROVINCE,
                APPOINTMENT.STATUS)
                .from(APPOINTMENT.join(PATIENT).on(APPOINTMENT.PATIENT_ID.eq(PATIENT.PATIENT_ID))
                        .join(PERSON).on(PERSON.PERSON_ID.eq(PATIENT.PERSON_ID))
                        .join(ADDRESS).on(PERSON.ADDRESS_ID.eq(ADDRESS.ADDRESS_ID)))
                .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.valueOf(staffID)))
                .fetch().formatJSON();
    }
}
