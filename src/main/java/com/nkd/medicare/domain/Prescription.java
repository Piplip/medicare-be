package com.nkd.medicare.domain;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Prescription implements Serializable {

    private String age;
    private String name;
    private String address;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String gender;
    private String prescribedID;
    private String appointmentID;
    private String diagnosis;
    private String prescribedTime;
    private String doctorName;
    private List<MedicationDTO> medicationList;
    private String status;
}
