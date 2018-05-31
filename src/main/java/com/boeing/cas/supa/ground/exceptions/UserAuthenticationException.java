package com.boeing.cas.supa.ground.exceptions;

import java.util.Optional;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

public class UserAuthenticationException extends Exception {

	private static final long serialVersionUID = 1L;

	private ApiError error;

	public UserAuthenticationException() {
		this(null);
	}

	public UserAuthenticationException(ApiError error) {
		super(resolveApiError(Optional.ofNullable(error)).getErrorDescription());
		this.error = resolveApiError(Optional.ofNullable(error));
	}

	public ApiError getError() {
		return error;
	}

	public void setError(ApiError error) {
		this.error = error;
	}
	
	private static ApiError resolveApiError(Optional<ApiError> error) {
		return error.orElse(new ApiError("USER_AUTH_FAILURE", "User authentication failure", RequestFailureReason.UNAUTHORIZED));
	}
}
