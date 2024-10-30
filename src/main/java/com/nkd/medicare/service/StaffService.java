package com.nkd.medicare.service;

import com.nkd.medicare.domain.MedicationDTO;
import com.nkd.medicare.domain.Prescription;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public interface StaffService {

    String fetchStaffData(String staffID, String startDate, String endDate, String typeAppointment, String oneDay);
    List<List<String>> suggestMedication(String nameMedication);
    String createPrescribed(Prescription prescription, String staffID);
    List<List<String>> addMedication(String nameMedication);
}
