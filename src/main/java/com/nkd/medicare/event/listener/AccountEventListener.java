package com.nkd.medicare.event.listener;

import com.nkd.medicare.event.AccountEvent;
import com.nkd.medicare.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AccountEventListener {

    private final EmailService emailService;

    @EventListener
    public void handleAccountEvent(AccountEvent accountEvent){
        switch (accountEvent.getEventType()){
            case REGISTRATION -> emailService.sendRegistrationEmail((String) accountEvent.getData().get("token")
                    , (String) accountEvent.getData().get("email")
                    , (LocalDateTime) accountEvent.getData().get("expirationTime"));
        }
    }
}
