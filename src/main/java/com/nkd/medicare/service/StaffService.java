package com.nkd.medicare.service;

import java.util.ArrayList;

public interface StaffService {

    String fetchStaffData(String staffID, String startDate, String endDate, String typeAppointment);
}
