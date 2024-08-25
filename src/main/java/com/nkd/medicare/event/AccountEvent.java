package com.nkd.medicare.event;

import com.nkd.medicare.enumeration.EventType;
import com.nkd.medicare.tables.pojos.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountEvent {
    private Account account;
    private EventType eventType;
    private Map<?,?> data;
}
