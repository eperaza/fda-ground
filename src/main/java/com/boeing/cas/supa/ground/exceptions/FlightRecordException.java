package com.boeing.cas.supa.ground.exceptions;

import java.util.Optional;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

public class FlightRecordException extends Exception {

	private static final long serialVersionUID = 1L;

	private ApiError error;

	public FlightRecordException() {
		this(null);
	}

	public FlightRecordException(ApiError error) {
		super(resolveApiError(Optional.ofNullable(error)).getErrorDescription());
		this.error = resolveApiError(Optional.ofNullable(error));
	}

	private static ApiError resolveApiError(Optional<ApiError> error) {
		return error.orElse(new ApiError("FLIGHT_RECORD_FAILURE", "Flight record processing failure", RequestFailureReason.INTERNAL_SERVER_ERROR));
	}

	public ApiError getError() {
		return error;
	}

	public void setError(ApiError error) {
		this.error = error;
	}
}
