package com.nkd.medicare.event.listener;

import com.nkd.medicare.event.UserEvent;
import com.nkd.medicare.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;

    @EventListener
    public void handleAccountEvent(UserEvent userEvent){
        switch (userEvent.getEventType()){
            case REGISTRATION -> emailService.sendRegistrationEmail((String) userEvent.getData().get("token")
                    , (String) userEvent.getData().get("email")
                    , (LocalDateTime) userEvent.getData().get("expirationTime"));
            case RENEW_TOKEN -> emailService.sendRenewTokenEmail((String) userEvent.getData().get("token")
                    , (String) userEvent.getData().get("email")
                    , (LocalDateTime) userEvent.getData().get("expirationTime"));
            case MAKE_APPOINTMENT -> emailService.sendAppointmentConfirmationEmail((String) userEvent.getData().get("patientEmail")
                    , (String) userEvent.getData().get("doctorEmail")
                    , (String) userEvent.getData().get("patientName")
                    , (String) userEvent.getData().get("date")
                    , (String) userEvent.getData().get("time")
                    , (String) userEvent.getData().get("reason"));
        }
    }
}
