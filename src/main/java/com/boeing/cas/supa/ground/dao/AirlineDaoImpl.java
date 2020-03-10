package com.boeing.cas.supa.ground.dao;

import java.util.List;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;
import com.boeing.cas.supa.ground.pojos.Airline;

@Repository
@Transactional(value = TxType.REQUIRES_NEW)
public class AirlineDaoImpl extends BaseDaoImpl implements AirlineDao {

	@Override
	public boolean save(Airline airline) {
		getSession().saveOrUpdate(airline);
		return airline.getId() > 0;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Airline> getAirlines() {
		return getSession().createQuery("from Airline").list();
	}

	@Override
	public Airline getAirlineById(int id) {
		return getSession().get(Airline.class, id);
	}

	@Override
	public Airline getAirlineByName(String name) {
		Query query = getSession().getNamedQuery("getAirlineByName");
		query.setParameter("name", name);
		
		return (Airline)query.uniqueResult();
	}
}