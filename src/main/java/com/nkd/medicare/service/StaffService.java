package com.nkd.medicare.service;

import com.nkd.medicare.domain.Prescription;

import java.util.List;

public interface StaffService {
    String fetchStaffData(String staffID);
    String getAppointments(String staffID);
    String getStatistic(String staffID, String startDate, String endDate, String typeAppointment, String oneDate);
    List<String> suggestMedication(String query);
    Prescription createPrescription(Prescription prescription, String staffID);
}
