package com.boeing.cas.supa.ground.exceptions;

import org.springframework.http.HttpStatus;

public class AirlineStatusUnauthorizedException extends Exception{

	private static final long serialVersionUID = 1L;
	
	private final String message;
	private final HttpStatus status;
	
	public AirlineStatusUnauthorizedException(String message, HttpStatus status) {
		super();
		this.message = message;
		this.status = status;
	}

	public AirlineStatusUnauthorizedException(HttpStatus status) {
		super();
		this.message = "You are not authorized to see this content.";
		this.status = status;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
	
	public HttpStatus getStatus() {
		return status;
	}
	
	
}
