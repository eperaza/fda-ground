package com.boeing.cas.supa.ground.services;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
	@Autowired
	private AirlineDao airlineDao;
	
	@Autowired
	private AirlineTailDao airlineTailDao;
	
	@Autowired
	private TspDao tspDao;
	
	@Transactional
	public boolean saveTsp(String airlineName, String tspContent, String stage, String effectiveDate, String userId) {
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
				} else if (tail.getIsActive() == false) {
					tail.setIsActive(true);
					tail.setUpdatedBy(userId);
					
					this.airlineTailDao.save(tail);
				}
				
				tspObject = new Tsp();
				tspObject.setAirlineTail(tail);
				tspObject.setCreatedBy(userId);
			} else {
				tspObject.setUpdatedBy(userId);
			}
			
			tspObject.setTspModifiedDate(getDateTime(tspContentObject.date));
			tspObject.setEffectiveDate(getDateTime(effectiveDate));
			tspObject.setStage(Tsp.Stage.valueOf(stage));
			tspObject.setTspContent(tspContent);
			tspObject.setVersion(String.valueOf(tspContentObject.version));
			
			success = this.tspDao.save(tspObject);
			
		} catch (Exception ex) {
			success = false;
		}
		
		return success;
	}
	
	public List<Tsp> getTsps(String airline, String tailNumber) {
		return this.tspDao.getTspListByAirlineAndTailNumber(airline, tailNumber);
	}
	
	public Tsp getActiveTspByAirlineAndTailNumberAndStage(String airline, String tailNumber, String stage) {
		return this.tspDao.getActiveTspByAirlineAndTailNumberAndStage(airline, tailNumber, Tsp.Stage.valueOf(stage.toUpperCase()));
	}
	
	private Timestamp getDateTime(String input) {
		if (Strings.isNullOrEmpty(input)) {
			return null;
		}
	
		ZonedDateTime dateTime = ZonedDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return Timestamp.from(dateTime.toInstant());
	}
}
