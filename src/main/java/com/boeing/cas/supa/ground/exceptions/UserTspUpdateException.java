package com.boeing.cas.supa.ground.exceptions;

import java.util.Optional;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

public class UserTspUpdateException extends Exception {
    private static final long serialVersionUID = 1L;

	private ApiError error;

	public UserTspUpdateException() {
		this(null);
	}

	public UserTspUpdateException(ApiError error) {
		super(resolveApiError(Optional.ofNullable(error)).getErrorDescription());
		this.error = resolveApiError(Optional.ofNullable(error));
	}

	private static ApiError resolveApiError(Optional<ApiError> error) {
		return error.orElse(new ApiError("UPDATE_USER_TSP_FAILURE", "User TSP version update failure", RequestFailureReason.INTERNAL_SERVER_ERROR));
	}

	public ApiError getError() {
		return error;
	}

	public void setError(ApiError error) {
		this.error = error;
	}
}




