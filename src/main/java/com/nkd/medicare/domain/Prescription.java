package com.nkd.medicare.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prescription implements Serializable {

    private String appointmentID;
    private String diagnosis;
    private LocalDate prescribedDate;
    private String doctorName;
    private List<MedicationDTO> medicationList;
    private String status;
}
