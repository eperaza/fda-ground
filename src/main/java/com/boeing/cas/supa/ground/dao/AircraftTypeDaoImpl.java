package com.boeing.cas.supa.ground.dao;

import java.util.List;
import javax.transaction.Transactional;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.boeing.cas.supa.ground.pojos.AircraftType;

@Repository
@Transactional
public class AircraftTypeDaoImpl extends BaseDaoImpl implements AircraftTypeDao {
	@SuppressWarnings("unchecked")
	@Override
	public List<AircraftType> getAircraftTypes() {
		return getSession().createQuery("from AircraftType").list();
	}

	@Override
	public AircraftType getAircraftTypeById(int id) {
		return getSession().get(AircraftType.class, id);
	}

	@Override
	public AircraftType getAircraftTypeByName(String name) {
		Query query = getSession().getNamedQuery("getAircraftTypeByName");
		query.setParameter("name", name);
		
		return (AircraftType)query.uniqueResult();
	}
}