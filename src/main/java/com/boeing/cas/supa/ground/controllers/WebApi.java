package com.boeing.cas.supa.ground.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class WebApi {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<Object> doSomething() {
        ResponseEntity<Object> response = null;
        try {
            response = new ResponseEntity<Object>("SUCCESS", HttpStatus.OK);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseEntity<Object>(ex.getMessage(), 
            		HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
    
    
}