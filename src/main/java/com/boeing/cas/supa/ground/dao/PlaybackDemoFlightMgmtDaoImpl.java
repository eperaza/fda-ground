package com.boeing.cas.supa.ground.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.boeing.cas.supa.ground.exceptions.PlaybackDemoFlightException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.pojos.PlaybackDemoFlight;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

@Repository
public class PlaybackDemoFlightMgmtDaoImpl implements PlaybackDemoFlightMgmtDao {

	private final Logger logger = LoggerFactory.getLogger(PlaybackDemoFlightMgmtDaoImpl.class);

	private static final String GET_DEMO_FLIGHT_STREAMS = "SELECT * FROM demo_flight_streams ORDER BY create_ts desc";
	private static final String GET_DEMO_FLIGHT_STREAM_BY_NAME = "SELECT * FROM demo_flight_streams WHERE LOWER(flight_stream_name) = :flight_stream_name ORDER BY create_ts desc";

	@Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public List<PlaybackDemoFlight> listDemoFlightStreams() throws PlaybackDemoFlightException {

		List<PlaybackDemoFlight> playbackDemoFlights = new ArrayList<>();

		try {

			playbackDemoFlights = jdbcTemplate.query(GET_DEMO_FLIGHT_STREAMS, new PlaybackDemoFlightRowMapper());
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve demo flight streams: {}", dae.getMessage(), dae);
			throw new PlaybackDemoFlightException(new ApiError("PLAYBACK_DEMO_FLIGHT_MGMT", "Failed to retrieve demo flight streams", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}

		return playbackDemoFlights;

	}

	@Override
	public PlaybackDemoFlight getDemoFlightStream(String flightStreamName) throws PlaybackDemoFlightException {

		Map<String,Object> namedParameters = new HashMap<>();
		namedParameters.put("flight_stream_name", flightStreamName);

		PlaybackDemoFlight playbackDemoFlight = null;
		try {
			playbackDemoFlight = jdbcTemplate.queryForObject(GET_DEMO_FLIGHT_STREAM_BY_NAME, namedParameters, new PlaybackDemoFlightRowMapper());
			return playbackDemoFlight;
		} catch (EmptyResultDataAccessException erdae) {
			logger.error("Failed to retrieve demo flight strean matching specified demo flight identifier: {}", erdae.getMessage(), erdae);
			throw new PlaybackDemoFlightException(new ApiError("PLAYBACK_DEMO_FLIGHT_MGMT", "Missing or invalid demo flight stream identifier", RequestFailureReason.NOT_FOUND));
		} catch (DataAccessException dae) {
			logger.error("Failed to retrieve demo flight stream matching specified demo flight identifier: {}", dae.getMessage(), dae);
			throw new PlaybackDemoFlightException(new ApiError("PLAYBACK_DEMO_FLIGHT_MGMT", "Failed to retrieve demo flight stream", RequestFailureReason.INTERNAL_SERVER_ERROR));
		}
	}

	private static final class PlaybackDemoFlightRowMapper implements RowMapper<PlaybackDemoFlight> {

		@Override
		public PlaybackDemoFlight mapRow(ResultSet resultSet, int rowNum) throws SQLException {

			PlaybackDemoFlight playbackDemoFlight = new PlaybackDemoFlight(
				resultSet.getString("flight_stream_name"),
				resultSet.getString("path")
			);

			return playbackDemoFlight;
		}
	}
}
