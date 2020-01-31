package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.pojos.Tsp;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
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
	public List<Tsp> getAllTsps() {
		return getSession().createQuery("from Tsp").list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tsp> getTspListByAirline(String airlineName) {
		Query query = getSession().getNamedQuery("getTspListByAirline");
		query.setParameter("airlineName", airlineName);
		
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tsp> getTspListByAirlineAndTailNumber(String airlineName, String tailNumber) {
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
	public Tsp getActiveTspByAirlineAndTailNumberAndStage(String airlineName, String tailNumber, Tsp.Stage stage) {
		Query query = getSession().getNamedQuery("getActiveTspByAirlineAndTailNumberAndStage");
		query.setParameter("airlineName", airlineName);
		query.setParameter("tailNumber", tailNumber);
		query.setParameter("stage", stage);
		
		query.setMaxResults(1); // get the first record
		
		return (Tsp)query.uniqueResult();
	}

	@Override
	public Tsp getTspByAirlineAndTailNumberAndModifiedDateAndEffectiveDateAndStage(String airlineName,
			String tailNumber, Date modifiedDate, Date effectiveDate, Tsp.Stage stage) {
		Query query = getSession().getNamedQuery("getTspByAirlineAndTailNumberAndModifiedDateAndEffectiveDateAndStage");
		query.setParameter("airlineName", airlineName);
		query.setParameter("tailNumber", tailNumber);
		query.setParameter("modifiedDate", modifiedDate);
		query.setParameter("effectiveDate", effectiveDate);
		query.setParameter("stage", stage);
		
		return (Tsp)query.uniqueResult();
	}
	
	public Tsp getTspByAirlineAndTailNumberAndVersionAndStage(String airlineName, String tailNumber, String version, Tsp.Stage stage) {
		Query query = getSession().getNamedQuery("getTspByAirlineAndTailNumberAndVersionAndStage");
		query.setParameter("airlineName", airlineName);
		query.setParameter("tailNumber", tailNumber);
		query.setParameter("version", version);
		query.setParameter("stage", stage);
		
		return (Tsp)query.uniqueResult();
	}
}
