package com.boeing.cas.supa.ground.exceptions;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

import java.util.Optional;

public class OnsCertificateException extends Exception {

	private static final long serialVersionUID = 1L;

	private ApiError error;

	public OnsCertificateException() {
		this(null);
	}

	public OnsCertificateException(ApiError error) {
		super(resolveApiError(Optional.ofNullable(error)).getErrorDescription());
		this.error = resolveApiError(Optional.ofNullable(error));
	}

	private static ApiError resolveApiError(Optional<ApiError> error) {
		return error.orElse(new ApiError("ONS_CERTIFICATE_FAILURE", "Ons Certificate failure", RequestFailureReason.INTERNAL_SERVER_ERROR));
	}

	public ApiError getError() {
		return error;
	}

	public void setError(ApiError error) {
		this.error = error;
	}
}
