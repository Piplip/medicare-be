package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.CustomUserDetails;
import com.nkd.medicare.exception.ApiException;
import com.nkd.medicare.tables.records.AccountRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.nkd.medicare.Tables.ACCOUNT;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final DSLContext context;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Username: " + username);
        AccountRecord account = context.selectFrom(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_EMAIL.equalIgnoreCase(username))
                .fetchAny();

        if(account == null){
            throw new ApiException("Account not found");
        }

        return new CustomUserDetails(account);
    }
}
