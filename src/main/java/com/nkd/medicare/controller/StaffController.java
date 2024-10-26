package com.nkd.medicare.controller;

import com.nkd.medicare.service.StaffService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchStaffData(HttpServletRequest request){
        String staffID = getCookie("STAFF-ID", request);

        String staffData = staffService.fetchStaffData(staffID);
        return ResponseEntity.ok(staffData);
    }

    @GetMapping("/fetch/appointments")
    public ResponseEntity<?> getTodayAppointment(HttpServletRequest request){
        String staffID = getCookie("STAFF-ID", request);

        var appointmentData = staffService.getAppointments(staffID);
        return ResponseEntity.ok(appointmentData);
    }

    private String getCookie(String cookieName, HttpServletRequest request){
        Cookie[] cookies = request.getCookies();

        if(cookies != null){
            for (Cookie c : cookies){
                if(c.getName().equals(cookieName))
                    return c.getValue();
            }
        }

        return null;
    }
}
