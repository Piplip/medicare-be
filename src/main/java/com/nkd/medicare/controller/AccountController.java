package com.nkd.medicare.controller;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.domain.Session;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.exception.DuplicateEmailException;
import com.nkd.medicare.service.AccountService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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
        Session sendbackSession;

        if(credential.getSessionID() != null){
            try{
                sendbackSession = accountService.loginWithSessionID(credential);
            } catch (ApiException e){
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            return ResponseEntity.ok(sendbackSession);
        }

        try {
            System.out.println("Login with credential");
            sendbackSession = accountService.login(credential);
        } catch (ApiException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok(sendbackSession);
    }

    @PostMapping("/staff/login")
    public ResponseEntity<?> handleStaffLogin(@RequestBody Credential credential){
        Session successSession = accountService.staffLogin(credential);

        HttpCookie cookie = ResponseCookie.from("STAFF-ID", successSession.getUserID().toString())
                .path("/")
                .maxAge(3600)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(successSession);
    }

    @GetMapping("/token/verify")
    public ResponseEntity<?> activateAccount(@RequestParam(value = "token") String token, @RequestParam(value = "email") String email){
        String accountID;
        try {
            accountID = accountService.activateAccount(token, email);
        } catch (ApiException e){
            String errorUrl = "http://localhost:" + clientPort + "/verify/fail?error=" + e.getMessage();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(errorUrl))
                    .build();
        }

        String successUrl = "http://localhost:" + clientPort + "/verify/success?" + "accountID=" + accountID;
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

    @PostMapping("/check-identity")
    public ResponseEntity<?> checkCCCD(@RequestParam(value = "accountID") String accountID, @RequestParam(value = "url") String url){
        try {
            accountService.checkCCCD(accountID, url);
        }
        catch(ApiException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok("Identity have been authenticated");
    }
}
