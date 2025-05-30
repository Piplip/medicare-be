package com.nkd.medicare.service.impl;

import com.google.gson.Gson;
import com.nkd.medicare.domain.MedicationDTO;
import com.nkd.medicare.domain.PharmacistSignal;
import com.nkd.medicare.domain.Prescription;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.enums.PrescribedStatus;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.service.StaffService;
import com.nkd.medicare.tables.records.*;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.time.*;
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
    public List<String> getAppointments(String staffID, String page, String pageSize, String query, String startDate, String endDate) {
        int pageNumber = page == null ? 1 : Integer.parseInt(page);
        int size = pageSize == null ? 10 : Integer.parseInt(pageSize);

        Condition condition = DSL.trueCondition();
        if(query != null){
             condition = condition.and(PERSON.FIRST_NAME.like("%" + query + "%").or(PERSON.LAST_NAME.like("%" + query + "%")));
        }
        if(startDate != null && !startDate.equals("null")){
            condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(LocalDate.parse(startDate)));
        }
        if(endDate != null && !endDate.equals("null")){
            condition = condition.and(APPOINTMENT.DATE.lessOrEqual(LocalDate.parse(endDate)));
        }

        Integer totalPage = context.selectCount().from(APPOINTMENT.join(PATIENT).on(APPOINTMENT.PATIENT_ID.eq(PATIENT.PATIENT_ID))
                        .join(PERSON).on(PERSON.PERSON_ID.eq(PATIENT.PERSON_ID))
                        .join(ADDRESS).on(PERSON.ADDRESS_ID.eq(ADDRESS.ADDRESS_ID)))
                .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.valueOf(staffID))
                        .and(condition))
                .fetchOneInto(Integer.class);

        String appointments = context.select(APPOINTMENT.APPOINTMENT_ID, APPOINTMENT.PATIENT_ID, APPOINTMENT.REASON, APPOINTMENT.DATE, APPOINTMENT.TIME, PERSON.FIRST_NAME
                , PERSON.LAST_NAME, PERSON.DATE_OF_BIRTH, PERSON.GENDER, ADDRESS.HOUSE_NUMBER, ADDRESS.STREET, ADDRESS.DISTRICT, ADDRESS.CITY, ADDRESS.PROVINCE,
                APPOINTMENT.STATUS)
                .from(APPOINTMENT.join(PATIENT).on(APPOINTMENT.PATIENT_ID.eq(PATIENT.PATIENT_ID))
                        .join(PERSON).on(PERSON.PERSON_ID.eq(PATIENT.PERSON_ID))
                        .join(ADDRESS).on(PERSON.ADDRESS_ID.eq(ADDRESS.ADDRESS_ID)))
                .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.valueOf(staffID))
                        .and(condition))
                .orderBy(APPOINTMENT.DATE.desc(), APPOINTMENT.TIME.desc())
                .limit(size).offset((pageNumber - 1) * size)
                .fetch().formatJSON();

        return List.of(String.valueOf(totalPage / size), appointments);
    }

    @Override
    public Prescription getAppointmentDetail(String appointmentID) {
        PrescribedRecord prescribedRecord = context.select().from(PRESCRIBED).join(APPOINTMENT).on(PRESCRIBED.PRESCRIBED_ID.eq(APPOINTMENT.PRESCRIBED_ID))
                .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(appointmentID)))
                .fetchOneInto(PrescribedRecord.class);

        if(prescribedRecord == null){
            throw new ApiException("No such prescription");
        }

        List<MedicationDTO> medicationDTOList = new ArrayList<>();

        PersonRecord person = context.select()
                .from(PERSON.join(PATIENT).on(PERSON.PERSON_ID.eq(PATIENT.PERSON_ID))
                        .join(APPOINTMENT).on(PATIENT.PATIENT_ID.eq(APPOINTMENT.PATIENT_ID))
                        .join(PRESCRIBED).on(APPOINTMENT.PRESCRIBED_ID.eq(PRESCRIBED.PRESCRIBED_ID)))
                .where(PRESCRIBED.PRESCRIBED_ID.eq(prescribedRecord.getPrescribedId()))
                .fetchOneInto(PersonRecord.class);
        assert person != null;
        AddressRecord address = context.select()
                .from(ADDRESS)
                .where(ADDRESS.ADDRESS_ID.eq(person.getAddressId()))
                .fetchOneInto(AddressRecord.class);

        List<PrescribedMedicationRecord> medicationRecords = context.select()
                .from(PRESCRIBED_MEDICATION.join(PRESCRIBED_MEDICATION_LIST).on(PRESCRIBED_MEDICATION.PRESCRIBED_MEDICATION_ID.eq(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_MEDICATION_ID)))
                .where(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_ID.eq(prescribedRecord.getPrescribedId()))
                .fetchInto(PrescribedMedicationRecord.class);

        for (PrescribedMedicationRecord e : medicationRecords) {
            MedicationDTO medication = new MedicationDTO();
            medication.setName(context.select(MEDICATION.NAME).from(MEDICATION)
                    .where(MEDICATION.MEDICATION_ID.eq(e.getMedicationId())).fetchOneInto(String.class));
            medication.setRoute(e.getRoute());
            medication.setFrequency(e.getFrequency());
            medication.setDosage(e.getDosage());
            medication.setNote(e.getPhysicianNote());
            medication.setStartDate(e.getStartDate().toString());
            medication.setEndDate(e.getEndDate().toString());
            medicationDTOList.add(medication);
        }

        Prescription prescription = new Prescription();
        prescription.setAppointmentID(appointmentID);
        prescription.setStatus(prescribedRecord.getStatus().toString());
        prescription.setMedicationList(medicationDTOList);
        prescription.setDiagnosis(prescribedRecord.getDiagnosis());
        prescription.setPrescribedTime(prescribedRecord.getPrescribedDate().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")));
        prescription.setPrescribedID(prescribedRecord.getPrescribedId().toString());
        prescription.setDateOfBirth(person.getDateOfBirth());
        prescription.setPhoneNumber(person.getPhoneNumber());
        prescription.setDoctorName(prescribedRecord.getPrescribingPhysicianName());
        prescription.setGender(person.getGender());
        prescription.setAge(String.valueOf((LocalDate.now().getYear() - person.getDateOfBirth().getYear())));
        prescription.setName(person.getLastName() + " " + person.getFirstName());
        assert address != null;
        prescription.setAddress(address.getHouseNumber() + " " + address.getStreet() + " , " + address.getDistrict() + " , " + address.getCity() + " , " + "Việt Nam");

        return prescription;
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
        map.put("allPatient", 0);
        map.put("age0_2", 0);
        map.put("age2_18", 0);
        map.put("age18_49", 0);
        map.put("age50", 0);
        map.put("done", 0);

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
                appointmentDate = LocalDate.of(appointmentDate.getYear() + 1,10,30);
                checkDay = (int) ChronoUnit.DAYS.between(start, appointmentDate) + 1;
                condition = condition.and(APPOINTMENT.DATE.greaterOrEqual(start));
            }

            if(checkDay >= 15) checkDay = 14;
            for (int i = 0; i < checkDay; i++) {
                weekDates.add(start.plusDays(i));
            }
        } else if (endDate != null) {
            end = LocalDate.parse(endDate, formatter);
            appointmentDate = LocalDate.of(appointmentDate.getYear()-1,10,30);
            checkDay = (int) ChronoUnit.DAYS.between(appointmentDate, end)+1;
            condition = condition.and(APPOINTMENT.DATE.lessOrEqual(end));
            if(checkDay >= 15) checkDay = 14;
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
                if (weekDates.contains(appointment.getDate())){
                    dayOfWeek = appointment.getDate().getDayOfWeek();
                    if (start != null || end != null) {
                        if (map.containsKey(appointment.getDate().toString())) map.replace(appointment.getDate().toString(), map.get(appointment.getDate().toString()) + 1);
                        else map.put(appointment.getDate().toString(), 1);
                        if(appointment.getStatus().equals(AppointmentStatus.DONE)) {
                            if(map.containsKey(appointment.getDate().toString()+"Done")) map.replace(appointment.getDate().toString()+"Done", map.get(appointment.getDate().toString()+"Done") + 1);
                            else map.put(appointment.getDate().toString()+"Done", 1);
                        }
                        else if(appointment.getStatus().equals(AppointmentStatus.NOT_SHOWED_UP)) {
                            if(map.containsKey(appointment.getDate().toString()+"NotShowUp")) map.replace(appointment.getDate().toString()+"NotShowUp", map.get(appointment.getDate().toString()+"NotShowUp") + 1);
                            else map.put(appointment.getDate().toString()+"NotShowUp", 1);
                        }
                    } else {
                        dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
                        if (map.containsKey(dayName)) map.replace(dayName, map.get(dayName) + 1);
                        else map.put(dayName, 1);
                        if(appointment.getStatus().equals(AppointmentStatus.DONE)) {
                            if(map.containsKey(dayName+"Done")) map.replace(dayName+"Done", map.get(dayName+"Done") + 1);
                            else map.put(dayName+"Done", 1);
                        }
                        else if(appointment.getStatus().equals(AppointmentStatus.NOT_SHOWED_UP)){
                            if(map.containsKey(dayName+"NotShowUp")) map.replace(dayName+"NotShowUp", map.get(dayName+"NotShowUp") + 1);
                            else map.put(dayName+"NotShowUp", 1);
                        }
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
            }
            else {
                if(start != null && end != null && start.isAfter(end)) {
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
                else map.put(month, 1);
                if (eachAppointment.getStatus().equals(AppointmentStatus.DONE)){
                    if(map.containsKey(month+"Done")) map.replace(month+"Done",map.get(month+"Done")+1);
                    else map.put(month+"Done",1);
                }
                else if(eachAppointment.getStatus().equals(AppointmentStatus.NOT_SHOWED_UP)){
                    if(map.containsKey(month+"NotShowUp")) map.replace(month+"NotShowUp",map.get(month+"NotShowUp")+1);
                    else map.put(month+"NotShowUp",1);
                }
            }
        }
        map.put("totalAppointment", appointments.size() - decreaseAppointment);
        map.replace("allPatient", map.get("female")+map.get("male")+map.get("other"));
        map.replace("done", map.get("totalAppointment")-map.get("notShowedUp")-map.get("cancelAppointments"));
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
        if(prescription.getAppointmentID() == null || prescription.getDiagnosis() == null || prescription.getDiagnosis().isEmpty()) return null;
        else{
            for (MedicationDTO e : prescription.getMedicationList()){
                if(e.getName() == null || e.getDosage() == null || e.getRoute() == null || e.getStartDate() == null || e.getEndDate() == null || e.getFrequency() == null){
                    return null;
                }
            }
        }

        PrescribedRecord prescribedRecord = context.select()
                .from(PRESCRIBED.join(APPOINTMENT).on(PRESCRIBED.PRESCRIBED_ID.eq(APPOINTMENT.PRESCRIBED_ID)))
                .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(prescription.getAppointmentID())))
                .fetchOneInto(PrescribedRecord.class);

        if(prescribedRecord != null && prescribedRecord.getStatus().equals(PrescribedStatus.PENDING)){
            List<Integer> Medicationtemplist;
            Medicationtemplist = context.select(PRESCRIBED_MEDICATION.PRESCRIBED_MEDICATION_ID)
                    .from(PRESCRIBED_MEDICATION.join(PRESCRIBED_MEDICATION_LIST).on(PRESCRIBED_MEDICATION.PRESCRIBED_MEDICATION_ID.eq(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_MEDICATION_ID)))
                    .where(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_ID.eq(prescribedRecord.getPrescribedId()))
                    .fetchInto(Integer.class);
            context.update(APPOINTMENT).set(APPOINTMENT.PRESCRIBED_ID, (Integer) null)
                            .where(APPOINTMENT.PRESCRIBED_ID.eq(prescribedRecord.getPrescribedId()))
                                    .execute();
            context.deleteFrom(PRESCRIBED_MEDICATION_LIST)
                    .where(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_ID.eq(prescribedRecord.getPrescribedId()))
                    .execute();
            context.deleteFrom(PRESCRIBED_MEDICATION)
                    .where(PRESCRIBED_MEDICATION.PRESCRIBED_MEDICATION_ID.in(Medicationtemplist))
                    .execute();
            context.deleteFrom(PRESCRIBED)
                    .where(PRESCRIBED.PRESCRIBED_ID.eq(prescribedRecord.getPrescribedId()))
                    .execute();

        }

        PrescribedMedicationRecord prescribedMedication = new PrescribedMedicationRecord();
        try {
            PersonRecord physicianName = context.select()
                    .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID)))
                    .where(STAFF.STAFF_ID.eq(Integer.parseInt(staffID)))
                    .fetchOneInto(PersonRecord.class);

            assert physicianName != null;

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
                        .returning(PRESCRIBED_MEDICATION.PRESCRIBED_MEDICATION_ID).fetchOne()).getPrescribedMedicationId();
                context.insertInto(PRESCRIBED_MEDICATION_LIST)
                        .set(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_ID, prescribed.getPrescribedId())
                        .set(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_MEDICATION_ID, prescribedMedicationID).execute();
            }

            context.update(APPOINTMENT)
                    .set(APPOINTMENT.PRESCRIBED_ID, prescribed.getPrescribedId())
                    .set(APPOINTMENT.STATUS, AppointmentStatus.DONE)
                    .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(prescription.getAppointmentID())))
                    .execute();

            PersonRecord person = context.select()
                    .from(PERSON.join(PATIENT).on(PERSON.PERSON_ID.eq(PATIENT.PERSON_ID))
                            .join(APPOINTMENT).on(PATIENT.PATIENT_ID.eq(APPOINTMENT.PATIENT_ID)))
                    .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(prescription.getAppointmentID())))
                    .fetchOneInto(PersonRecord.class);

            assert person != null;
            AddressRecord address = context.select()
                    .from(ADDRESS)
                    .where(ADDRESS.ADDRESS_ID.eq(person.getAddressId()))
                    .fetchOneInto(AddressRecord.class);

            prescription.setPrescribedID(prescribed.getPrescribedId().toString());
            prescription.setDateOfBirth(person.getDateOfBirth());
            prescription.setPhoneNumber(person.getPhoneNumber());
            prescription.setDoctorName(prescribed.getPrescribingPhysicianName());
            prescription.setGender(person.getGender());
            prescription.setPrescribedTime(prescribed.getPrescribedDate().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")));
            prescription.setAge(String.valueOf((LocalDate.now().getYear() - person.getDateOfBirth().getYear())));
            prescription.setName(person.getLastName() + " " + person.getFirstName());
            prescription.setStatus(prescribed.getStatus().name());
            assert address != null;
            prescription.setAddress(address.getHouseNumber()+" "+address.getStreet()+" , "+address.getDistrict()+" , "+address.getCity()+" , "+address.getProvince());

            return prescription;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    public Prescription getPrescription(String appointmentID) {
        List<MedicationDTO> Medicationlist = new ArrayList<>();
        List<PrescribedMedicationRecord> Medicationtemplist;
        Prescription prescription = new Prescription();

        PrescribedRecord prescribed = context.select()
                .from(APPOINTMENT.join(PRESCRIBED).on(APPOINTMENT.PRESCRIBED_ID.eq(PRESCRIBED.PRESCRIBED_ID)))
                .where(APPOINTMENT.APPOINTMENT_ID.eq(Integer.parseInt(appointmentID)))
                .fetchOneInto(PrescribedRecord.class);

        if(prescribed != null) {
            Medicationtemplist = context.select()
                    .from(PRESCRIBED_MEDICATION.join(PRESCRIBED_MEDICATION_LIST)
                            .on(PRESCRIBED_MEDICATION.PRESCRIBED_MEDICATION_ID.eq(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_MEDICATION_ID)))
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
        }
        else{
            return null;
        }
        prescription.setMedicationList(Medicationlist);
        prescription.setDiagnosis(prescribed.getDiagnosis());
        return prescription;
    }

    @Override
    public PharmacistSignal completePrescribed(String prescribedID, String staffID) {
        int c = context.update(PRESCRIBED)
                .set(PRESCRIBED.STATUS, PrescribedStatus.DONE)
                .where(PRESCRIBED.PRESCRIBED_ID.eq(Integer.parseInt(prescribedID)))
                .execute();
        String signalID = c == 0 ? "PH_03" : "PH_01";
        return new PharmacistSignal(signalID, Map.of("prescribedID", prescribedID, "staffID", staffID));
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
            Integer temp = context.select(APPOINTMENT.PRESCRIBED_ID)
                    .from(APPOINTMENT)
                    .where(APPOINTMENT.PRESCRIBED_ID.eq(prescribed.getPrescribedId()))
                    .fetchOneInto(Integer.class);
            if(temp == null) continue;
            PersonRecord person = context.select()
                    .from(PERSON.join(PATIENT).on(PERSON.PERSON_ID.eq(PATIENT.PERSON_ID))
                            .join(APPOINTMENT).on(PATIENT.PATIENT_ID.eq(APPOINTMENT.PATIENT_ID))
                            .join(PRESCRIBED).on(APPOINTMENT.PRESCRIBED_ID.eq(PRESCRIBED.PRESCRIBED_ID)))
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
                    .from(PRESCRIBED_MEDICATION.join(PRESCRIBED_MEDICATION_LIST).on(PRESCRIBED_MEDICATION.PRESCRIBED_MEDICATION_ID.eq(PRESCRIBED_MEDICATION_LIST.PRESCRIBED_MEDICATION_ID)))
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
            prescription.setAppointmentID(appointmentID);
            prescription.setStatus(prescribed.getStatus().toString());
            prescription.setMedicationList(Medicationlist);
            prescription.setDiagnosis(prescribed.getDiagnosis());
            prescription.setPrescribedTime(prescribed.getPrescribedDate().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")));
            prescription.setPrescribedID(prescribed.getPrescribedId().toString());
            prescription.setDateOfBirth(person.getDateOfBirth());
            prescription.setPhoneNumber(person.getPhoneNumber());
            prescription.setDoctorName(prescribed.getPrescribingPhysicianName());
            prescription.setGender(person.getGender());
            prescription.setAge(String.valueOf((LocalDate.now().getYear() - person.getDateOfBirth().getYear())));
            prescription.setName(person.getLastName() + " " + person.getFirstName());
            assert address != null;
            prescription.setAddress(address.getHouseNumber() + " " + address.getStreet() + " , " + address.getDistrict() + " , " + address.getCity() + " , " + "Việt Nam");
            listPrescription.add(prescription);
        }
        return listPrescription;
    }
}
