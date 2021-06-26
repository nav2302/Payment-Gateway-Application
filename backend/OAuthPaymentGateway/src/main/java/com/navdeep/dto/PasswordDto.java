package com.navdeep.dto;

import com.navdeep.validator.ValidPassword;

import lombok.Value;

@Value
public class PasswordDto {

    private  String token;

    @ValidPassword
    private String newPassword;
    
    private String confirmPassword;
    
    
}