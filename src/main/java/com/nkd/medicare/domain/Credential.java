package com.nkd.medicare.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Credential {

    private String email;
    private String password;
    private String sessionID;

    @JsonIgnore
    public String getPassword(){
        return password;
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }
}
