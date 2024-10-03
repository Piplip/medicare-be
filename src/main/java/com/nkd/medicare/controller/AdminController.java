package com.nkd.medicare.controller;

import com.nkd.medicare.domain.Credential;
import com.nkd.medicare.domain.DataExcelComplete;
import com.nkd.medicare.exception.DuplicateEmailException;
import com.nkd.medicare.service.impl.AdminServicelmpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminServicelmpl adminservice;
    private Map<String, DataExcelComplete> dulieuthanhcong;
    @PostMapping("/excel")
    public Map<String, DataExcelComplete> handleExcel(@RequestParam(value = "url") String url){
        try{
            this.dulieuthanhcong = adminservice.readexcel(url);
        } catch (Exception e){
            ResponseEntity.ok("Loi he thong");
            return null;
        }
        return dulieuthanhcong;
    }
}
