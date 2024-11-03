package com.nkd.medicare.service.impl;

import com.google.gson.Gson;
import com.nkd.medicare.domain.MedicationDTO;
import com.nkd.medicare.domain.Prescription;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.enums.PrescribedStatus;
import com.nkd.medicare.service.StaffService;
import com.nkd.medicare.tables.records.*;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
    public String fetchStaffData(String staffID) {
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
                .orderBy(APPOINTMENT.DATE, APPOINTMENT.TIME)
                .fetch().formatJSON();
    }

    @Override
    public String getStatistic(String staffID, String startDate, String endDate, String typeAppointment, String date) {
        Map<String, Integer> map = new HashMap<>();
        map.put("cancelAppointments", 0);
        map.put("notShowedUp", 0);
        map.put("referredPatient", 0);
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
        List<Integer> checkPerson = new ArrayList<>();
        String dayName;
        int checkDay = 0;
        DayOfWeek dayOfWeek;

        if (date != null) {
            one = LocalDate.parse(date, formatter);
            appointmentDate = one;
            dayOfWeek = appointmentDate.getDayOfWeek();
            startOfWeek = appointmentDate.minusDays(dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
            condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(startOfWeek));
        } else if (startDate != null) {
            start = LocalDate.parse(startDate, formatter);
            if (endDate != null) {
                end = LocalDate.parse(endDate, formatter);
                checkDay = (int) ChronoUnit.DAYS.between(start, end) + 1;
                condition = condition.and(APPOINTMENT.DATE.between(start, end));
            } else {
                appointmentDate = LocalDate.of(appointmentDate.getYear() + 1, 10, 30);
                checkDay = (int) ChronoUnit.DAYS.between(start, appointmentDate) + 1;
                condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(start));
            }

            if (checkDay >= 15) checkDay = 14;
            for (int i = 0; i < checkDay; i++) {
                weekDates.add(start.plusDays(i));
            }
        } else if (endDate != null) {
            end = LocalDate.parse(endDate, formatter);
            appointmentDate = LocalDate.of(appointmentDate.getYear() - 1, 10, 30);
            checkDay = (int) ChronoUnit.DAYS.between(appointmentDate, end) + 1;
            condition = condition.and(APPOINTMENT.DATE.lessOrEqual(end));
            if (checkDay >= 15) checkDay = 14;
            for (int i = 0; i < checkDay; i++) {
                weekDates.add(end.minusDays(i));
            }
        } else {
            dayOfWeek = appointmentDate.getDayOfWeek();
            startOfWeek = appointmentDate.minusDays(dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
        }
        if (checkDay == 0) {
            for (int i = 0; i < 7; i++) {
                assert startOfWeek != null;
                weekDates.add(startOfWeek.plusDays(i));
            }
        }

        List<AppointmentRecord> appointments = context.selectFrom(APPOINTMENT)
                .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.parseInt(staffID))
                        .and(condition))
                .fetchInto(AppointmentRecord.class);
        int decreaseAppointment = 0;
        for (AppointmentRecord appointment : appointments) {
            if (typeAppointment.equals("w")) {
                if (weekDates.contains(appointment.getDate())) {
                    dayOfWeek = appointment.getDate().getDayOfWeek();
                    if (start != null || end != null) {
                        if (map.containsKey(appointment.getDate().toString()))
                            map.replace(appointment.getDate().toString(), map.get(appointment.getDate().toString()) + 1);
                        else map.put(appointment.getDate().toString(), 1);
                    } else {
                        dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
                        if (map.containsKey(dayName)) map.replace(dayName, map.get(dayName) + 1);
                        else map.put(dayName, 1);
                    }
                }
            }
            if (one != null) {
                if (!appointment.getDate().isEqual(one)) {
                    decreaseAppointment++;
                    continue;
                }
            } else if (start != null) {
                if (end != null) {
                    if (appointment.getDate().isBefore(start) || appointment.getDate().isAfter(end)) {
                        decreaseAppointment++;
                        continue;
                    }
                } else {
                    if (appointment.getDate().isBefore(start)) {
                        decreaseAppointment++;
                        continue;
                    }
                }
            } else if (end != null) {
                if (appointment.getDate().isAfter(end)) {
                    decreaseAppointment++;
                    continue;
                }
            }
            if (appointment.getStatus().equals(AppointmentStatus.CANCELLED)) {
                map.replace("cancelAppointments", map.get("cancelAppointments") + 1);
            } else {
                if (start != null && end != null && start.isAfter(end)) {
                    decreaseAppointment++;
                    continue;
                }
                if (appointment.getStatus().equals(AppointmentStatus.NOT_SHOWED_UP)) {
                    map.replace("notShowedUp", map.get("notShowedUp") + 1);
                }
                if (appointment.getPhysicianReferred() == 1) {
                    map.replace("referredPatient", map.get("referredPatient") + 1);
                }
                PersonRecord person = context.select()
                        .from(PATIENT.join(PERSON).on(PATIENT.PERSON_ID.eq(PERSON.PERSON_ID)))
                        .where(PATIENT.PATIENT_ID.eq(appointment.getPatientId()))
                        .fetchOneInto(PersonRecord.class);
                assert person != null;
                if (!checkPerson.contains(person.getPersonId())) {
                    checkPerson.add(person.getPersonId());
                    if (person.getGender().equals("Female")) map.replace("female", map.get("female") + 1);
                    else if (person.getGender().equals("Male")) map.replace("male", map.get("male") + 1);
                    else map.replace("other", map.get("other") + 1);

                    int checkAge = LocalDate.now().getYear() - person.getDateOfBirth().getYear();
                    if (checkAge <= 2) map.replace("age0_2", map.get("age0_2") + 1);
                    else if (checkAge <= 18) map.replace("age2_18", map.get("age2_18") + 1);
                    else if (checkAge <= 49) map.replace("age18_49", map.get("age18_49") + 1);
                    else map.replace("age50", map.get("age50") + 1);
                }
            }
        }
        if (typeAppointment.equals("m")) {
            appointments = context.selectFrom(APPOINTMENT)
                    .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.parseInt(staffID)))
                    .fetchInto(AppointmentRecord.class);
            for (AppointmentRecord eachAppointment : appointments) {
                String month = eachAppointment.getDate().getMonth().toString();
                if (map.containsKey(month)) map.replace(month, map.get(month) + 1);
                else map.put(month, 0);
            }
        }
        map.put("totalAppointment", appointments.size() - decreaseAppointment);
        Gson gson = new Gson();
        return gson.toJson(map);
    }

    @Override
    public List<String> suggestMedication(String query) {
        return context.select().from(MEDICATION)
                .where(MEDICATION.NAME.likeIgnoreCase(query + "%"))
                .limit(20)
                .fetch().getValues(MEDICATION.NAME);
    }

    @Override
    public Prescription createPrescription(Prescription prescription, String staffID) {
        if (prescription.getAppointmentID() == null || prescription.getDiagnosis() == null || prescription.getDiagnosis().isEmpty())
            return null;
        else {
            for (MedicationDTO e : prescription.getMedicationList()) {
                if (e.getName() == null || e.getDosage() == null || e.getRoute() == null || e.getStartDate() == null || e.getEndDate() == null || e.getFrequency() == null) {
                    return null;
                }
            }
        }
//        String prescribedID = context.select(PRESCRIBED.STATUS)
//                .from(PRESCRIBED.join(APPOINTMENT).on(PRESCRIBED.PRESCRIBED_ID.eq(APPOINTMENT.PRESCRIBED_ID)))
//                .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(prescription.getAppointmentID())))
//                .fetchOneInto(String.class);

        PrescribedMedicationRecord prescribedMedication = new PrescribedMedicationRecord();
        try {
            PersonRecord physicianName = context.select()
                    .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID)))
                    .where(STAFF.STAFF_ID.eq(Integer.parseInt(staffID)))
                    .fetchOneInto(PersonRecord.class);

            PrescribedRecord prescribed = Objects.requireNonNull(context.insertInto(PRESCRIBED)
                    .set(PRESCRIBED.PRESCRIBED_DATE, LocalDateTime.now())
                    .set(PRESCRIBED.PRESCRIBING_PHYSICIAN_NAME, physicianName.getFirstName() +" "+ physicianName.getLastName())
                    .set(PRESCRIBED.QUANTITY, ((byte) prescription.getMedicationList().size()))
                    .set(PRESCRIBED.DIAGNOSIS, prescription.getDiagnosis())
                    .set(PRESCRIBED.STATUS, PrescribedStatus.PENDING)
                    .returning().fetchAny());

            for (MedicationDTO e : prescription.getMedicationList()) {
                prescribedMedication.setMedicationId(Integer.parseInt(
                        Objects.requireNonNull(context.select(MEDICATION.MEDICATION_ID)
                                .from(MEDICATION)
                                .where(MEDICATION.NAME.eq(e.getName()))
                                .fetchOneInto(String.class))));
                prescribedMedication.setDosage(e.getDosage());
                prescribedMedication.setFrequency(e.getFrequency());
                prescribedMedication.setRoute(e.getRoute());
                prescribedMedication.setStartDate(OffsetDateTime.parse(e.getStartDate()).toLocalDate());
                prescribedMedication.setEndDate(OffsetDateTime.parse(e.getEndDate()).toLocalDate());
                prescribedMedication.setPhysicianNote(e.getNote());

                int prescribedMedicationID = Objects.requireNonNull(context.insertInto(PRESCRIBED_MEDICATION).set(prescribedMedication)
                        .returning(PRESCRIBED_MEDICATION.PRESRCIBED_MEDICATION_ID).fetchOne()).getPresrcibedMedicationId();
                context.insertInto(PRESCRIBED_MEDICATION_LIST)
                        .set(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_ID, prescribed.getPrescribedId())
                        .set(PRESCRIBED_MEDICATION_LIST.PRESRCIBED_MEDICATION_ID, prescribedMedicationID).execute();
            }
            context.update(APPOINTMENT)
                    .set(APPOINTMENT.PRESCRIBED_ID, prescribed.getPrescribedId())
                    .set(APPOINTMENT.STATUS, AppointmentStatus.DONE)
                    .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(prescription.getAppointmentID())))
                    .execute();
            AddressRecord address ;
            PersonRecord person = context.select()
                    .from(PERSON.join(PATIENT).on(PERSON.PERSON_ID.eq(PATIENT.PERSON_ID))
                            .join(APPOINTMENT).on(PATIENT.PATIENT_ID.eq(APPOINTMENT.PATIENT_ID)))
                    .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(prescription.getAppointmentID())))
                    .fetchOneInto(PersonRecord.class);
            address = context.select()
                    .from(ADDRESS)
                    .where(ADDRESS.ADDRESS_ID.eq(person.getAddressId()))
                    .fetchOneInto(AddressRecord.class);
            prescription.setPrescribedID(prescribed.getPrescribedId().toString());
            prescription.setDateOfBirth(person.getDateOfBirth());
            prescription.setPhoneNumber(person.getPhoneNumber());
            prescription.setDoctorName(prescribed.getPrescribingPhysicianName());
            prescription.setGender(person.getGender());
            prescription.setPrescribedTime(prescribed.getPrescribedDate());
            prescription.setAge(String.valueOf((LocalDate.now().getYear() - person.getDateOfBirth().getYear())));
            prescription.setFullname(person.getFirstName()+person.getLastName());
            prescription.setAddress(address.getHouseNumber()+" "+address.getStreet()+" , "+address.getDistrict()+" , "+address.getCity()+" , "+address.getProvince());
            return prescription;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    public Prescription showPrescription(String appointmentID) {
        List<MedicationDTO> Medicationlist = new ArrayList<>();
        List<PrescribedMedicationRecord> Medicationtemplist = new ArrayList<>();
        Prescription prescription = new Prescription();
        PrescribedRecord prescribed = null;

        prescribed = context.select()
                .from(APPOINTMENT.join(PRESCRIBED).on(APPOINTMENT.PRESCRIBED_ID.eq(PRESCRIBED.PRESCRIBED_ID)))
                .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(appointmentID)))
                .fetchOneInto(PrescribedRecord.class);
        if (prescribed != null) {
            Medicationtemplist = context.select()
                    .from(PRESCRIBED_MEDICATION.join(PRESCRIBED_MEDICATION_LIST).on(PRESCRIBED_MEDICATION.PRESRCIBED_MEDICATION_ID.eq(PRESCRIBED_MEDICATION_LIST.PRESRCIBED_MEDICATION_ID)))
                    .where(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_ID.eq(prescribed.getPrescribedId()))
                    .fetchInto(PrescribedMedicationRecord.class);
            for (PrescribedMedicationRecord e : Medicationtemplist) {
                MedicationDTO medication = new MedicationDTO();
                medication.setName(context.select(MEDICATION.NAME).from(MEDICATION)
                        .where(MEDICATION.MEDICATION_ID.eq(e.getMedicationId())).fetchOneInto(String.class));
                medication.setRoute(e.getRoute());
                medication.setFrequency(e.getFrequency());
                medication.setDosage(e.getDosage());
                medication.setNote(e.getPhysicianNote());
                medication.setStartDate(e.getStartDate().toString());
                medication.setEndDate(e.getEndDate().toString());
                Medicationlist.add(medication);
            }
        } else {
            return null;
        }
        prescription.setAppointmentID(appointmentID);
        prescription.setDoctorName(prescribed.getPrescribingPhysicianName());
        prescription.setStatus(prescribed.getStatus().toString());
        prescription.setMedicationList(Medicationlist);
        prescription.setDiagnosis(prescribed.getDiagnosis());
        prescription.setPrescribedTime(prescribed.getPrescribedDate());
        return prescription;

    }

    @Override
    public Integer editPrescribed(String prescribedID) {
        if (prescribedID != null) {
            int checkStatus = context.update(PRESCRIBED)
                    .set(PRESCRIBED.STATUS, PrescribedStatus.PENDING)
                    .where(PRESCRIBED.PRESCRIBED_ID.eq(Integer.parseInt(prescribedID)))
                    .execute();
            if(checkStatus == 0) return 0;
        }
        return 1;
    }

    @Override
    public List<Prescription> getAllPrescription() {
        List<Prescription> listPrescription = new ArrayList<>();
        List<PrescribedMedicationRecord> Medicationtemplist;
        List<PrescribedRecord> listPrescribed = context.select()
                .from(PRESCRIBED)
                .where(PRESCRIBED.STATUS.eq(PrescribedStatus.PENDING))
                .fetchInto(PrescribedRecord.class);
        for(PrescribedRecord prescribed: listPrescribed){
            List<MedicationDTO> Medicationlist = new ArrayList<>();
            PersonRecord person = context.select()
                    .from(PERSON.join(PATIENT).on(PERSON.PERSON_ID.eq(PATIENT.PERSON_ID))
                            .join(APPOINTMENT).on(PATIENT.PATIENT_ID.eq(APPOINTMENT.PATIENT_ID))
                            .join(PRESCRIBED).on(APPOINTMENT.PRESCRIBED_ID.eq(prescribed.getPrescribedId())))
                    .where(PRESCRIBED.PRESCRIBED_ID.eq(prescribed.getPrescribedId()))
                    .fetchOneInto(PersonRecord.class);
            String appointmentID = context.select(APPOINTMENT.APPOINTMENT_ID)
                    .from(APPOINTMENT).where(APPOINTMENT.PRESCRIBED_ID.eq(prescribed.getPrescribedId()))
                    .fetchOneInto(String.class);
            assert person != null;
            AddressRecord address = context.select()
                    .from(ADDRESS)
                    .where(ADDRESS.ADDRESS_ID.eq(person.getAddressId()))
                    .fetchOneInto(AddressRecord.class);
            Medicationtemplist = context.select()
                    .from(PRESCRIBED_MEDICATION.join(PRESCRIBED_MEDICATION_LIST).on(PRESCRIBED_MEDICATION.PRESRCIBED_MEDICATION_ID.eq(PRESCRIBED_MEDICATION_LIST.PRESRCIBED_MEDICATION_ID)))
                    .where(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_ID.eq(prescribed.getPrescribedId()))
                    .fetchInto(PrescribedMedicationRecord.class);
            for (PrescribedMedicationRecord e : Medicationtemplist) {
                MedicationDTO medication = new MedicationDTO();
                medication.setName(context.select(MEDICATION.NAME).from(MEDICATION)
                        .where(MEDICATION.MEDICATION_ID.eq(e.getMedicationId())).fetchOneInto(String.class));
                medication.setRoute(e.getRoute());
                medication.setFrequency(e.getFrequency());
                medication.setDosage(e.getDosage());
                medication.setNote(e.getPhysicianNote());
                medication.setStartDate(e.getStartDate().toString());
                medication.setEndDate(e.getEndDate().toString());
                Medicationlist.add(medication);
            }
            Prescription prescription = new Prescription();
            prescription.setStatus(prescribed.getStatus().toString());
            prescription.setMedicationList(Medicationlist);
            prescription.setDiagnosis(prescribed.getDiagnosis());
            prescription.setPrescribedTime(prescribed.getPrescribedDate());
            prescription.setPrescribedID(prescribed.getPrescribedId().toString());
            prescription.setDateOfBirth(person.getDateOfBirth());
            prescription.setPhoneNumber(person.getPhoneNumber());
            prescription.setAppointmentID(appointmentID);
            prescription.setDoctorName(prescribed.getPrescribingPhysicianName());
            prescription.setGender(person.getGender());
            prescription.setAge(String.valueOf((LocalDate.now().getYear() - person.getDateOfBirth().getYear())));
            prescription.setFullname(person.getFirstName()+person.getLastName());
            assert address != null;
            prescription.setAddress(address.getHouseNumber()+" "+address.getStreet()+" , "+address.getDistrict()+" , "+address.getCity()+" , "+address.getProvince());
            listPrescription.add(prescription);
        }
        return listPrescription;
    }

}
