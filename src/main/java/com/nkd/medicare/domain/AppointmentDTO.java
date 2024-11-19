package com.nkd.medicare.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AppointmentDTO {

    private String patientEmail;
    private String patientName;
    private String doctorID;
    private String appointmentDate;
    private String appointmentTime;
    private String reason;
    private String isReferral;
    private String isReminder;
}
