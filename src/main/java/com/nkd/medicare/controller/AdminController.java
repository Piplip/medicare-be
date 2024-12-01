package com.nkd.medicare.controller;

import com.nkd.medicare.domain.StaffDTO;
import com.nkd.medicare.domain.StaffExcelData;
import com.nkd.medicare.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/staff")
    public ResponseEntity<?> getStaff(@RequestParam("name") String name, @RequestParam("department") String department,
                                      @RequestParam("primary-language") String primaryLanguage, @RequestParam("specialization") String specialization,
                                      @RequestParam("gender") String gender, @RequestParam("page-size") String pageSize,
                                      @RequestParam("page-number") String pageNumber, @RequestParam("staff-type") String staffType,
                                      @RequestParam("staff-status") String status, @RequestParam(value = "staff-id", required = false) String staffID){
        boolean isAdmin = false;
        if(staffID != null){
            isAdmin = adminService.checkAdmin(staffID);
        }
        if(isAdmin){
            return ResponseEntity.badRequest().body("You are not authorized to view this page");
        }
        return ResponseEntity.ok(adminService.getStaff(name, department, primaryLanguage, specialization, gender, pageSize, pageNumber, staffType, status));
    }

    @PostMapping("/staff/add")
    public ResponseEntity<?> createStaffFromFile(@RequestParam(value = "url") String fileURL){
        List<StaffExcelData> returnData;
        try{
            returnData = adminService.readFromExcel(fileURL);
        } catch (Exception e){
            return ResponseEntity.badRequest().body("Error reading excel file");
        }
        return ResponseEntity.ok(returnData);
    }

    @DeleteMapping("/staff/delete")
    public ResponseEntity<?> deleteStaff(@RequestParam("id") String staffID, @RequestParam("note") String note){
        System.out.println("staff ID " + staffID);
        System.out.println("Note " + note);
        adminService.deleteStaff(staffID, note);
        return ResponseEntity.ok("Staff deleted successfully");
    }

    @GetMapping("/staff/get")
    public ResponseEntity<?> getStaffByID(@RequestParam("id") String staffID){
        return ResponseEntity.ok(adminService.getStaffByID(staffID));
    }

    @PatchMapping("/staff/update")
    public ResponseEntity<?> updateStaff(@RequestBody StaffDTO staffDTO){
        adminService.updateStaff(staffDTO);
        return ResponseEntity.ok("Staff updated successfully");
    }

    @PatchMapping("/staff/update/profile-img")
    public ResponseEntity<?> updateStaffProfileImage(@RequestParam("id") String staffID, @RequestParam("imageURL") String imageURL){
        adminService.updateStaffProfileImage(staffID, imageURL);
        return ResponseEntity.ok("Staff profile image updated successfully");
    }
}
