package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.pojos.AirlineTail;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import javax.transaction.Transactional;

@Repository
@Transactional
public class AirlineTailDaoImpl extends BaseDaoImpl implements AirlineTailDao {
	
	@Override
	public boolean save(AirlineTail airlineTail) {
		getSession().saveOrUpdate(airlineTail);
		return airlineTail.getId() > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AirlineTail> getAirlineTails() {
		return getSession().createQuery("from AirlineTail").list();
	}

	@Override
	public AirlineTail getAirlineTailById(int id) {
		return getSession().get(AirlineTail.class, id);
	}

	@Override
	public AirlineTail getTailByAirlineAndTailNumber(String airlineName, String tailNumber) {
		Query query = getSession().getNamedQuery("getTailByAirlineAndTailNumber");
		query.setParameter("name", airlineName);
		query.setParameter("tailNumber", tailNumber);
		
		return (AirlineTail)query.uniqueResult();
	}
}
