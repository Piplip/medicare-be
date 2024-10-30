package com.nkd.medicare.controller;

import com.nkd.medicare.domain.MedicationDTO;
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
    @MessageMapping("/prescribed")
    @SendTo("/staffconversation")
    public String handlePrescribed(@RequestBody List<MedicationDTO> listmedication, HttpServletRequest request, @RequestParam("diagonis")  String diagonis){
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
        return staffService.createPrescribed(listmedication,staffID,diagonis);
    }
}
