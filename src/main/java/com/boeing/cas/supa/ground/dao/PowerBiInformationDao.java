package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.PowerBiInformationException;
import com.boeing.cas.supa.ground.pojos.PowerBiInformation;

public interface PowerBiInformationDao {

	public PowerBiInformation getPowerBiInformation(String airline) throws PowerBiInformationException;
}
