package com.nkd.medicare.service;

import com.nkd.medicare.domain.MedicationDTO;

import java.util.ArrayList;
import java.util.List;

public interface StaffService {

    String fetchStaffData(String staffID, String startDate, String endDate, String typeAppointment, String oneDay);
    String suggestMedication(String nameMedication);
    String createPrescribed(List<MedicationDTO> listMedication, String staffID, String diagonis);
    String addMedication(String nameMedication);
}
