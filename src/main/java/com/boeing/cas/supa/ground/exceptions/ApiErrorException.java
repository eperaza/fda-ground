package com.boeing.cas.supa.ground.exceptions;

import com.boeing.cas.supa.ground.pojos.ApiError;

public class ApiErrorException extends Exception {

	private static final long serialVersionUID = 1L;

	private ApiError apiError;
	
	public ApiErrorException(String message, ApiError apiError) {

		super(message);
		this.setApiError(apiError);
	}

	public ApiError getApiError() {
		return apiError;
	}

	public void setApiError(ApiError apiError) {
		this.apiError = apiError;
	}
}
