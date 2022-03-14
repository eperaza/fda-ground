package com.boeing.cas.supa.ground.pojos;


public class PreUserAccount {
	String registrationToken = null;
	String userId = null;
    String first = null;
	String last = null;
	String email = null;
	String accountState = null;
	String airline = null;
    String role = null;

    public PreUserAccount(String userId, String first, String last, String email, String accountState, String airline,
            String role) {
        this.userId = userId;
        this.first = first;
        this.last = last;
        this.email = email;
        this.accountState = accountState;
        this.airline = airline;
        this.role = role;
    }

    public PreUserAccount(){
        
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountState() {
        return accountState;
    }

    public void setAccountState(String accountState) {
        this.accountState = accountState;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    


}
