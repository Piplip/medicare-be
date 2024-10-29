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
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final DSLContext context;

    @Override
    public String fetchStaffData(String staffID, String startDate, String endDate, String typeAppointment, String date) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("appointmentCancel", 0);
        map.put("appointmentNotShowUp", 0);
        map.put("refferedpatient", 0);
        map.put("male", 0);
        map.put("female", 0);
        map.put("other", 0);
        map.put("age0_2", 0);
        map.put("age2_18", 0);
        map.put("age18_49", 0);
        map.put("age50", 0);

        Condition condition = DSL.trueCondition();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate appointmentDate = LocalDate.now();
        LocalDate one = null, start = null, end = null, startOfWeek = null;
        List<LocalDate> weekDates = new ArrayList<>();
        List<Integer> checkWeek = new ArrayList<>();
        List<Integer> checkPerson = new ArrayList<>();
        String dayName;
        int checkday = 0;
        DayOfWeek dayOfWeek;

        if (date != null && !date.equals("none")) {
            one = LocalDate.parse(date, formatter);
            appointmentDate = one;
            dayOfWeek = appointmentDate.getDayOfWeek();
            startOfWeek = appointmentDate.minusDays(dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
            condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(startOfWeek));
        } else if (startDate != null && !startDate.equals("none")) {
            start = LocalDate.parse(startDate, formatter);
            if (endDate != null && !endDate.equals("none")) {
                end = LocalDate.parse(endDate, formatter);
                checkday = (int) ChronoUnit.DAYS.between(start, end);
                condition = condition.and(APPOINTMENT.DATE.between(start, end));
                if(checkday >= 15) checkday = 14;
                for (int i = 0; i < checkday; i++) {
                    weekDates.add(start.plusDays(i));
                    if(start.getDayOfWeek().equals(DayOfWeek.SUNDAY)) checkWeek.add(i+1);
                }
                if(checkWeek.size() < 3) checkWeek.add(checkday+1);
            } else {
                appointmentDate = start;
                dayOfWeek = appointmentDate.getDayOfWeek();
                startOfWeek = appointmentDate.minusDays(dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
                condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(startOfWeek));
            }
        } else if (endDate != null && !endDate.equals("none")) {
            end = LocalDate.parse(endDate, formatter);
            appointmentDate = end;
            dayOfWeek = appointmentDate.getDayOfWeek();
            startOfWeek = appointmentDate.minusDays(dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
            condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(startOfWeek));
        } else {
            dayOfWeek = appointmentDate.getDayOfWeek();
            startOfWeek = appointmentDate.minusDays(dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
        }
        if (checkday == 0) {
            for (int i = 0; i < 7; i++) {
                weekDates.add(startOfWeek.plusDays(i));
            }
        }
        List<AppointmentRecord> listAppointment = context.selectFrom(APPOINTMENT)
                .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.parseInt(staffID))
                        .and(condition))
                .fetchInto(AppointmentRecord.class);
        int decreaseAppointment = 0;
        for (AppointmentRecord eachAppointment : listAppointment) {
            if (typeAppointment.equals("w")) {
                if (weekDates.contains(eachAppointment.getDate())) {
                    dayOfWeek = eachAppointment.getDate().getDayOfWeek();
                    if(start != null && end != null){
                        for(int i = 0; i < checkWeek.size(); i++){
                            if(weekDates.indexOf(eachAppointment.getDate()) < checkWeek.get(i)){
                                if (i == 0) dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
                                else dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase() + i;
                                if (map.containsKey(dayName)) map.replace(dayName, map.get(dayName) + 1);
                                else map.put(dayName, 0);
                            }
                        }
                    }
                    else{
                        dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
                        if (map.containsKey(dayName)) map.replace(dayName, map.get(dayName) + 1);
                        else map.put(dayName, 0);
                    }
                }
            }
            if (one != null) {
                if (!eachAppointment.getDate().isEqual(one)) {
                    decreaseAppointment++;
                    continue;
                }
            } else if (start != null) {
                if (end != null) {
                    if (eachAppointment.getDate().isBefore(start) || eachAppointment.getDate().isAfter(end)) {
                        decreaseAppointment++;
                        continue;
                    }
                } else {
                    if (eachAppointment.getDate().isBefore(start)) {
                        decreaseAppointment++;
                        continue;
                    }
                }
            } else if (end != null) {
                if (eachAppointment.getDate().isAfter(end)) {
                    decreaseAppointment++;
                    continue;
                }
            }
            if (eachAppointment.getStatus().equals(AppointmentStatus.CANCELLED)) {
                map.replace("appointmentCancel", map.get("appointmentCancel") + 1);
            }
            else {
                if (eachAppointment.getStatus().equals(AppointmentStatus.NOT_SHOW_UP)) {
                    map.replace("appointmentNotShowUp", map.get("appointmentNotShowUp") + 1);
                }
                if (eachAppointment.getPhysicianReferred() == 1) {
                    map.replace("refferedpatient", map.get("refferedpatient") + 1);
                }
                PersonRecord person = context.select(PERSON)
                        .from(PATIENT.join(PERSON).on(PATIENT.PERSON_ID.eq(PERSON.PERSON_ID)))
                        .where(PATIENT.PATIENT_ID.eq(eachAppointment.getPatientId()))
                        .fetchOneInto(PersonRecord.class);
                if (!checkPerson.contains(person.getPersonId())) {
                    checkPerson.add(person.getPersonId());
                    if (person.getGender().equals("Female")) map.replace("female", map.get("female") + 1);
                    else if (person.getGender().equals("Male")) map.replace("male", map.get("male") + 1);
                    else map.replace("other", map.get("other") + 1);

                    int checkage = person.getDateOfBirth().getYear() - LocalDate.now().getYear();
                    if (checkage <= 2) map.replace("age0_2", map.get("age0_2") + 1);
                    else if (checkage <= 18) map.replace("age2_18", map.get("age2_18") + 1);
                    else if (checkage <= 49) map.replace("age18_49", map.get("age18_49") + 1);
                    else if (checkage >= 50) map.replace("age50", map.get("age50") + 1);
                }
            }
        }
        if (typeAppointment.equals("m")) {
            listAppointment = context.selectFrom(APPOINTMENT)
                    .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.parseInt(staffID)))
                    .fetchInto(AppointmentRecord.class);
            for(AppointmentRecord eachAppointment : listAppointment) {
                String month = eachAppointment.getDate().getMonth().toString();
                if (map.containsKey(month)) map.replace(month, map.get(month) + 1);
                else map.put(month, 0);
            }
        }
        map.put("totalappointment", listAppointment.size() - decreaseAppointment);
        ObjectMapper mapper = new ObjectMapper();
        Gson gson = new Gson();
        String datachart = gson.toJson(map);
        return datachart;
    }
    @Override
    public List<List<String>> suggestMedication(String nameMedication) {
        return context.select(MEDICATION.NAME, MEDICATION.CONTRAINDICATIONS)
                .from(MEDICATION).where(
                        MEDICATION.NAME.likeIgnoreCase("%" + nameMedication + "%")
                                .or(MEDICATION.CONTRAINDICATIONS.likeIgnoreCase("%" + nameMedication + "%")))
                .fetch()
                .stream()
                .map(record -> List.of(
                        record.get(MEDICATION.NAME),
                        record.get(MEDICATION.CONTRAINDICATIONS)
                ))
                .distinct()
                .collect(Collectors.toList());

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
                presrcibedMedication.setMedicationId(Integer.parseInt(
                        Objects.requireNonNull(context.select(MEDICATION.MEDICATION_ID)
                                .from(MEDICATION)
                                .where(MEDICATION.NAME.eq(e.getMedicationname()))
                                .fetchOneInto(String.class))));
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<List<String>> addMedication(String nameMedication) {
        return context.select(MEDICATION.NAME, MEDICATION.MEDICATION_ID)
                .from(MEDICATION)
                .where(MEDICATION.NAME.eq(nameMedication))
                .orderBy(MEDICATION.MEDICATION_ID.asc())
                .limit(1)
                .fetch()
                .stream()
                .map(record -> List.of(
                        record.get(MEDICATION.NAME),
                        String.valueOf(record.get(MEDICATION.MEDICATION_ID))  // Convert ID to String
                ))
                .collect(Collectors.toList());
    }
}
