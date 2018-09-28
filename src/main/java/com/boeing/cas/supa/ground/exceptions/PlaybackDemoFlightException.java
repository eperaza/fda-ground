package com.boeing.cas.supa.ground.exceptions;

import java.util.Optional;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

public class PlaybackDemoFlightException extends Exception {

	private static final long serialVersionUID = 1L;

	private ApiError error;

	public PlaybackDemoFlightException() {
		this(null);
	}

	public PlaybackDemoFlightException(ApiError error) {
		super(resolveApiError(Optional.ofNullable(error)).getErrorDescription());
		this.error = resolveApiError(Optional.ofNullable(error));
	}

	private static ApiError resolveApiError(Optional<ApiError> error) {
		return error.orElse(new ApiError("DEMO_FLIGHT_MGMT", "SUPA Playback Demo Flight Mgmt processing failure", RequestFailureReason.INTERNAL_SERVER_ERROR));
	}

	public ApiError getError() {
		return error;
	}

	public void setError(ApiError error) {
		this.error = error;
	}
}
