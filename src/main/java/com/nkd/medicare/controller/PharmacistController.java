package com.nkd.medicare.controller;

import com.nkd.medicare.service.PharmacistService;
import com.nkd.medicare.service.StaffService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pharamist")
public class PharmacistController {

    private final PharmacistService pharmacistService;

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchStaffData(HttpServletRequest request){
        String staffID = getCookie(request);

        if(staffID == null)
            return ResponseEntity.badRequest().body("Please login first.");

        String staffData = pharmacistService.fetchStaffData(staffID);
        return ResponseEntity.ok(staffData);
    }
    private String getCookie(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();

        if(cookies != null){
            for (Cookie c : cookies){
                if(c.getName().equals("STAFF-ID"))
                    return c.getValue();
            }
        }

        return null;
    }
}
