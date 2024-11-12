package com.nkd.medicare.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.Data;
import com.logaritex.ai.api.FileApi;
import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;
import com.nkd.medicare.domain.Prescription;
import com.nkd.medicare.enumeration.EventType;
import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.enums.StaffStaffType;
import com.nkd.medicare.event.UserEvent;
import com.nkd.medicare.service.UserService;
import com.nkd.medicare.tables.records.AppointmentRecord;
import com.nkd.medicare.tables.records.FeedbackRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final DSLContext context;
    private final ApplicationEventPublisher publisher;
    private final StaffServiceImpl showPrescripton;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";
    private Map<String, Data.Thread> threadList= new HashMap<>();
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

        String doctorEmail = context.select(ACCOUNT.ACCOUNT_EMAIL)
                .from(ACCOUNT.join(STAFF).on(ACCOUNT.OWNER_ID.eq(STAFF.STAFF_ID)))
                .where(STAFF.STAFF_ID.eq(Integer.parseInt(appointmentDTO.getDoctorID())))
                .fetchOneInto(String.class);

        assert doctorEmail != null;
        Map<?, ?> data = Map.of("patientEmail", appointmentDTO.getPatientEmail(), "doctorEmail", doctorEmail,
            "patientName", appointmentDTO.getPatientName(), "date", appointmentDTO.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                , "time", appointmentDTO.getAppointmentTime().format(DateTimeFormatter.ofPattern("HH:ss")), "reason", appointmentDTO.getReason());
        UserEvent makeAppointmentEvent = UserEvent.builder()
                        .eventType(EventType.MAKE_APPOINTMENT)
                        .data(data)
                        .build();
        publisher.publishEvent(makeAppointmentEvent);

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

    @Override
    public Prescription getPrescripton(String appointmentID) {
        return showPrescripton.showPrescription(appointmentID);
    }
    @Override
    public String getChatbotRespone(String text, String email) throws InterruptedException {
        AssistantApi assistantApi = new AssistantApi(API_KEY);
        if(!threadList.containsKey(email)) {
            Data.Thread thread = assistantApi.createThread(new Data.ThreadRequest());
            threadList.put(email,thread);
        }
        assistantApi.createMessage(new Data.MessageRequest(
                        Data.Role.user,
                        text),
                threadList.get(email).id());
        Data.Run run = assistantApi.createRun(
                threadList.get(email).id(),
                new Data.RunRequest("asst_vG8F6Le0N4mt0DrBQMd0n1JZ"));
        while (assistantApi.retrieveRun( threadList.get(email).id(), run.id()).status() != Data.Run.Status.completed) {
            java.lang.Thread.sleep(500);
        }
        Data.DataList<Data.Message> messages = assistantApi.listMessages(
                new Data.ListRequest(),  threadList.get(email).id());
        List<Data.Message> assistantMessages = messages.data().stream()
                .filter(msg -> msg.role() == Data.Role.assistant).toList();
        return assistantMessages.getLast().content().getLast().text().value();
    }
    @Override
    public String deleteHistoryChatbot(String email) {
        Boolean check = threadList.remove(email, threadList.get(email));
        if(check) return "Da xoa lich su chatbot";
        else return "Xoa lich su chatbot that bai";
    }

    @Override
    public String showListAppointmentOfDoctor(String date, String staffID) {
        LocalDate startOfWeek, endOfWeek, appointmentDate;
        String dayName;
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
                if(appointmentRecord.getDate().getDayOfWeek().getValue() == 1) continue;
                result[appointmentRecord.getDate().getDayOfWeek().getValue()-2][appointmentRecord.getTime().getHour()-8] = true;
            }
            for(int i=0; i<=5; i++){
                for(int j=0; j<=9; j++){
                    if(result[i][j] == null){
                        result[i][j] = false;
                    }
                }
            }
        }
        Gson gson = new Gson();
        return gson.toJson(result);
    }
}
