package com.boeing.cas.supa.ground.exceptions;

import com.boeing.cas.supa.ground.pojos.ApiError;
import com.boeing.cas.supa.ground.utils.Constants;

import java.util.Optional;

public class TspConfigLogException  extends Exception{

    private static final long serialVersionUID = 1L;

    private ApiError error;

    public TspConfigLogException() {
        this(null);
    }

    public TspConfigLogException(ApiError error) {
        super(resolveApiError(Optional.ofNullable(error)).getErrorDescription());
        this.error = resolveApiError(Optional.ofNullable(error));
    }

    private static ApiError resolveApiError(Optional<ApiError> error) {
        return error.orElse(new ApiError("TSP_CONFIG_SYSTEM_LOG_FAILURE", "TSP Config Upload failure", Constants.RequestFailureReason.INTERNAL_SERVER_ERROR));
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }
}
