package com.nkd.medicare.controller;

import com.nkd.medicare.service.StaffService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

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
        String staffData = staffService.fetchStaffData(staffID, startDate, endDate, type);
        return ResponseEntity.ok(staffData);
    }

}
