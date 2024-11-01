package com.nkd.medicare.domain;

import com.nkd.medicare.tables.records.PersonRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionWithPatient implements Serializable {
    private String age;
    private String fullname;
    private String address;
    private PersonRecord preson;
    private Prescription prescription;
}
