package com.nkd.medicare.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nkd.medicare.enums.AccountAccountRole;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Session implements Serializable {

    private String email;
    private boolean isOnline;
    private String sessionID;
    private String firstName;
    private String lastName;
    private AccountAccountRole accountRole;
    private LocalDateTime createdTime;
    private LocalDateTime lastAccessTime;
    private LocalDateTime expirationTime;

    public Session(Session session){
        this.firstName = session.getFirstName();
        this.lastName = session.getLastName();
        this.email = session.getEmail();
        this.sessionID = session.getSessionID();
    }
}
