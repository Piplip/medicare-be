package com.nkd.medicare.controller;

import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;
import com.nkd.medicare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffData(@RequestParam("name") String name, @RequestParam("department") String department,
                                          @RequestParam("primary-language") String primaryLanguage, @RequestParam("specialization") String specialization,
                                          @RequestParam("gender") String gender, @RequestParam("page-size") String pageSize,
                                          @RequestParam("page-number") String pageNumber){
        return ResponseEntity.ok(userService.findDoctor(name, department, primaryLanguage, specialization, gender, pageSize, pageNumber));
    }

    @GetMapping("/staff/id")
    public ResponseEntity<?> getDoctorWithID(@RequestParam("id") String staffID){
        return ResponseEntity.ok(userService.findDoctorWithID(staffID));
    }

    @PostMapping("/appointment")
    public ResponseEntity<?> makeAppointment(@RequestBody AppointmentDTO appointmentDTO){
        String msg = userService.makeAppointment(appointmentDTO);
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestParam("email") String email){
        return ResponseEntity.ok(userService.getUserProfile(email));
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> getAppointmentList(@RequestParam("email") String email, @RequestParam(value = "status", required = false) String status
            , @RequestParam(value = "query", required = false) String query, @RequestParam(value = "department", required = false) String department
            , @RequestParam(value = "startDate", required = false) String startDate, @RequestParam(value = "endDate", required = false) String endDate
    ){
        return ResponseEntity.ok(userService.getAppointmentList(email, status, query, department, startDate, endDate));
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> postFeedback(@RequestBody FeedbackDTO feedbackDTO, @RequestParam("email") String email){
        userService.postFeedback(feedbackDTO, email);
        return ResponseEntity.ok("Feedback posted successfully");
    }

    @GetMapping("/feedbacks")
    public ResponseEntity<?> getFeedbacks(@RequestParam("email") String email){
        return ResponseEntity.ok(userService.getFeedbacks(email));
    }
}
