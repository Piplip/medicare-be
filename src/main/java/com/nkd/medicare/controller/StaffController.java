package com.nkd.medicare.controller;

import com.nkd.medicare.domain.Prescription;
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
    public ResponseEntity<?> getTodayAppointment(HttpServletRequest request){
        String staffID = getCookie(request);

        var appointmentData = staffService.getAppointments(staffID);
        return ResponseEntity.ok(appointmentData);
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

    @PostMapping("/create/prescription")
    public ResponseEntity<?> createMedication(@RequestBody Prescription prescription, HttpServletRequest request){
        String staffID = getCookie(request);
        return ResponseEntity.ok(staffService.createPrescription(prescription, staffID));
    }
    @PostMapping("/get/prescription")
    public ResponseEntity<?> getMedication(@RequestParam("appointmentId") String appointmentID, HttpServletRequest request){
        String staffID = getCookie(request);
        return ResponseEntity.ok(staffService.showPrescription(appointmentID));
    }
    @PostMapping("/edit/prescription")
    public ResponseEntity<?> editMedication(@RequestParam("prescribedID") String prescribedID, HttpServletRequest request){
        String staffID = getCookie(request);
        return ResponseEntity.ok(staffService.editPrescribed(prescribedID));
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
