package com.boeing.cas.supa.ground.pojos;

import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

public class AircraftConfigurationProperty {
    private static final String PlatformProperty = "platform";
    private static final String PropertiesProperty = "properties";
    
    @SerializedName(PlatformProperty)
    public String platform;
    
    @SerializedName(PropertiesProperty)
    public HashMap<String, Object> properties;
    
    public AircraftConfigurationProperty() {
    	this.properties = new HashMap<String, Object>();
    }
}
