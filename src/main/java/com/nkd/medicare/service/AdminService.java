package com.nkd.medicare.service;

import com.nkd.medicare.domain.StaffDTO;
import com.nkd.medicare.domain.StaffExcelData;
import org.apache.poi.ss.usermodel.Row;

import java.util.List;

public interface AdminService {
    List<String> getStaff(String name, String department, String primaryLanguage, String specialization
            , String gender, String pageSize, String pageNumber, String staffType, String status);
    List<StaffExcelData> readFromExcel(String url);
    StaffExcelData addStaff(Row Url) throws Exception;
    void deleteStaff(String staffID, String note);
    String getStaffByID(String staffID);
    void updateStaff(StaffDTO staffDTO);
    void updateStaffProfileImage(String staffID, String imageURL);
    boolean checkAdmin(String staffID);
}
