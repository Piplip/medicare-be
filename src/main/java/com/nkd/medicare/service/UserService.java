package com.nkd.medicare.service;

import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;
import com.nkd.medicare.domain.Prescription;

public interface UserService {
    String findDoctor(String name, String department, String primaryLanguage, String specialization, String gender, String pageSize, String pageNumber);
    String findDoctorWithID(String id);
    String makeAppointment(AppointmentDTO appointmentDTO);
    String getUserProfile(String email);
    String getAppointmentList(String email, String status, String query, String department, String startDate, String endDate);
    void postFeedback(FeedbackDTO feedbackDTO, String email);
    String getFeedbacks(String email);
    Prescription getPrescripton(String appointmentID);
    String getChatbotRespone(String text, String email) throws InterruptedException;
    String deleteHistoryChatbot(String email);
}
