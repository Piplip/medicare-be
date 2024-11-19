package com.nkd.medicare.service;

import com.nkd.medicare.domain.PharmacistSignal;
import com.nkd.medicare.domain.Prescription;

import java.util.List;

public interface StaffService {
    String fetchStaffData(String staffID);
    List<String> getAppointments(String staffID, String lastDate, String lastTime, String query, String startDate, String endDate);
    Prescription getAppointmentDetail(String appointmentID);
    String getStatistic(String staffID, String startDate, String endDate, String typeAppointment, String oneDate);
    List<String> suggestMedication(String query);
    Prescription createPrescription(Prescription prescription, String staffID);
    Prescription getPrescription(String appointmentID);
    PharmacistSignal completePrescribed(String prescribedID, String staffID);
    List<Prescription> getAllPrescription();
}
