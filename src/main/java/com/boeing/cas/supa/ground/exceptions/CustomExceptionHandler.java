package com.boeing.cas.supa.ground.exceptions;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler{

	@ExceptionHandler(AirlineStatusUnauthorizedException.class)
    public ResponseEntity<Object> airlineStatusUnauthorizedExceptionHandle(AirlineStatusUnauthorizedException ex, WebRequest request) {
        Object resultObj = new ApiError("Not Authorized", "Your role doesn't have enough permissions.", RequestFailureReason.UNAUTHORIZED);
        return new ResponseEntity<>(resultObj, ex.getStatus());
    }
    
}
