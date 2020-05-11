package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.pojos.AircraftConfiguration;
import com.boeing.cas.supa.ground.pojos.AircraftInfo;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Repository
@Transactional(value = TxType.REQUIRES_NEW)
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
	public AircraftInfo getTailByAirlineAndTailNumber(String airlineName, String tailNumber) {
		Query query = getSession().getNamedQuery("getTailByAirlineAndTailNumber");
		query.setParameter("name", airlineName);
		query.setParameter("tailNumber", tailNumber);
		
		return (AircraftInfo)query.uniqueResult();
	}

	@Override
	public AircraftConfiguration getAircarftPropertiesByAirlineAndTailNumber(String airlineName, String tailNumber) {
		Query query = getSession().getNamedQuery("getAircarftPropertiesByAirlineAndTailNumber");
		query.setParameter("name", airlineName);
		query.setParameter("tailNumber", tailNumber);
		
		return (AircraftConfiguration)query.uniqueResult();
	}
}
