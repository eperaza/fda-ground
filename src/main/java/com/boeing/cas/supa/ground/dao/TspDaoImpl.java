package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.pojos.Tsp;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import javax.transaction.Transactional;

@Repository
@Transactional
public class TspDaoImpl extends BaseDaoImpl implements TspDao {

	@Override
	public boolean save(Tsp tsp) {
		getSession().saveOrUpdate(tsp);
		return tsp.getId() > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tsp> getTsps() {
		return getSession().createQuery("from Tsp").list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tsp> getTsps(String airlineName) {
		Query query = getSession().getNamedQuery("getTspListByAirline");
		query.setParameter("airlineName", airlineName);
		
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tsp> getTsps(String airlineName, String tailNumber) {
		Query query = getSession().getNamedQuery("getTspListByAirlineAndTailNumber");
		query.setParameter("airlineName", airlineName);
		query.setParameter("tailNumber", tailNumber);
		
		return query.list();
	}

	@Override
	public Tsp getTspById(int id) {
		return getSession().get(Tsp.class, id);
	}

	@Override
	public Tsp getActiveTspByAirlineAndTailNumber(String airlineName, String tailNumber) {
		Query query = getSession().getNamedQuery("getActiveTspByAirlineAndTailNumber");
		query.setParameter("airlineName", airlineName);
		query.setParameter("tailNumber", tailNumber);
		
		query.setMaxResults(1); // get the first record
		
		return (Tsp)query.uniqueResult();
	}
	
	public Tsp getTspByAirlineAndTailNumberAndVersion(String airlineName, String tailNumber, String version) {
		Query query = getSession().getNamedQuery("getTspByAirlineAndTailNumberAndVersion");
		query.setParameter("airlineName", airlineName);
		query.setParameter("tailNumber", tailNumber);
		query.setParameter("version", version);
		
		return (Tsp)query.uniqueResult();
	}
}
