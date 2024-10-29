package com.nkd.medicare.domain;

import com.nkd.medicare.enums.AccountAccountRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class MedicationDTO {
    private String medicationname;
    private String dosage;
    private String frequency;
    private String route;
    private String startDate;
    private String endDate;
    private String allow_refill;
    private String note;
}
