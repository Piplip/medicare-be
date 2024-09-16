package com.nkd.medicare.domain;
import com.nkd.medicare.tables.records.AccountRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private AccountRecord account;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(account.getAccountRole().name()));
    }

    @Override
    public String getPassword() {
        return account.getAccountPassword();
    }

    @Override
    public String getUsername() {
        return account.getAccountEmail();
    }
}
