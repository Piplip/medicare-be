package com.nkd.medicare.controller;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.exception.DuplicateEmailException;
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
        try{
            accountService.createAccount(credential);
        } catch (DuplicateEmailException duplicate){
            return ResponseEntity.badRequest().body(duplicate.getMessage());
        }

        return ResponseEntity.ok("Successfully create new account with email " + credential.getEmail()
                + ". Please check your email to activate your account");
    }

    @PostMapping("/login")
    public ResponseEntity<?> handleLogin(@RequestBody Credential credential){
        Credential sendBackCredential;

        if(credential.getSessionID() != null){
            try{
                sendBackCredential = accountService.loginWithSessionID(credential);
            } catch (ApiException e){
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            return ResponseEntity.ok(sendBackCredential);
        }

        try {
            sendBackCredential = accountService.login(credential);
        } catch (ApiException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok(sendBackCredential);
    }

    @GetMapping("/token/verify")
    public ResponseEntity<?> activateAccount(@RequestParam(value = "token") String token, @RequestParam(value = "email") String email){
        try {
            accountService.activateAccount(token, email);
        } catch (ApiException e){
            String errorUrl = "http://localhost:" + clientPort + "/verify/fail?error=" + e.getMessage();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(errorUrl))
                    .build();
        }

        String successUrl = "http://localhost:" + clientPort + "/verify/success";
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(successUrl))
                .build();
    }

    @PostMapping("/token/renew")
    public ResponseEntity<?> renewToken(@RequestParam(value = "account") String accountID){
        try{
            accountService.renewConfirmationToken(Integer.valueOf(accountID));
        }catch (ApiException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok("New token has been generated and send to your email! Follow the instructions in the mail to activate your account");
    }
}
