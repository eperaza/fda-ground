package com.boeing.cas.supa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.boeing.cas.supa.ground.controllers.AirlineStatusController;
import com.boeing.cas.supa.ground.exceptions.AirlineStatusUnauthorizedException;
import com.boeing.cas.supa.ground.exceptions.FeatureManagementException;
import com.boeing.cas.supa.ground.exceptions.FileDownloadException;
import com.boeing.cas.supa.ground.exceptions.TspConfigLogException;
import com.boeing.cas.supa.ground.pojos.AirlinePreferences;
import com.boeing.cas.supa.ground.pojos.AirlineStatusChecklistItem;
import com.boeing.cas.supa.ground.pojos.CosmosDbFlightPlanSource;
import com.boeing.cas.supa.ground.services.AirlineStatusService;
import com.boeing.cas.supa.ground.services.FileManagementService;
import com.boeing.cas.supa.ground.services.MongoFlightManagerService;
import com.boeing.cas.supa.ground.utils.AzureStorageUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static com.boeing.cas.supa.ground.dao.FeatureManagementDaoImpl.AirlinePreferencesRowMapper;
import static com.boeing.cas.supa.ground.services.FileManagementService.TSP_CONFIG_ZIP_CONTAINER;
import static com.boeing.cas.supa.utils.Constants.AIRLINE;
import static com.boeing.cas.supa.utils.Constants.FLIGHT_PLAN_CONTAINER;

@SpringBootTest
@RunWith(MockitoJUnitRunner.class)
public class AirlineStatusTests {

    @Mock
    private AirlineStatusService service;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @InjectMocks
    private AirlineStatusController controller;

    @Captor
    private ArgumentCaptor<String> captorOne;

    @Captor
    private ArgumentCaptor<String> captorTwo;

    private static FileManagementService fileManagementService;
    private static final Logger logger = LoggerFactory.getLogger(AirlineStatusTests.class);
    private static AzureStorageUtil asu;
    private static MongoFlightManagerService mfms;
    private static String TOKEN_STRING;

    @BeforeClass
    public static void generateJWT() {
        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            TOKEN_STRING = JWT.create()
                    .withIssuer("auth0")
                    .sign(algorithm);
        } catch (JWTCreationException jwtce) {
            TOKEN_STRING = StringUtils.EMPTY;
            logger.error("Invalid Signing configuration / Couldn't convert Claims.", jwtce.getMessage(), jwtce);
        }
    }

    @Test
    public void contextLoads() {
        assertNotNull(controller);
        assertNotNull(service);
        assertNotNull(jdbcTemplate);
        logger.info("Context Loads Successfully");
    }

    @Test
    public void getBlobLastModifiedTimeStamp() throws TspConfigLogException, IOException {
        // given
        Date lastModified;
        fileManagementService = mock(FileManagementService.class);
        String fileName = new StringBuilder(AIRLINE).append("/").append(AIRLINE).append("-config-pkg.zip").toString();

        // when
        when(fileManagementService.getBlobLastModifiedTimeStamp(TSP_CONFIG_ZIP_CONTAINER, fileName))
                .thenReturn(new Date());
        lastModified = fileManagementService.getBlobLastModifiedTimeStamp(TSP_CONFIG_ZIP_CONTAINER, fileName);

        // then
        assertNotNull(lastModified);
        logger.debug("getBlobLastModifiedTimeStamp Test Passed");
    }

    @Test
    public void flightPlanDownload() throws IOException, ConfigurationException {
        // given
        asu = mock(AzureStorageUtil.class);
        String fileName = AIRLINE + ".source";

        // when
        when(asu.downloadFile(FLIGHT_PLAN_CONTAINER, fileName)).thenReturn(new ByteArrayOutputStream());
        ByteArrayOutputStream outputStream = asu.downloadFile(FLIGHT_PLAN_CONTAINER, fileName);

        // then
        verify(asu).downloadFile(FLIGHT_PLAN_CONTAINER, fileName);
        verify(asu).downloadFile(captorOne.capture(), captorTwo.capture());
        assertEquals(FLIGHT_PLAN_CONTAINER, captorOne.getValue());
        assertEquals(fileName, captorTwo.getValue());
        assertNotNull(outputStream);
        logger.debug("flightPlanDownload Test Passed");
    }

    @Test
	public void getAllFlightObjectsFromCosmosDB() throws IOException, ConfigurationException{
        //given
		mfms = mock(MongoFlightManagerService.class);
        Optional<Integer> limit = Optional.of(Mockito.anyInt());
        Optional<String> flightId = Optional.of(Mockito.anyString());
        Optional<String> departureAirport = Optional.of(Mockito.anyString());
        Optional<String> arrivalAirport = Optional.of(Mockito.anyString());
        Object flightObjects = new Object();

        CosmosDbFlightPlanSource flightPlanSource = Mockito.any(CosmosDbFlightPlanSource.class);

        //when
        when(mfms.getAllFlightObjectsFromCosmosDB(flightId, departureAirport, arrivalAirport, flightPlanSource, limit)).thenReturn(flightObjects);
        Object response = mfms.getAllFlightObjectsFromCosmosDB(flightId, departureAirport, arrivalAirport, flightPlanSource, limit);

        //then
        verify(mfms).getAllFlightObjectsFromCosmosDB(flightId, departureAirport, arrivalAirport, flightPlanSource, limit);  
		assertNotNull(response);
        logger.debug("getAllFlightObjectsFromCosmosDB Test Passed");
    }

    @Test
    public void getAirlinePreferences() {
        // given
        List<AirlinePreferences> list = new ArrayList<>();
        AirlinePreferences preferences = new AirlinePreferences();
        list.add(preferences);

        // when
        when(jdbcTemplate.query(Mockito.anyString(), Mockito.any(AirlinePreferencesRowMapper.class))).thenReturn(list);
        List<AirlinePreferences> preferencesList = jdbcTemplate.query(Mockito.anyString(),
                Mockito.any(AirlinePreferencesRowMapper.class));

        // then
        verify(jdbcTemplate).query(Mockito.anyString(), Mockito.any(AirlinePreferencesRowMapper.class));
        assertNotNull(preferencesList);
        logger.debug("getAirlinePreferences Test Passed");
    }

    @Test
    public void checkAutoConfig() throws NoSuchAlgorithmException, IOException, TspConfigLogException,
            FileDownloadException, InterruptedException, CancellationException, CompletionException, ExecutionException,
            AirlineStatusUnauthorizedException {
        // given
        Map<List<AirlineStatusChecklistItem>, HttpStatus> responseMap = new LinkedHashMap<>();
        List<AirlineStatusChecklistItem> list = new ArrayList<>();
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        Date lastModified = new Date();

        List<Object> content = new ArrayList<>();
        Map<String, String> description = new HashMap<>();
        description.put("lastModified", lastModified.toString());

        final ObjectMapper mapper = new ObjectMapper();
        final Object obj = mapper.convertValue(description, Object.class);

        content.add(obj);
        checklistItem.setContent(content);
        checklistItem.setStatus(HttpStatus.OK);
        list.add(checklistItem);
        responseMap.put(list, HttpStatus.OK);

        // when checkAutoConfig is called
        when(service.checkAutoConfig(Mockito.anyString(), Mockito.eq(AIRLINE)))
                .thenReturn(CompletableFuture.completedFuture(responseMap));
        CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> tspReport = service
                .checkAutoConfig(TOKEN_STRING, AIRLINE);

        // then verify tspReport is generated
        verify(service).checkAutoConfig(TOKEN_STRING, AIRLINE);
        verify(service).checkAutoConfig(captorOne.capture(), captorTwo.capture());
        assertNotNull(tspReport);
        assertEquals(TOKEN_STRING, captorOne.getValue());
        assertEquals(AIRLINE, captorTwo.getValue());
        logger.debug("checkAutoConfig Test Passed");
    }

    @Test
    public void checkFlightPlan() throws NoSuchAlgorithmException, IOException, TspConfigLogException,
            FileDownloadException, InterruptedException, CancellationException, CompletionException, ExecutionException,
            AirlineStatusUnauthorizedException, ConfigurationException, FeatureManagementException {
        // given
        Map<List<AirlineStatusChecklistItem>, HttpStatus> responseMap = new LinkedHashMap<>();
        List<AirlineStatusChecklistItem> list = new ArrayList<>();
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        CosmosDbFlightPlanSource flightPlanSource = new CosmosDbFlightPlanSource(AIRLINE);
        List<Object> content = new ArrayList<>();
        content.add(flightPlanSource);

        checklistItem.setContent(content);
        checklistItem.setStatus(HttpStatus.OK);
        list.add(checklistItem);
        responseMap.put(list, HttpStatus.OK);

        // when checkFlightPlan is called
        when(service.checkFlightPlan(Mockito.anyString(), Mockito.eq(AIRLINE)))
                .thenReturn(CompletableFuture.completedFuture(responseMap));
        CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> flightPlanReport = service
                .checkFlightPlan(TOKEN_STRING, AIRLINE);

        // then verify flightPlanReport is generated
        verify(service).checkFlightPlan(TOKEN_STRING, AIRLINE);
        verify(service).checkFlightPlan(captorOne.capture(), captorTwo.capture());
        assertNotNull(flightPlanReport);
        assertEquals(TOKEN_STRING, captorOne.getValue());
        assertEquals(AIRLINE, captorTwo.getValue());
        logger.debug("checkFlightPlan Test Passed");
    }

    @Test
    public void checkAirlinePreferences() throws FileDownloadException, InterruptedException {
        // given
        Map<List<AirlineStatusChecklistItem>, HttpStatus> responseMap = new LinkedHashMap<>();
        List<AirlineStatusChecklistItem> checkList = new ArrayList<>();
        AirlineStatusChecklistItem checklistItem = new AirlineStatusChecklistItem();
        List<AirlinePreferences> list = new ArrayList<>();
        AirlinePreferences preferences = new AirlinePreferences();
        list.add(preferences);
        List<Object> description = new ArrayList<>();
        description.add(preferences);
        checklistItem.setContent(description);
        checklistItem.setStatus(HttpStatus.OK);
        checkList.add(checklistItem);
        responseMap.put(checkList, HttpStatus.OK);

        // when checkAirlinePreferences is called
        when(service.checkAirlinePreferences(Mockito.anyString(), Mockito.eq(AIRLINE)))
                .thenReturn(CompletableFuture.completedFuture(responseMap));
        CompletableFuture<Map<List<AirlineStatusChecklistItem>, HttpStatus>> preferencesReport = service
                .checkAirlinePreferences(TOKEN_STRING, AIRLINE);

        // then verify preferencesReport is generated
        verify(service).checkAirlinePreferences(TOKEN_STRING, AIRLINE);
        verify(service).checkAirlinePreferences(captorOne.capture(), captorTwo.capture());
        assertNotNull(preferencesReport);
        assertEquals(TOKEN_STRING, captorOne.getValue());
        assertEquals(AIRLINE, captorTwo.getValue());
        logger.debug("checkAirlinePreferences Test Passed");
    }

}
