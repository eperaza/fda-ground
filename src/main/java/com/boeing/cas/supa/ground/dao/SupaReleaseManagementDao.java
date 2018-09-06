package com.boeing.cas.supa.ground.dao;

import java.util.List;

import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.pojos.SupaRelease;

public interface SupaReleaseManagementDao {

	public List<SupaRelease> getSupaReleases() throws SupaReleaseException;
	
	public SupaRelease getSupaReleaseByRelease(String release) throws SupaReleaseException;

	public SupaRelease getSupaReleaseByPartNumber(String partNumber) throws SupaReleaseException;
}
