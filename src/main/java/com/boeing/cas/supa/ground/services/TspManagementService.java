package com.boeing.cas.supa.ground.services;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.boeing.cas.supa.ground.dao.ActiveTspDao;
import com.boeing.cas.supa.ground.dao.AirlineDao;
import com.boeing.cas.supa.ground.dao.AircraftInfoDao;
import com.boeing.cas.supa.ground.dao.AircraftTypeDao;
import com.boeing.cas.supa.ground.dao.TspDao;
import com.boeing.cas.supa.ground.pojos.Airline;
import com.boeing.cas.supa.ground.pojos.AircraftInfo;
import com.boeing.cas.supa.ground.pojos.Tsp;
import com.boeing.cas.supa.ground.pojos.TspContent;
import com.google.common.base.Strings;
import com.google.gson.Gson;

@Service
@Transactional
public class TspManagementService {
	private static final Logger LOG = LoggerFactory.getLogger(TspManagementService.class);
	
	private static final String DateFormat = "MM-dd-yyyy";
	
	@Autowired
	private AirlineDao airlineDao;
	
	@Autowired
	private AircraftInfoDao aircraftInfoDao;
	
	@Autowired
	private TspDao tspDao;
	
	@Autowired
	private ActiveTspDao activeTspDao;
	
	@Autowired
	private AircraftTypeDao aircraftTypeDao;
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public boolean saveTsp(String airlineName, String aircraftType, String tspContent, String userId, boolean isActive, String cutoffDate, Integer numberOfFlights) {
		boolean success = false;
		Gson gson = new Gson();
		
		try {
			TspContent tspContentObject = gson.fromJson(tspContent, TspContent.class);
			Tsp tspObject = this.tspDao.getTspByAirlineAndTailNumberAndVersion(airlineName, tspContentObject.tail,
					String.valueOf(tspContentObject.version));
			
			if (tspObject == null) {
				tspObject = new Tsp();
				Airline airline = this.airlineDao.getAirlineByName(airlineName);
				if (airline == null) {
					airline = new Airline();
					airline.setName(airlineName);
					airline.setCreatedBy(userId);
					
					this.airlineDao.save(airline);
				}
				
				AircraftInfo tail = this.aircraftInfoDao.getTailByAirlineAndTailNumber(airlineName, tspContentObject.tail);
				if (tail == null) {
					tail = new AircraftInfo();
					tail.setAirline(airline);
					tail.setTailNumber(tspContentObject.tail);
					tail.setIsActive(true);
					tail.setCreatedBy(userId);
					tail.setAircraftType(this.aircraftTypeDao.getAircraftTypeByName(aircraftType));
					
					this.aircraftInfoDao.save(tail);
				} else {
					if (tail.getIsActive() == false) {
						tail.setIsActive(true);

						this.aircraftInfoDao.save(tail);
					}
				}
				
				tspObject = new Tsp();
				tspObject.setAircraftInfo(tail);
				tspObject.setCreatedBy(userId);
			} else {
				tspObject.setUpdatedBy(userId);
			}
		
			tspObject.setTspContent(tspContent);
			tspObject.setVersion(String.valueOf(tspContentObject.version));
			tspObject.setNumberOfFlights(numberOfFlights);
			tspObject.setCutoffDate(getCutoffDate(cutoffDate));
			
			success = this.tspDao.save(tspObject);
			
			if (success) {
				if (isActive) {
					success = activateTsp(tspObject.getId(), userId);
				} else {
					success = deactivateTsp(tspObject.getId());
				}
			}
			
		} catch (Exception ex) {
			LOG.error("Failed to save TSP to database. Error: " + ex);
			success = false;
		}
		
		return success;
	}
	
	public List<Tsp> getTsps() {
		return this.tspDao.getTsps();
	}
	
	public List<Tsp> getTsps(String airline) {
		return this.tspDao.getTsps(airline);
	}
	
	public List<Tsp> getTsps(String airline, String tailNumber) {
		return this.tspDao.getTsps(airline, tailNumber);
	}
	
	public Tsp getActiveTspByAirlineAndTailNumber(String airline, String tailNumber) {
		return this.tspDao.getActiveTspByAirlineAndTailNumber(airline, tailNumber);
	}
	
	public boolean activateTsp(String airlineName, String tailNumber, String version, String userId) {
		boolean success = false;
		
		try {
			Tsp tspObject = this.tspDao.getTspByAirlineAndTailNumberAndVersion(airlineName, tailNumber,
					String.valueOf(version));	
			
			success = this.activateTsp(tspObject, userId);
		} catch (Exception ex) {
			LOG.error("Failed to activate a TSP. Error: " + ex);
			success = false;
		}
		
		return success;
	}
	
	public boolean activateTsp(int id, String userId) {
		boolean success = false;
		
		try {
			Tsp tspObject = this.tspDao.getTspById(id);			

			success = activateTsp(tspObject, userId);
		} catch (Exception ex) {
			LOG.error("Failed to activate a TSP. Error: " + ex);
			success = false;
		}
		
		return success;
	}
	
	public boolean deactivateTsp(int id) {
		boolean success = false;
		
		try {
			Tsp tspObject = this.tspDao.getTspById(id);			
			
			if (tspObject == null) {
				LOG.info("No TSP to deactivate");
				return false;
			}
			
			if (tspObject.getActiveTsp() == null) {
				return true;
			}
			
			this.activeTspDao.deleteActiveTsp(tspObject.getActiveTsp());
			success = true;
			
		} catch (Exception ex) {
			LOG.error("Failed to deactivate a TSP. Error: " + ex);
			success = false;
		}
		
		return success;
	}
	
	private boolean activateTsp(Tsp tspObject, String userId) {
		if (tspObject == null) {
			LOG.info("No TSP to activate");
			return false;
		}
		
		Tsp activeTsp = tspObject.getAircraftInfo().getCurrentActiveTsp();
		if (activeTsp != null && activeTsp.getId() == tspObject.getId()) {
			// already activated
			return true;
		}
		
		if (activeTsp != null) {
			this.activeTspDao.deleteActiveTsp(activeTsp.getActiveTsp());
		}
		
		return this.activeTspDao.activateTsp(tspObject);
	}
	
	private Date getCutoffDate(String cutoffDate) {
		if (Strings.isNullOrEmpty(cutoffDate)) {
			return null;
		}
		
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateFormat);
			return simpleDateFormat.parse(cutoffDate);
		} catch (Exception ex) {
			LOG.error("Invalid cutoff date format: " + cutoffDate);
			return null;
		}
	}
}
