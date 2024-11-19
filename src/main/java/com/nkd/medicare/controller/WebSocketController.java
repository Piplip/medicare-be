package com.nkd.medicare.controller;

import com.nkd.medicare.domain.PharmacistSignal;
import com.nkd.medicare.domain.Prescription;
import com.nkd.medicare.service.StaffService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WebSocketController {

    private final StaffService staffService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @PostMapping("/create/prescription")
    public ResponseEntity<?> handlePrescribed(@RequestBody Prescription prescription, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        String staffID = null;
        if(cookies != null){
            for (Cookie c : cookies){
                if(c.getName().equals("STAFF-ID"))
                    staffID = c.getValue();
            }
        }
        Prescription returnPrescription = staffService.createPrescription(prescription, staffID);
        PharmacistSignal pharmacistSignal = returnPrescription != null ?
                new PharmacistSignal("CP-100", Map.of("prescription", returnPrescription)) :
                new PharmacistSignal("CP-400", Map.of("error", "Failed to create prescription"));
        simpMessagingTemplate.convertAndSend("/prescriptions", pharmacistSignal);
        return ResponseEntity.ok(returnPrescription);
    }

    @PostMapping("/complete/prescription")
    public ResponseEntity<?> handleCompletePrescribed(@RequestParam("prescribedID") String prescribedID, @RequestParam("staffID") String staffID) {
        PharmacistSignal signal = staffService.completePrescribed(prescribedID, staffID);
        simpMessagingTemplate.convertAndSend("/prescriptions", signal);
        return ResponseEntity.ok(signal);
    }

    @PostMapping("/view/prescription")
    public void handleViewPrescription(@RequestParam("prescribedID") String prescribedID, @RequestParam("name") String pharmacistName
            , @RequestParam("id") String pharmacistID){
        simpMessagingTemplate.convertAndSend("/prescriptions", new PharmacistSignal("MC-1001"
                , Map.of("prescribedID", prescribedID, "name", pharmacistName, "viewPersonID", pharmacistID)));
    }

    @PostMapping("/leave/prescription")
    public void handleLeavePrescription(@RequestParam("prescribedID") String prescribedID, @RequestParam("name") String pharmacistName
            , @RequestParam("id") String pharmacistID){
        simpMessagingTemplate.convertAndSend("/prescriptions", new PharmacistSignal("MC-1002"
                , Map.of("prescribedID", prescribedID, "name", pharmacistName, "viewPersonID", pharmacistID)));
    }
}