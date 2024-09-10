package com.nkd.medicare.controller;

import com.nkd.medicare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffData(@RequestParam("name") String name, @RequestParam("department") String department,
                                          @RequestParam("primary-language") String primaryLanguage, @RequestParam("specialization") String specialization,
                                          @RequestParam("gender") String gender, @RequestParam("page-size") String pageSize, @RequestParam("page-number") String pageNumber){
        return ResponseEntity.ok(userService.getStaffData(name, department, primaryLanguage, specialization, gender, pageSize, pageNumber));
    }
}
