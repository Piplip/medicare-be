package com.nkd.medicare.controller;

import com.nkd.medicare.domain.Prescription;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.service.StaffService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchStaffData(HttpServletRequest request){
        String staffID = getCookie(request);

        if(staffID == null)
            return ResponseEntity.badRequest().body("Please login first.");

        String staffData = staffService.fetchStaffData(staffID);
        return ResponseEntity.ok(staffData);
    }

    @GetMapping("/fetch/appointments")
    public ResponseEntity<?> getAppointments(HttpServletRequest request, @RequestParam(value = "page", required = false) String page,
                                             @RequestParam(value = "size", required = false) String pageSize,
                                             @RequestParam(value = "query", required = false) String query,
                                             @RequestParam(value = "startDate", required = false) String startDate,
                                             @RequestParam(value = "endDate", required = false) String endDate){
        String staffID = getCookie(request);

        var appointmentData = staffService.getAppointments(staffID, page, pageSize, query, startDate, endDate);
        return ResponseEntity.ok(appointmentData);
    }

    @GetMapping("/appointment/detail")
    public ResponseEntity<?> getAppointmentDetail(@RequestParam("appointmentID") String appointmentID){
        Prescription result;
        try {
            result = staffService.getAppointmentDetail(appointmentID);
        }catch (ApiException e){
            return ResponseEntity.badRequest().body("No such prescription for the selected appointment");
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/fetch/statistic")
    public ResponseEntity<?> getStatistic(HttpServletRequest request, @RequestParam(value = "startDate", required = false) String startDate,
                                          @RequestParam(value = "endDate", required = false) String endDate,
                                          @RequestParam(value = "view", required = false) String viewType,
                                            @RequestParam(value = "date", required = false) String date){
        String staffID = getCookie(request);
        return ResponseEntity.ok(staffService.getStatistic(staffID, startDate, endDate, viewType, date));
    }

    @GetMapping("/medication/suggest")
    public ResponseEntity<?> findMedication(@RequestParam("q") String query){
        return ResponseEntity.ok(staffService.suggestMedication(query));
    }

    @GetMapping("/get/prescription/all")
    public ResponseEntity<?> getAllPrescription(){
        return ResponseEntity.ok(staffService.getAllPrescription());
    }

    @GetMapping("/get/prescription")
    public ResponseEntity<?> getMedication(@RequestParam("appointmentID") String appointmentID){
        return ResponseEntity.ok(staffService.getPrescription(appointmentID));
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
