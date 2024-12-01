package com.nkd.medicare.controller;

import com.nkd.medicare.domain.AppointmentDTO;
import com.nkd.medicare.domain.FeedbackDTO;
import com.nkd.medicare.domain.Prescription;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            , @RequestParam(value = "page", required = false) String page
    ){
        return ResponseEntity.ok(userService.getAppointmentList(email, status, query, department, startDate, endDate, page));
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

    @GetMapping("/appointment/detail")
    public ResponseEntity<?> getAppointmentDetail(@RequestParam("appointmentID") String appointmentID){
        Prescription returnValue;
        try {
            returnValue = userService.getAppointmentDetail(appointmentID);
        }catch (ApiException e){
            return ResponseEntity.badRequest().body("No such prescription for the selected appointment");
        }
        return ResponseEntity.ok(returnValue);
    }

    @PostMapping("/chatbot")
    public ResponseEntity<?> createChatbotThread(){
        HttpCookie cookie = ResponseCookie.from("USER-CHAT-THREAD-ID", userService.createChatbotThread())
                .path("/")
                .maxAge(3600 * 3)
                .build();
        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body("Chatbot thread created successfully");
    }

    @GetMapping("/chatbot")
    public String getChatbotMessage(@RequestParam("message") String message, HttpServletRequest request) {
        String returnMsg;
        Cookie[] cookies = request.getCookies();
        String id = null;
        if(cookies != null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals("USER-CHAT-THREAD-ID")){
                    id = cookie.getValue();
                    break;
                }
            }
        }
        try {
            returnMsg = userService.getChatbotResponse(message, id);
        }catch (Exception e){
            return "Sorry, I am not able to understand your question. Please try again.";
        }

        return returnMsg;
    }

    @GetMapping("/appointment/doctor/schedule")
    public ResponseEntity<?> getDoctorSchedule(@RequestParam("date") String date, @RequestParam("staffID") String staffID){
        return ResponseEntity.ok(userService.getDoctorSchedule(date, staffID));
    }

    @PostMapping("/change-password/make")
    public ResponseEntity<?> createChangePasswordRequest(@RequestParam("email") String email,
                                                         @RequestParam("oldPass") String oldPass){
        boolean goNextStep = userService.createChangePasswordRequest(email, oldPass);
        if(goNextStep)
            return ResponseEntity.ok("OK");
        else return ResponseEntity.badRequest().body("Current password is incorrect");
    }

    @PostMapping("/change-password/verify")
    public ResponseEntity<?> verifyChangePasswordRequest(@RequestParam("email") String email,
                                                         @RequestParam("OTP") String OTP, @RequestParam("newPass") String newPass){
        boolean goNextStep = userService.verifyChangePasswordRequest(email, OTP, newPass);
        if(goNextStep)
            return ResponseEntity.ok("OK");
        else return ResponseEntity.badRequest().body("OTP is incorrect");
    }

    @PostMapping("/change-profile")
    public ResponseEntity<?> changeProfile(@RequestParam("phone") String phone, @RequestParam("subPhone") String subPhone,
                                           @RequestParam("email") String email){
        userService.changeProfile(phone, subPhone, email);
        return ResponseEntity.ok("Profile updated successfully");
    }

    @GetMapping("/doctor/{staffID}/statistic")
    public String getDoctorStatistic(@PathVariable("staffID") String staffID){
        return userService.getDoctorStatistic(staffID);
    }
}
