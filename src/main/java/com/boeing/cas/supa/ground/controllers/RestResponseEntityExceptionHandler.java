package com.boeing.cas.supa.ground.controllers;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.boeing.cas.supa.ground.exceptions.UserAccountRegistrationException;
import com.boeing.cas.supa.ground.exceptions.UserAuthenticationException;
import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants.RequestFailureReason;
import com.boeing.cas.supa.ground.utils.ControllerUtils;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
		return new ResponseEntity<>(apiError, ControllerUtils.translateRequestFailureReasonToHttpErrorCode(apiError.getFailureReason()));
	}

	@Override
	protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("async request timeout exception handler", ex.getLocalizedMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
	}

	@Override
	protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
			WebRequest request) {
		return buildResponseEntity(new ApiError("global bind exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global conversion exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global internal exception handler", ex.getLocalizedMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global unaccepted media type exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global unsupported media type exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global unreadable message exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global unwritable message exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global unsupported method exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global invalid method argument exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers,
			HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global missing path variable exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global missing servlet request parameter exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global missing servlet request part exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
			HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global no handler found exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global servlet request binding exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatus status, WebRequest request) {
		return buildResponseEntity(new ApiError("global mismatched type exception handler", ex.getLocalizedMessage(), RequestFailureReason.BAD_REQUEST));
	}

	@ExceptionHandler(UserAuthenticationException.class)
	protected ResponseEntity<Object> handleEntityNotFound(UserAuthenticationException uae) {
		return buildResponseEntity(uae.getError());
	}

	@ExceptionHandler(UserAccountRegistrationException.class)
	protected ResponseEntity<Object> handleEntityNotFound(UserAccountRegistrationException uare) {
		return buildResponseEntity(uare.getError());
	}
	
	@ExceptionHandler(RuntimeException.class)
	protected ResponseEntity<Object> handleRuntimeException(RuntimeException re, HttpHeaders headers,
			HttpStatus statys, WebRequest request) {
		return buildResponseEntity(new ApiError("global runtime exception handler", re.getLocalizedMessage(), RequestFailureReason.INTERNAL_SERVER_ERROR));
	}
}
