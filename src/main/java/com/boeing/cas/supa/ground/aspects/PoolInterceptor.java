package com.boeing.cas.supa.ground.aspects;

import org.apache.tomcat.jdbc.pool.DataSource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
 * This class logs the status of the Tomcat Connection Pool
 * before and after each transaction. Shows active, idle and 
 * total connections available.
 */
@Aspect
@Component
public class PoolInterceptor {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DataSource ds;

    @Before("execution(* com.boeing.cas.supa.ground.dao.*.*(..))")
    public void logBeforeConnection(JoinPoint jp) throws Throwable {
        logDataSourceInfos("Before", jp);
    }

    @After("execution(* com.boeing.cas.supa.ground.dao.*.*(..)) ")
    public void logAfterConnection(JoinPoint jp) throws Throwable {
        logDataSourceInfos("After", jp);
    }

    public void logDataSourceInfos(final String time, final JoinPoint jp) {
        final String method = String.format("%s:%s", jp.getTarget().getClass().getName(), jp.getSignature().getName());
        logger.info("Status for {}", ds.getPoolName());
        logger.info(String.format("%s %s: number of connections in use by the application (active): %d.", time, method, ds.getNumActive()));
        logger.info(String.format("%s %s: the number of established but idle connections: %d.", time, method, ds.getNumIdle()));
        logger.info(String.format("%s %s: number of threads waiting for a connection: %d.", time, method, ds.getWaitCount()));
        logger.info(String.format("%s %s: number of max connections: %d.", time, method, ds.getMaxActive()));

    }
}