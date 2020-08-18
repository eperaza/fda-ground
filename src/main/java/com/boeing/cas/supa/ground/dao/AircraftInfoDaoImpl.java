package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.pojos.AircraftConfiguration;
import com.boeing.cas.supa.ground.pojos.AircraftInfo;
import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

@Repository
@Transactional
public class AircraftInfoDaoImpl extends BaseDaoImpl implements AircraftInfoDao {
	
	@Override
	public boolean save(AircraftInfo airlineTail) {
		getSession().saveOrUpdate(airlineTail);
		return airlineTail.getId() > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AircraftInfo> getAirlineTails() {
		return getSession().createQuery("from AirlineTail").list();
	}

	@Override
	public AircraftInfo getAirlineTailById(int id) {
		return getSession().get(AircraftInfo.class, id);
	}

	@Override
	public Date getAircraftPropertyLastModifiedTimeStamp(String airlineName, String tailNumber) {
		Query query = getSession().getNamedQuery("getAircraftPropertyLastModifiedTimeStamp");
		query.setParameter("name", airlineName);
		query.setParameter("tailNumber", tailNumber);
		return (Date)query.uniqueResult();
	}

	@Override
	public AircraftInfo getTailByAirlineAndTailNumber(String airlineName, String tailNumber) {
		Query query = getSession().getNamedQuery("getTailByAirlineAndTailNumber");
		query.setParameter("name", airlineName);
		query.setParameter("tailNumber", tailNumber);
		
		return (AircraftInfo)query.uniqueResult();
	}

	@Override
	public AircraftConfiguration getAircraftPropertiesByAirlineAndTailNumber(String airlineName, String tailNumber) {
		Query query = getSession().getNamedQuery("getAircraftPropertiesByAirlineAndTailNumber");
		query.setParameter("name", airlineName);
		query.setParameter("tailNumber", tailNumber);
		
		return (AircraftConfiguration)query.uniqueResult();
	}
}
