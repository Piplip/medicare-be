package com.nkd.medicare.domain;

import jakarta.annotation.Nonnull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@ToString
public class StaffDTO {
    private String staffID;
    private String staffType;
    private LocalDate endDate;

    private String accountID;
    private String idNumber;
    private String email;

    private String personID;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String secPhoneNumber;
    private String primaryLanguage;

    private String addressID;
    private String houseNumber;
    private String street;
    private String district;
    private String city;
    private String province;
}
