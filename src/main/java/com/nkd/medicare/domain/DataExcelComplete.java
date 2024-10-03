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

public class DataExcelComplete {
    private String accountID;
    private String email;
    private String password;
    private String firstname;
    private String lastname;
    private String CCCD;
    private LocalDate dateofbirth;
    private String phonenumber;
    private String staffid;
    private AccountAccountRole role;
}
