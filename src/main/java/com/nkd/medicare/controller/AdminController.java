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
                                      @RequestParam("page-number") String pageNumber, @RequestParam("staff-type") String staffType){
        return ResponseEntity.ok(adminService.getStaff(name, department, primaryLanguage, specialization, gender, pageSize, pageNumber, staffType));
    }

    @PostMapping("/excel")
    public ResponseEntity<?> handleExcel(@RequestParam(value = "url") String url){
        List<StaffExcelData> dulieuthanhcong;
        try{
            dulieuthanhcong = adminService.readFromExcel(url);
        } catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(dulieuthanhcong);
    }

    @DeleteMapping("/staff")
    public ResponseEntity<?> deleteStaff(@RequestParam("id") String staffID){
        adminService.deleteStaff(staffID);
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
}
