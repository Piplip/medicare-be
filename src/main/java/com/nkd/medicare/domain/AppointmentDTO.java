package com.nkd.medicare.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AppointmentDTO {

    private String patientEmail;
    private String doctorID;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String reason;
    private String isReferral;
    private String isReminder;
}
