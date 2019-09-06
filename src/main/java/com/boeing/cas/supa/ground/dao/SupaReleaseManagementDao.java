package com.boeing.cas.supa.ground.dao;

import java.util.List;

import com.boeing.cas.supa.ground.exceptions.SupaReleaseException;
import com.boeing.cas.supa.ground.pojos.CurrentSupaRelease;
import com.boeing.cas.supa.ground.pojos.SupaRelease;

public interface SupaReleaseManagementDao {

	public void insertCurrentSupaRelease(CurrentSupaRelease currentSupaRelease) throws SupaReleaseException;

	public void removeCurrentSupaRelease(String airline) throws SupaReleaseException;

	public List<CurrentSupaRelease> getCurrentSupaRelease(String airline) throws SupaReleaseException;

	public List<SupaRelease> getSupaReleases(String airline) throws SupaReleaseException;

	public List<SupaRelease> getWarReleases(String airline) throws SupaReleaseException;

	public SupaRelease getSupaReleaseByRelease(String release, String airline) throws SupaReleaseException;

	public SupaRelease getWarReleaseByRelease(String release, String airline) throws SupaReleaseException;

	public SupaRelease getSupaReleaseByPartNumber(String partNumber, String airline) throws SupaReleaseException;
}
