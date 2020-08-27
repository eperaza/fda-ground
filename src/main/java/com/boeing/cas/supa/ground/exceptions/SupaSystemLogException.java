package com.boeing.cas.supa.ground.exceptions;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

import java.util.Optional;

public class SupaSystemLogException extends Exception {

	private static final long serialVersionUID = 1L;

	private ApiError error;

	public SupaSystemLogException() {
		this(null);
	}

	public SupaSystemLogException(ApiError error) {
		super(resolveApiError(Optional.ofNullable(error)).getErrorDescription());
		this.error = resolveApiError(Optional.ofNullable(error));
	}

	private static ApiError resolveApiError(Optional<ApiError> error) {
		return error.orElse(new ApiError("SUPA_SYSTEM_LOG_FAILURE", "Supa System Log processing failure", RequestFailureReason.INTERNAL_SERVER_ERROR));
	}

	public ApiError getError() {
		return error;
	}

	public void setError(ApiError error) {
		this.error = error;
	}
}
