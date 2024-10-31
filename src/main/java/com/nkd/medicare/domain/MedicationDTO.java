package com.nkd.medicare.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MedicationDTO {
    private String name;
    private String dosage;
    private String frequency;
    private String route;
    private String startDate;
    private String endDate;
    private String note;
}