package com.nkd.medicare.event;

import com.nkd.medicare.enumeration.EventType;
import com.nkd.medicare.tables.pojos.Account;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEvent {
    private Account account;
    private EventType eventType;
    private Map<?,?> data;
}
