package com.boeing.cas.supa.ground.services;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boeing.cas.supa.ground.dao.ActiveTspDao;
import com.boeing.cas.supa.ground.dao.AirlineDao;
import com.boeing.cas.supa.ground.dao.AirlineTailDao;
import com.boeing.cas.supa.ground.dao.TspDao;
import com.boeing.cas.supa.ground.pojos.Airline;
import com.boeing.cas.supa.ground.pojos.AirlineTail;
import com.boeing.cas.supa.ground.pojos.Tsp;
import com.boeing.cas.supa.ground.pojos.TspContent;
import com.google.common.base.Strings;
import com.google.gson.Gson;

@Service
public class TspManagementService {
	private static final Logger LOG = LoggerFactory.getLogger(TspManagementService.class);
	
	@Autowired
	private AirlineDao airlineDao;
	
	@Autowired
	private AirlineTailDao airlineTailDao;
	
	@Autowired
	private TspDao tspDao;
	
	@Autowired
	private ActiveTspDao activeTspDao;
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public boolean saveTsp(String airlineName, String tspContent, String stage, String effectiveDate, String userId, boolean isActive) {
		boolean success = false;
		Gson gson = new Gson();
		
		try {
			TspContent tspContentObject = gson.fromJson(tspContent, TspContent.class);
			Tsp tspObject = this.tspDao.getTspByAirlineAndTailNumberAndVersionAndStage(airlineName, tspContentObject.tail,
					String.valueOf(tspContentObject.version), Tsp.Stage.valueOf(stage.toUpperCase()));
			
			if (tspObject == null) {
				tspObject = new Tsp();
				Airline airline = this.airlineDao.getAirlineByName(airlineName);
				if (airline == null) {
					airline = new Airline();
					airline.setName(airlineName);
					airline.setCreatedBy(userId);
					
					this.airlineDao.save(airline);
				}
				
				AirlineTail tail = this.airlineTailDao.getTailByAirlineAndTailNumber(airlineName, tspContentObject.tail);
				if (tail == null) {
					tail = new AirlineTail();
					tail.setAirline(airline);
					tail.setTailNumber(tspContentObject.tail);
					tail.setIsActive(true);
					tail.setCreatedBy(userId);
					
					this.airlineTailDao.save(tail);
				} else {
					if (tail.getIsActive() == false) {
						tail.setIsActive(true);

						this.airlineTailDao.save(tail);
					}
				}
				
				tspObject = new Tsp();
				tspObject.setAirlineTail(tail);
				tspObject.setCreatedBy(userId);
			} else {
				tspObject.setUpdatedBy(userId);
			}
		
			tspObject.setEffectiveDate(getDateTime(effectiveDate));
			tspObject.setStage(Tsp.Stage.valueOf(stage));
			tspObject.setTspContent(tspContent);
			tspObject.setVersion(String.valueOf(tspContentObject.version));
			
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
	
	public List<Tsp> getAllTsps() {
		return this.tspDao.getAllTsps();
	}
	
	public List<Tsp> getTspListByAirline(String airline) {
		return this.tspDao.getTspListByAirline(airline);
	}
	
	public List<Tsp> getTspListByAirlineAndTailNumber(String airline, String tailNumber) {
		return this.tspDao.getTspListByAirlineAndTailNumber(airline, tailNumber);
	}
	
	public List<Tsp> getTspListByAirlineAndTailNumberAndStage(String airline, String tailNumber, String stage) {
		return this.tspDao.getTspListByAirlineAndTailNumberAndStage(airline, tailNumber, Tsp.Stage.valueOf(stage.toUpperCase()));
	}
	
	public Tsp getActiveTspByAirlineAndTailNumberAndStage(String airline, String tailNumber, String stage) {
		return this.tspDao.getActiveTspByAirlineAndTailNumberAndStage(airline, tailNumber, Tsp.Stage.valueOf(stage.toUpperCase()));
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public boolean activateTsp(String airlineName, String tailNumber, String version, String stage, String userId) {
		boolean success = false;
		
		try {
			Tsp tspObject = this.tspDao.getTspByAirlineAndTailNumberAndVersionAndStage(airlineName, tailNumber,
					String.valueOf(version), Tsp.Stage.valueOf(stage.toUpperCase()));	
			
			if (tspObject == null) {
				LOG.info("No TSP to activate");
				return false;
			}
			
			success = this.activateTsp(tspObject.getId(), userId);
		} catch (Exception ex) {
			LOG.error("Failed to activate a TSP. Error: " + ex);
			success = false;
		}
		
		return success;
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public boolean activateTsp(int id, String userId) {
		boolean success = false;
		
		try {
			Tsp tspObject = this.tspDao.getTspById(id);			
			
			if (tspObject == null) {
				LOG.info("No TSP to activate");
				return false;
			}
			
			Tsp activeTsp = tspObject.getAirlineTail().getActiveTsp();
			if (activeTsp != null && activeTsp.getId() == tspObject.getId()) {
				// already activated
				return true;
			}
			
			if (activeTsp != null) {
				this.activeTspDao.deleteActiveTsp(activeTsp.getActiveTsp());
			}
			
			tspObject.setUpdatedBy(userId);
			success = this.activeTspDao.activateTsp(tspObject);
		} catch (Exception ex) {
			LOG.error("Failed to activate a TSP. Error: " + ex);
			success = false;
		}
		
		return success;
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
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
	
	private Timestamp getDateTime(String input) {
		if (Strings.isNullOrEmpty(input)) {
			return null;
		}
	
		ZonedDateTime dateTime = ZonedDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return Timestamp.from(dateTime.toInstant());
	}
}
