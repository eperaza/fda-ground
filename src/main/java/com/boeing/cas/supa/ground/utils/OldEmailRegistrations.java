package com.boeing.cas.supa.ground.utils;

import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class OldEmailRegistrations {

    public HashSet<String> oldRegistrationAirlines;

    public OldEmailRegistrations(){
        this.oldRegistrationAirlines = new HashSet<>();
        this.oldRegistrationAirlines.add("airline-amx");
        this.oldRegistrationAirlines.add("airline-cnd");
    }

    public boolean containsAirline(String airline) {
        if(this.oldRegistrationAirlines.contains(airline)){
            return true;
        }
        return false;
    }
}
