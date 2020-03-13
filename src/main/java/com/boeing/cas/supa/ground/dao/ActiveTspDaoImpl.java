package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.pojos.ActiveTsp;
import com.boeing.cas.supa.ground.pojos.Tsp;

import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Repository
@Transactional(value = TxType.REQUIRES_NEW)
public class ActiveTspDaoImpl extends BaseDaoImpl implements ActiveTspDao {

	@Override
	public boolean activateTsp(Tsp tsp) {
		if (tsp.getActiveTsp() != null) {
			// already active
			return true;
		}
		
		ActiveTsp activeTsp = new ActiveTsp();
		activeTsp.setTsp(tsp);
		activeTsp.setAircraftInfo(tsp.getAircraftInfo());
		activeTsp.setCreatedBy(tsp.getCreatedBy());
		
		getSession().saveOrUpdate(activeTsp);
		
		return activeTsp.getId() > 0;
	}

	@Override
	public void deleteActiveTsp(ActiveTsp activeTsp) {
		getSession().delete(activeTsp);
	}
}
