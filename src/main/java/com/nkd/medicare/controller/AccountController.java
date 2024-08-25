package com.nkd.medicare.controller;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class AccountController {

    private final AccountService accountService;

    @Value("${spring.client.port}")
    private String clientPort;

    @PostMapping("/sign-up")
    public ResponseEntity<?> handleSignUp(@RequestBody Credential credential){
        accountService.createAccount(credential);
        return ResponseEntity.ok("Successfully create new account with email " + credential.getEmail());
    }

    @GetMapping("/verify")
    public ResponseEntity<?> activateAccount(@RequestParam(value = "token") String token){
        accountService.activateAccount(token);
        String redirectUrl = "http://localhost:" +clientPort + "/verify/success";
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }
}
