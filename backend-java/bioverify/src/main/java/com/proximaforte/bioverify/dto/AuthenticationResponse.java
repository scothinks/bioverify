// FILE: src/main/java/com/proximaforte/bioverify/dto/AuthenticationResponse.java

package com.proximaforte.bioverify.dto;

public class AuthenticationResponse {
    private String token;
    public AuthenticationResponse(String token) { this.token = token; }
    // getter and setter
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}