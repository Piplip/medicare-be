package com.nkd.medicare.service;

import com.nkd.medicare.domain.AppointmentDTO;

public interface UserService {
    String findDoctor(String name, String department, String primaryLanguage, String specialization, String gender, String pageSize, String pageNumber);
    String findDoctorWithID(String id);
    String makeAppointment(AppointmentDTO appointmentDTO);
}
