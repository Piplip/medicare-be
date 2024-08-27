package com.nkd.medicare.domain;

import com.nkd.medicare.enums.AccountAccountRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    private String email;
    private boolean isOnline;
    private String sessionID;
    private AccountAccountRole accountRole;
    private LocalDateTime createdTime;
    private LocalDateTime lastAccessTime;
    private LocalDateTime expirationTime;
}
