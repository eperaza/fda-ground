package com.boeing.cas.supa.ground.dao;

import com.boeing.cas.supa.ground.exceptions.UserTspUpdateException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.Tsp;
import com.boeing.cas.supa.ground.pojos.User;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

@Repository
@Transactional
public class TspDaoImpl extends BaseDaoImpl implements TspDao {

	private final Logger logger = LoggerFactory.getLogger(TspDaoImpl.class);

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	
	private static final String USER_TSP_UPDATE_SQL 
	= "BEGIN TRANSACTION; "
	+ "UPDATE user_tsp_version WITH (UPDLOCK, SERIALIZABLE) SET version=:version, last_updated=:last_updated WHERE user_object_id=:user_object_id; "
	+ "IF @@ROWCOUNT = 0 "
	+ "BEGIN "
	+ "INSERT INTO user_tsp_version (user_object_id, version, last_updated) VALUES (:user_object_id, :version, :last_updated) "
	+ "END "
	+ "COMMIT TRANSACTION;";

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

	public void updateUserTSPVersion(User user, String version) throws UserTspUpdateException{

		int returnVal = 0;
		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("user_object_id", user.getObjectId());
		namedParameters.put("version", version);

		Calendar cNow = Calendar.getInstance();
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
		String date_time = date.format(cNow.getTime()) + "T" + time.format(cNow.getTime()) + "Z";

		namedParameters.put("last_updated", date_time);

		try {

			returnVal = jdbcTemplate.update(USER_TSP_UPDATE_SQL, namedParameters);
			if (returnVal != 1) {
				logger.warn("Could not update user account in database: {} record(s) updated", returnVal);
				throw new UserTspUpdateException(new ApiError("UPDATE_USER_TSP_FAILURE", String.format("%d record(s) updated", returnVal), RequestFailureReason.INTERNAL_SERVER_ERROR));
			}
			logger.warn("Updated TSP version of {} in database: {} record(s) updated", user.getObjectId(), returnVal);

		}
		catch (DataAccessException dae) {

			logger.warn("Failed to update user tsp version record in database: {}", dae.getMessage(), dae);
			throw new UserTspUpdateException(new ApiError("UPDATE_USER_TSP_FAILURE", "Database exception", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}
}
