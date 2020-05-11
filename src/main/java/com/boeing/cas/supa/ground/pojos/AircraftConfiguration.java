package com.boeing.cas.supa.ground.pojos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.gson.annotations.SerializedName;

public class AircraftConfiguration {
	private static final String TailNumberProperty = "aircraft.tailNumber";
	private static final String CarrierProperty = "aircraft.carrier";
	private static final String TypeProperty = "aicraft.type";
	private static final String ConfigurationsProperty = "configurations";
	
    @SerializedName(TailNumberProperty)
    public String tailNumber;
    
    @SerializedName(CarrierProperty)
    public String carrier;
    
    @SerializedName(TypeProperty)
    public String type;
    
    @SerializedName(ConfigurationsProperty)
    public List<AircraftConfigurationProperty> configurations;
    
    public AircraftConfiguration() {
        this.configurations = new ArrayList<AircraftConfigurationProperty>();
    }
    
    public AircraftConfiguration(AircraftInfo aircraftInfo) {
    	this.tailNumber = aircraftInfo.getTailNumber();
    	this.carrier = aircraftInfo.getAirline().getName();
    	this.type = aircraftInfo.getAircraftType().getName();
    	this.configurations = new ArrayList<AircraftConfigurationProperty>();
    	
		for (AircraftProperty property : aircraftInfo.getAircraftProperties()) {
			addAircraftPropertyToList(property);
		}
    }

	private void addAircraftPropertyToList(AircraftProperty property) {
		String platform = property.getPropertyValue().getPlatformProperty().getPlatform().getName();
		Optional<AircraftConfigurationProperty> existingProperty = configurations.stream()
				.filter(item -> item.platform.equalsIgnoreCase(platform)).findFirst();
		if (existingProperty.isPresent()) {
			existingProperty.get().properties.put(property.getPropertyValue().getPlatformProperty().getProperty().getName(), 
					property.getPropertyValue().getValue());
		} else {
			existingProperty = Optional.of(new AircraftConfigurationProperty());
			existingProperty.get().platform = platform;
			existingProperty.get().properties.put(property.getPropertyValue().getPlatformProperty().getProperty().getName(), 
					property.getPropertyValue().getValue());
			
			this.configurations.add(existingProperty.get());
		}
	}
}