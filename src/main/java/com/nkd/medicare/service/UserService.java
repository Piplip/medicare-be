package com.nkd.medicare.service;

import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;
import com.nkd.medicare.domain.Prescription;

import java.util.List;

public interface UserService {
    List<String> findDoctor(String name, String department, String primaryLanguage, String specialization, String gender, String pageSize, String pageNumber);
    String findDoctorWithID(String id);
    String makeAppointment(AppointmentDTO appointmentDTO);
    Prescription getAppointmentDetail(String appointmentID);
    String getUserProfile(String email);
    String getAppointmentList(String email, String status, String query, String department, String startDate, String endDate, String page);
    void postFeedback(FeedbackDTO feedbackDTO, String email);
    String getFeedbacks(String email);
    String createChatbotThread();
    String getChatbotResponse(String text, String threadID) throws InterruptedException;
    String showListAppointmentOfDoctor(String date, String staffID);
    boolean createChangePasswordRequest(String email, String oldPass);
    boolean verifyChangePasswordRequest(String email, String otp, String newPass);
}
