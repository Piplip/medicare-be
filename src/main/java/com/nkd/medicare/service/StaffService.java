package com.nkd.medicare.service;

public interface StaffService {
    String fetchStaffData(String staffID);
    String getAppointments(String staffID);
}
