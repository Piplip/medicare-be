package com.nkd.medicare.controller;

import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;
import com.nkd.medicare.service.DoctorService;
import com.nkd.medicare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class DoctorController {

    private final DoctorService DoctorService;


    @GetMapping("/doctorappointments")
    public ResponseEntity<?> getAppointmentList(@RequestParam("email") String email, @RequestParam(value = "status", required = false) String status
            , @RequestParam(value = "query", required = false) String query, @RequestParam(value = "startDate", required = false) String startDate, @RequestParam(value = "endDate", required = false) String endDate
    ){
        return ResponseEntity.ok(DoctorService.getAppointmentList(email, status, query, startDate, endDate));
    }

}
