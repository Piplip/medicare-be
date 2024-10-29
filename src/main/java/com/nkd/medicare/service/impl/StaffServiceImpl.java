package com.nkd.medicare.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nkd.medicare.domain.MedicationDTO;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.service.StaffService;
import com.nkd.medicare.tables.records.AppointmentRecord;
import com.nkd.medicare.tables.records.MedicationRecord;
import com.nkd.medicare.tables.records.PersonRecord;
import com.nkd.medicare.tables.records.PresrcibedMedicationRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final DSLContext context;

    @Override
    public String fetchStaffData(String staffID, String startDate, String endDate, String typeAppointment, String oneDate) {
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate appointmentDate = LocalDate.now();
        List<LocalDate> weekDates = new ArrayList<>();
        long checkday = 0;
        DayOfWeek dayOfWeek;

        if(oneDate != null && !oneDate.equals("none")){
            LocalDate one = LocalDate.parse(startDate, formatter);
            condition = condition.and(APPOINTMENT.DATE.eq(one));
            appointmentDate = one;
        }
        if (startDate != null && !startDate.equals("none")) {
            LocalDate start = LocalDate.parse(startDate, formatter);
            if (endDate != null && !endDate.equals("none")) {
                LocalDate end = LocalDate.parse(endDate, formatter);
                checkday = ChronoUnit.DAYS.between(start,end);
                condition = condition.and(APPOINTMENT.DATE.between(start, end));
                if(checkday < 7){
                    for (int i = 0; i < checkday; i++) {
                        weekDates.add(start.plusDays(i));
                    }
                }
            } else {
                appointmentDate = start;
                condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(start));
            }
        } else if (endDate != null && !endDate.equals("none")) {
            LocalDate end = LocalDate.parse(endDate, formatter);
            condition = condition.and(APPOINTMENT.DATE.lessOrEqual(end));
        }
        if(checkday == 0) {
            dayOfWeek = appointmentDate.getDayOfWeek();
            LocalDate startOfWeek = appointmentDate.minusDays(dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
            for (int i = 0; i < 7; i++) {
                weekDates.add(startOfWeek.plusDays(i));
            }
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
                    if(weekDates.contains(eachAppointment.getDate())){
                        dayOfWeek = eachAppointment.getDate().getDayOfWeek();
                        String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
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
        }
        map.put("totalappointment", listAppointment.size());
        ObjectMapper mapper = new ObjectMapper();
        Gson gson = new Gson();
        String datachart = gson.toJson(map);
        return datachart;
    }

    @Override
    public String suggestMedication(String nameMedication) {
        return context.select(MEDICATION.NAME, MEDICATION.CONTRAINDICATIONS)
                .from(MEDICATION).where(
                        MEDICATION.NAME.likeIgnoreCase(nameMedication)
                       .or(MEDICATION.CONTRAINDICATIONS.likeIgnoreCase(nameMedication)))
                .fetch().formatJSON();

    }

    @Override
    public String createPrescribed(List<MedicationDTO> listMedication, String staffID, String diagonis) {
        PresrcibedMedicationRecord presrcibedMedication = new PresrcibedMedicationRecord();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        try {
            String physicanName = context.select(PERSON.LAST_NAME)
                    .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID)))
                            .where(STAFF.STAFF_ID.eq(Integer.parseInt(staffID)))
                            .fetchOneInto(String.class);
            int presrcibedId = Objects.requireNonNull(context.insertInto(PRESCRIBED)
                    .set(PRESCRIBED.PRESCRIBED_DATE, LocalDate.now())
                    .set(PRESCRIBED.PRESCRIBING_PHYSICIAN_NAME, physicanName)
                    .set(PRESCRIBED.QUANTITY, (byte) listMedication.size())
                    .set(PRESCRIBED.DIAGNOSIS, diagonis)
                    .returning(PRESCRIBED.PRESCRIBED_ID).fetchOne()).getPrescribedId();
            for (MedicationDTO e : listMedication) {
                presrcibedMedication.setMedicationId(Integer.parseInt(e.getMedicationID()));
                presrcibedMedication.setDosage(new BigDecimal(e.getDosage()));
                presrcibedMedication.setAllowRefill(Byte.valueOf(e.getAllow_refill()));
                presrcibedMedication.setFrequency(e.getFrequency());
                presrcibedMedication.setRoute(e.getRoute());
                presrcibedMedication.setStartDate(LocalDate.parse(e.getStartDate(), formatter));
                presrcibedMedication.setEndDate(LocalDate.parse(e.getEndDate(), formatter));
                presrcibedMedication.setPhysicianNote(e.getNote());
                int presrcibedMedicationId = Objects.requireNonNull(context.insertInto(PRESRCIBED_MEDICATION).set(presrcibedMedication)
                        .returning(PRESRCIBED_MEDICATION.PRESRCIBED_MEDICATION_ID).fetchOne()).getPresrcibedMedicationId();
                context.insertInto(PRESCRIBED_MEDICATION_LIST)
                        .set(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_ID, presrcibedId)
                        .set(PRESCRIBED_MEDICATION_LIST.PRESRCIBED_MEDICATION_ID, presrcibedMedicationId).execute();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String addMedication(String nameMedication) {
        return context.select(MEDICATION.MEDICATION_ID, MEDICATION.NAME)
                .from(MEDICATION).where(MEDICATION.NAME.eq(nameMedication))
                .fetch().formatJSON();
    }
}
