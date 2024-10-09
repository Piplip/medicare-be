package com.nkd.medicare.service;

import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;

public interface UserService {
    String findDoctor(String name, String department, String primaryLanguage, String specialization, String gender, String pageSize, String pageNumber);
    String findDoctorWithID(String id);
    String makeAppointment(AppointmentDTO appointmentDTO);
    String getUserProfile(String email);
    String getAppointmentList(String email);
    void postFeedback(FeedbackDTO feedbackDTO, String email);
}
