package com.nkd.medicare.service;

public interface DoctorService {
    String getAppointmentList(String email, String status, String query, String startDate, String endDate);
}
