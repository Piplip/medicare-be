package com.nkd.medicare.controller;

import com.nkd.medicare.domain.MedicationDTO;
import com.nkd.medicare.service.StaffService;
import com.nkd.medicare.tables.Medication;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchStaffData(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        String staffID = null;

        if(cookies != null){
            for (Cookie c : cookies){
                if("STAFF-ID".equals(c.getName())){
                    staffID = c.getValue();
                    break;
                }
            }
        }
        String startDate = request.getParameter("startDay");
        String endDate = request.getParameter("endDay");
        String type = request.getParameter("type");
        String oneDate = request.getParameter("oneDay");
        String staffData = staffService.fetchStaffData(staffID, startDate, endDate, type, oneDate);
        return ResponseEntity.ok(staffData);
    }
    @GetMapping("/createMedication/suggest")
    public ResponseEntity<?> findMedication(@RequestParam("input")  String medication){
        return ResponseEntity.ok(staffService.suggestMedication(medication));
    }
    @GetMapping("/createMedication")
    public ResponseEntity<?> createMedication(@RequestBody List<MedicationDTO> listmedication, HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        String staffID = null;

        if(cookies != null){
            for (Cookie c : cookies){
                if("STAFF-ID".equals(c.getName())){
                    staffID = c.getValue();
                    break;
                }
            }
        }
        String diagonis = request.getParameter("diagonis");
        return ResponseEntity.ok(staffService.createPrescribed(listmedication,staffID,diagonis));
    }
    @GetMapping("/createMedication/addMedication")
    public ResponseEntity<?> addMedication(@RequestParam("input")  String medication){
        return ResponseEntity.ok(staffService.addMedication(medication));
    }
}
