package com.nkd.medicare.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {
    String appointmentID;
    String diagnosis;
    List<MedicationDTO> MedicationList;
}
