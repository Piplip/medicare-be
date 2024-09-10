package com.nkd.medicare.service;

public interface UserService {
    String getStaffData(String name, String department, String primaryLanguage, String specialization, String gender, String pageSize, String pageNumber);
}
