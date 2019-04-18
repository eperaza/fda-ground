package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.FeatureManagementException;
import com.boeing.cas.supa.ground.pojos.AirlinePreferences;
import com.boeing.cas.supa.ground.pojos.FeatureManagement;
import com.boeing.cas.supa.ground.pojos.KeyValueUpdate;
import com.boeing.cas.supa.ground.pojos.UserPreferences;

import java.util.List;

public interface FeatureManagementDao {


	public void updateFeatureManagement(String airline, String updatedBy, KeyValueUpdate keyValueUpdate) throws FeatureManagementException;

	public void updateAirlinePreferences(String airline, String updatedBy, KeyValueUpdate keyValueUpdate) throws FeatureManagementException;

	public void updateUserPreferences(String airline, String updatedBy, KeyValueUpdate keyValueUpdate) throws FeatureManagementException;

	public List<FeatureManagement> getFeatureManagement(String airline) throws FeatureManagementException;

	public List<AirlinePreferences> getAirlinePreferences(String airline, boolean displayOnly) throws FeatureManagementException;

	public List<UserPreferences> getUserPreferences(String airline) throws FeatureManagementException;

}
