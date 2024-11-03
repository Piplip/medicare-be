package com.nkd.medicare.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prescription implements Serializable {
    private String age;
    private String fullname;
    private String address;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String gender;
    private String appointmentID;
    private String prescribedID;
    private String diagnosis;
    private LocalDateTime prescribedTime;
    private String doctorName;
    private List<MedicationDTO> medicationList;
    private String status;
}
