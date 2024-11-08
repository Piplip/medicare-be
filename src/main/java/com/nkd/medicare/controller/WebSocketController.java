package com.nkd.medicare.controller;

import com.nkd.medicare.domain.MedicationDTO;
import com.nkd.medicare.domain.Prescription;
import com.nkd.medicare.service.PharmacistService;
import com.nkd.medicare.service.StaffService;
import com.nkd.medicare.service.impl.StaffServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff")
public class WebSocketController {
    private final StaffService staffService;
    private final PharmacistService pharmacistService;


    @MessageMapping("/create/prescription")
    @SendTo("/pharmacist")
    public ResponseEntity<?> handlePrescribed(@RequestBody Prescription prescription, HttpServletRequest request) {
            String staffID = getCookie(request);
            return ResponseEntity.ok(staffService.createPrescription(prescription,staffID));
    }
    @PostMapping("/get/prescription")
    public ResponseEntity<?> handleBeforeEditPrescribed(@RequestParam("prescribedID") String prescribedID){
        return ResponseEntity.ok(prescribedID);
    }
    @MessageMapping("/complete/prescription")
    @SendTo("/pharmacist")
    public ResponseEntity<?> handleCompletePrescribed(@RequestParam("prescribedID") String prescribedID) {
        return ResponseEntity.ok(pharmacistService.completePrescribed(prescribedID));
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
