package com.nkd.medicare.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class StaffExcelData {
    private String accountID;
    private String staffID;
    private String firstname;
    private String lastname;
    private LocalDate dateOfBirth;
    private String CCCD;
    private String phoneNumber;
    private String email;
    private String password;
    private AccountAccountRole role;
    private int resultType;
}