package com.nkd.medicare.service;

import com.nkd.medicare.domain.PharmacistSignal;

public interface PharmacistService {
    String fetchStaffData(String staffID);
    PharmacistSignal completePrescribed(String prescribedID);
}
