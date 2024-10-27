package com.nkd.medicare.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.service.StaffService;
import com.nkd.medicare.tables.records.AppointmentRecord;
import com.nkd.medicare.tables.records.PersonRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final DSLContext context;

    @Override
    public String fetchStaffData(String staffID, String startDate, String endDate, String typeAppointment) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("appointmentCancel", 0);
        map.put("refferedpatient", 0);
        map.put("male", 0);
        map.put("female", 0);
        map.put("other", 0);
        map.put("age0_2", 0);
        map.put("age2_18", 0);
        map.put("age18_49", 0);
        map.put("age50", 0);
        map.put("ageFail", 0);
        map.put("monday", 0);
        map.put("tuesday", 0);
        map.put("wednesday", 0);
        map.put("thursday", 0);
        map.put("friday", 0);
        map.put("saturday", 0);
        map.put("sunday", 0);
        map.put("dayNameFail", 0);

        Condition condition = DSL.trueCondition();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (startDate != null && !startDate.equals("none")) {
            LocalDate start = LocalDate.parse(startDate, formatter);
            if (endDate != null && !endDate.equals("none")) {
                LocalDate end = LocalDate.parse(endDate, formatter);
                System.out.println("Start Date: " + startDate);
                System.out.println("End Date: " + endDate);
                condition = condition.and(APPOINTMENT.DATE.between(start, end));
            } else {
                System.out.println("Start Date: " + startDate);
                condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(start));
            }
        } else if (endDate != null && !endDate.equals("none")) {
            LocalDate end = LocalDate.parse(endDate, formatter);
            System.out.println("End Date: " + endDate);
            condition = condition.and(APPOINTMENT.DATE.lessOrEqual(end));
        }

        List<AppointmentRecord> listAppointment = context.selectFrom(APPOINTMENT)
                .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.parseInt(staffID))
                        .and(condition))
                .fetchInto(AppointmentRecord.class);

        for(AppointmentRecord eachAppointment: listAppointment){
            if(eachAppointment.getStatus().equals(AppointmentStatus.CANCELLED)){
                map.replace("appointmentCancel",map.get("appointmentCancel")+1);
            }
            else{
                if(eachAppointment.getPhysicianReferred() == 1) map.replace("refferedpatient",map.get("refferedpatient")+1);;
                PersonRecord person = context.select(PERSON)
                        .from(PATIENT.join(PERSON).on(PATIENT.PERSON_ID.eq(PERSON.PERSON_ID)))
                        .where(PATIENT.PATIENT_ID.eq(eachAppointment.getPatientId()))
                        .fetchOneInto(PersonRecord.class);

                if(person.getGender().equals("FEMALE")) map.replace("female",map.get("female")+1);
                else if(person.getGender().equals("MALE")) map.replace("male",map.get("male")+1);
                else map.replace("other",map.get("other")+1);

                int checkage = person.getDateOfBirth().getYear()-LocalDate.now().getYear();
                if(checkage <= 2) map.replace("age0_2",map.get("age0_2")+1);
                else if(checkage <= 18) map.replace("age2_18",map.get("age2_18")+1);
                else if(checkage <= 49) map.replace("age18_49",map.get("age18_49")+1);
                else if(checkage >= 50) map.replace("age50",map.get("age50")+1);
                else map.replace("ageFail",map.get("ageFail")+1);

                if(typeAppointment.equals("m")){
                    String month  ="month"+eachAppointment.getDate().getMonth().toString();
                    if(map.containsKey(month)) map.replace(month,map.get(month)+1);
                    else map.put(month,0);
                }
                else if(typeAppointment.equals("w")){
                    DayOfWeek dayofweek = eachAppointment.getDate().getDayOfWeek();
                    String dayName = dayofweek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                    if(dayName.equals("Monday")) map.replace("monday",map.get("monday")+1);
                    else if(dayName.equals("Tuesday")) map.replace("tuesday",map.get("tuesday")+1);
                    else if(dayName.equals("Wednesday")) map.replace("wednesday",map.get("wednesday")+1);
                    else if(dayName.equals("Thursday")) map.replace("thursday",map.get("thursday")+1);
                    else if(dayName.equals("Friday")) map.replace("friday",map.get("friday")+1);
                    else if(dayName.equals("Saturday")) map.replace("saturday",map.get("saturday")+1);
                    else if(dayName.equals("Sunday")) map.replace("sunday",map.get("sunday")+1);
                    else map.replace("dayNameFail",map.get("dayNameFail")+1);
                }
            }
        }

        map.put("totalappointment", listAppointment.size());
        ObjectMapper mapper = new ObjectMapper();
        Gson gson = new Gson();
        String datachart = gson.toJson(map);
        return datachart;
    }
}
