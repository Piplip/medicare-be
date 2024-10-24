package com.nkd.medicare.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.service.StaffService;
import com.nkd.medicare.tables.records.AppointmentRecord;
import com.nkd.medicare.tables.records.PersonRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.tools.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

import static com.nkd.medicare.Tables.*;
import static com.nkd.medicare.Tables.ACCOUNT;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final DSLContext context;

    @Override
    public String fetchStaffData(String staffID) {
        System.out.println("call fetch staff data");
        int appoinmentCancel = 0;
        int male = 0;
        int female = 0;
        int other = 0;
        int age0_2 = 0;
        int age2_18 = 0;
        int age18_49 = 0;
        int age50 = 0;
        int checkage = 0;
        int agefail = 0;
        int monday = 0;
        int tuesday = 0;
        int wednesday = 0;
        int thursday = 0;
        int friday = 0;
        int saturday = 0;
        int sunday = 0;
        int daynamefail = 0;
        int refferedpatient = 0;
        List<AppointmentRecord> listAppointment = context.selectFrom(APPOINTMENT)
                .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.parseInt(staffID)))
                .fetchInto(AppointmentRecord.class);
        for(AppointmentRecord eachAppointment: listAppointment){
            if(eachAppointment.getStatus().equals(AppointmentStatus.CANCELLED)){
                appoinmentCancel++;
            }
            else{
                if(eachAppointment.getPhysicianReferred() == 1) refferedpatient++;
                PersonRecord person = context.select(PERSON)
                        .from(PATIENT.join(PERSON).on(PATIENT.PERSON_ID.eq(PERSON.PERSON_ID)))
                        .where(PATIENT.PATIENT_ID.eq(eachAppointment.getPatientId()))
                        .fetchOneInto(PersonRecord.class);

                if(person.getGender().equals("FEMALE")) female++;
                else if(person.getGender().equals("MALE")) male++;
                else other++;

                checkage = person.getDateOfBirth().getYear()-LocalDate.now().getYear();
                if(checkage <= 2) age0_2++;
                else if(checkage <= 18) age2_18++;
                else if(checkage <= 49) age18_49++;
                else if(checkage >= 50) age50++;
                else agefail++;

                DayOfWeek dayofweek = eachAppointment.getDate().getDayOfWeek();
                String dayName = dayofweek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                if(dayName.equals("Monday")) monday++;
                else if(dayName.equals("Tuesday")) tuesday++;
                else if(dayName.equals("Wednesday")) wednesday++;
                else if(dayName.equals("Thursday")) thursday++;
                else if(dayName.equals("Friday")) friday++;
                else if(dayName.equals("Saturday")) saturday++;
                else if(dayName.equals("Sunday")) sunday++;
                else daynamefail++;
            }
        }
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("totalappointment", listAppointment.size());
        map.put("appointmentCancel", appoinmentCancel);
        map.put("refferedpatient", refferedpatient);
        map.put("male", male);
        map.put("female", female);
        map.put("other", other);
        map.put("age0_2", age0_2);
        map.put("age2_18", age2_18);
        map.put("age18_49", age18_49);
        map.put("age50", age50);
        map.put("ageFail", agefail);
        map.put("monday", monday);
        map.put("tuesday", tuesday);
        map.put("wednesday", wednesday);
        map.put("thursday", thursday);
        map.put("friday", friday);
        map.put("saturday", saturday);
        map.put("sunday", sunday);
        map.put("dayNameFail", daynamefail);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
