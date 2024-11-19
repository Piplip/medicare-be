package com.nkd.medicare.service.impl;

import com.google.gson.*;
import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.Data;
import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;
import com.nkd.medicare.domain.MedicationDTO;
import com.nkd.medicare.domain.Prescription;
import com.nkd.medicare.enumeration.EventType;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.enums.StaffStaffType;
import com.nkd.medicare.event.UserEvent;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.service.UserService;
import com.nkd.medicare.tables.records.*;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

import static com.nkd.medicare.Tables.*;
import static java.lang.Thread.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final DSLContext context;
    private final ApplicationEventPublisher publisher;
    private static final String API_KEY = System.getenv("API_KEY");
    private final Map<String, Data.Thread> threadList= new HashMap<>();
    private final PasswordEncoder encoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public List<String> findDoctor(String name, String department, String primaryLanguage, String specialization, String gender, String pageSize, String pageNumber) {
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

        Integer totalRecord = context.selectCount()
                .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                        .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                        .leftJoin(STAFF_SPECIALIZATION).on(STAFF.STAFF_ID.eq(STAFF_SPECIALIZATION.STAFF_ID))
                        .join(SPECIALIZATION).on(STAFF_SPECIALIZATION.SPECIALIZATION_ID.eq(SPECIALIZATION.SPECIALIZATION_ID)))
                .where (
                        (PERSON.FIRST_NAME.like("%" + name + "%").or(PERSON.LAST_NAME.like("%" + name + "%")))
                                .and(STAFF.STAFF_TYPE.eq(StaffStaffType.DOCTOR))
                                .and(condition))
                .fetchOneInto(Integer.class);

        String data = context.select(STAFF.STAFF_ID, PERSON.PERSON_ID, STAFF.STAFF_IMAGE, STAFF.DEPARTMENT_ID, PERSON.FIRST_NAME,
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

        assert totalRecord != null;
        return List.of(String.valueOf(totalRecord / Integer.parseInt(pageSize)), data);
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

        Integer departmentID = context.select(STAFF.DEPARTMENT_ID)
                .from(STAFF)
                .where(STAFF.STAFF_ID.eq(Integer.parseInt(appointmentDTO.getDoctorID())))
                .fetchOneInto(Integer.class);

        Integer appointmentID = context.insertInto(APPOINTMENT, APPOINTMENT.PATIENT_ID, APPOINTMENT.PHYSICIAN_ID, APPOINTMENT.DATE, APPOINTMENT.TIME, APPOINTMENT.DEPARTMENT_ID,
                        APPOINTMENT.REASON, APPOINTMENT.STATUS, APPOINTMENT.PHYSICIAN_REFERRED)
                .values(patientID, Integer.parseInt(appointmentDTO.getDoctorID()),
                        LocalDate.parse(appointmentDTO.getAppointmentDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        LocalTime.parse(appointmentDTO.getAppointmentTime(), DateTimeFormatter.ofPattern("HH:mm")), departmentID,
                        appointmentDTO.getReason(), AppointmentStatus.SCHEDULED, (byte) (appointmentDTO.getIsReferral().equals("yes") ? 1 : 0))
                .returningResult(APPOINTMENT.APPOINTMENT_ID)
                .fetchOneInto(Integer.class);

        if(appointmentDTO.getIsReminder().equals("yes")){
            context.insertInto(APPOINTMENT_REMINDERS, APPOINTMENT_REMINDERS.APPOINTMENT_ID, APPOINTMENT_REMINDERS.TARGET_ADDRESS
                            , APPOINTMENT_REMINDERS.TARGET_TYPE, APPOINTMENT_REMINDERS.LANGUAGE, APPOINTMENT.DATE, APPOINTMENT.TIME)
                    .values(appointmentID, appointmentDTO.getPatientEmail(), "patient", "en", LocalDate.parse(appointmentDTO.getAppointmentDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            LocalTime.parse(appointmentDTO.getAppointmentTime(), DateTimeFormatter.ofPattern("HH:mm")))
                    .execute();
        }

        String doctorEmail = context.select(ACCOUNT.ACCOUNT_EMAIL)
                .from(ACCOUNT.join(STAFF).on(ACCOUNT.OWNER_ID.eq(STAFF.STAFF_ID)))
                .where(STAFF.STAFF_ID.eq(Integer.parseInt(appointmentDTO.getDoctorID())))
                .fetchOneInto(String.class);

        assert doctorEmail != null;
        Map<?, ?> data = Map.of("patientEmail", appointmentDTO.getPatientEmail(), "doctorEmail", doctorEmail,
            "patientName", appointmentDTO.getPatientName(),
                "date", LocalDate.parse(appointmentDTO.getAppointmentDate(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                , "time", appointmentDTO.getAppointmentTime(), "reason", appointmentDTO.getReason());
        UserEvent makeAppointmentEvent = UserEvent.builder()
                        .eventType(EventType.MAKE_APPOINTMENT)
                        .data(data)
                        .build();
        publisher.publishEvent(makeAppointmentEvent);

        assert appointmentID != null;
        return appointmentID.toString();
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
        prescription.setDoctorName(prescribedRecord.getPrescribingPhysicianName());

        return prescription;
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
    public String getAppointmentList(String email, String status, String query, String department, String startDate, String endDate, String page) {
        Condition condition = DSL.trueCondition();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if(department != null && !department.isEmpty() && !department.equals("default")){
            condition = condition.and(DEPARTMENT.NAME.eq(department));
        }
        if(startDate != null && !startDate.equals("none")){
            if(endDate != null && !endDate.equals("none")){
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
            condition = switch (status) {
                case "SCHEDULED" -> condition.and(APPOINTMENT.STATUS.eq(AppointmentStatus.SCHEDULED));
                case "CONFIRMED" -> condition.and(APPOINTMENT.STATUS.eq(AppointmentStatus.CONFIRMED));
                case "CANCELLED" -> condition.and(APPOINTMENT.STATUS.eq(AppointmentStatus.CANCELLED));
                default -> condition;
            };
        }
        if(query != null && !query.isEmpty()){
            condition = condition.and(PERSON.LAST_NAME.eq(query));
        }

        int currentPage = page != null ?  Integer.parseInt(page) - 1 : 0;

        Integer totalPage = context.selectCount().from(ACCOUNT.join(APPOINTMENT).on(ACCOUNT.OWNER_ID.eq(APPOINTMENT.PATIENT_ID))
                        .join(STAFF).on(APPOINTMENT.PHYSICIAN_ID.eq(STAFF.STAFF_ID))
                        .join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                        .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                        .join(PAYMENT).on(APPOINTMENT.APPOINTMENT_ID.eq(PAYMENT.APPOINTMENT_ID)))
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email).and(condition))
                .fetchOneInto(Integer.class);

        String data = context.select(APPOINTMENT.APPOINTMENT_ID, APPOINTMENT.DATE, APPOINTMENT.TIME, APPOINTMENT.REASON, APPOINTMENT.STATUS, PAYMENT.TRANSACTION_STATUS,
                        DEPARTMENT.NAME, PERSON.FIRST_NAME, PERSON.LAST_NAME)
                .from(ACCOUNT.join(APPOINTMENT).on(ACCOUNT.OWNER_ID.eq(APPOINTMENT.PATIENT_ID))
                        .join(STAFF).on(APPOINTMENT.PHYSICIAN_ID.eq(STAFF.STAFF_ID))
                        .join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                        .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                        .join(PAYMENT).on(APPOINTMENT.APPOINTMENT_ID.eq(PAYMENT.APPOINTMENT_ID)))
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email).and(condition))
                .orderBy(APPOINTMENT.DATE.desc(), APPOINTMENT.TIME.desc())
                .limit(10)
                .offset(currentPage * 10)
                .fetch().formatJSON();

        List<String> result = new ArrayList<>();
        assert totalPage != null;
        result.add(String.valueOf(totalPage / 10));
        result.add(data);

        Gson gson = new GsonBuilder().create();
        return gson.toJson(result);
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

    @Override
    public String createChatbotThread() {
        AssistantApi assistantApi = new AssistantApi(API_KEY);
        String id = UUID.randomUUID().toString();
        Data.Thread thread = assistantApi.createThread(new Data.ThreadRequest());
        threadList.put(id, thread);

        return id;
    }

    @Override
    public String getChatbotResponse(String text, String userSessionID) throws InterruptedException {
        AssistantApi assistantApi = new AssistantApi(API_KEY);
        String temp = threadList.get(userSessionID).id();
        assistantApi.createMessage(new Data.MessageRequest(Data.Role.user, text), temp);
        Data.Run run = assistantApi.createRun(temp,
                new Data.RunRequest("asst_vG8F6Le0N4mt0DrBQMd0n1JZ"));
        while (assistantApi.retrieveRun( temp, run.id()).status() != Data.Run.Status.completed) {
            sleep(500);
        }
        Data.DataList<Data.Message> messages = assistantApi.listMessages(
                new Data.ListRequest(),  temp);
        List<Data.Message> assistantMessages = messages.data().stream()
                .filter(msg -> msg.role() == Data.Role.assistant).toList();
        return assistantMessages.getFirst().content().getFirst().text().value();
    }

    @Override
    public String showListAppointmentOfDoctor(String date, String staffID) {
        LocalDate startOfWeek, endOfWeek, appointmentDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DayOfWeek dayOfWeek;
        appointmentDate = LocalDate.parse(date, formatter);
        dayOfWeek = appointmentDate.getDayOfWeek();
        startOfWeek = appointmentDate.minusDays(dayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
        endOfWeek = startOfWeek.plusDays(6);
        Boolean[][] result = new Boolean[6][10];
        List<AppointmentRecord> appointments = context.selectFrom(APPOINTMENT)
                .where(APPOINTMENT.PHYSICIAN_ID.eq(Integer.parseInt(staffID))
                        .and(APPOINTMENT.DATE.between(startOfWeek,endOfWeek)))
                .fetchInto(AppointmentRecord.class);
        if(!appointments.isEmpty()) {
            for(AppointmentRecord appointmentRecord : appointments) {
                if(appointmentRecord.getDate().getDayOfWeek().getValue() == 1 || appointmentRecord.getTime().getHour() >= 18) continue;
                result[appointmentRecord.getDate().getDayOfWeek().getValue()-1][appointmentRecord.getTime().getHour() - 8] = true;
            }
        }
        for(int i=0; i<=5; i++){
            for(int j=0; j<=9; j++){
                if(result[i][j] == null){
                    result[i][j] = false;
                }
            }
        }
        if(appointmentDate.isEqual(LocalDate.now())){
            for( int i=0; i<=appointmentDate.getDayOfWeek().getValue()-1; i++){
                for(int j=0; j<=9; j++){
                    if(LocalDate.now().with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.of("vi-VN")).getFirstDayOfWeek()))
                            .isBefore(LocalDate.now())){
                        if(j<=LocalTime.now().getHour()-8){
                            result[i][j] = null;
                        }
                    }
                }
            }
        }
        Gson gson = new Gson();
        return gson.toJson(result);
    }

    @Override
    public boolean createChangePasswordRequest(String email, String oldPass) {
        String actualPass = context.select(ACCOUNT.ACCOUNT_PASSWORD).from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email)).fetchOneInto(String.class);

        boolean validPass = encoder.matches(oldPass, actualPass);
        if(!validPass) return false;

        UserEvent event = UserEvent.builder().eventType(EventType.CHANGE_PASSWORD).data(Map.of("patientEmail", email)).build();
        publisher.publishEvent(event);
        return true;
    }

    @Override
    public boolean verifyChangePasswordRequest(String email, String otp, String newPass) {
        boolean validOTP = Objects.requireNonNull(redisTemplate.opsForValue().get(otp)).trim().equals(email);
        if(!validOTP) return false;

        String encodedPass = encoder.encode(newPass);
        context.update(ACCOUNT).set(ACCOUNT.ACCOUNT_PASSWORD, encodedPass)
                .where(ACCOUNT.ACCOUNT_EMAIL.eq(email)).execute();
        return true;
    }
}
