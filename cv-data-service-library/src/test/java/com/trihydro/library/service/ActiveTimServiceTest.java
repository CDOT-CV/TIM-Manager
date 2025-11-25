package com.trihydro.library.service;

import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.trihydro.library.model.ActiveRsuTimQueryModel;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.CVRestServiceProps;
import com.trihydro.library.model.TimUpdateModel;
import com.trihydro.library.model.WydotTim;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class ActiveTimServiceTest {
    @Mock
    RestTemplateProvider mockRestTemplateProvider;

    @Mock
    CVRestServiceProps mockConfig;

    Long timTypeId = -1L;
    List<WydotTim> wydotTims;

    @InjectMocks
    ActiveTimService uut;

    String baseUrl = "http://localhost:8080";
    RestTemplate restTemplate = new RestTemplate();

    @Mock
    MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();

    @BeforeEach
    void setupSubTest() {
        mockRestTemplateProvider = Mockito.mock(RestTemplateProvider.class);
        mockConfig = Mockito.mock(CVRestServiceProps.class);
        uut = new ActiveTimService();
        uut.InjectDependencies(mockConfig, mockRestTemplateProvider);
        lenient().when(mockRestTemplateProvider.GetRestTemplate()).thenReturn(restTemplate);
        when(mockConfig.getCvRestService()).thenReturn(baseUrl);
    }

    void setupWydotTims() {
        wydotTims = new ArrayList<>();
        WydotTim wydotTim = new WydotTim();
        wydotTim.setDirection("d");
        wydotTim.setClientId("unit_test_id1");
        wydotTims.add(wydotTim);
        wydotTim = new WydotTim();
        wydotTim.setDirection("i");
        wydotTim.setClientId("unit_test_id2");
        wydotTims.add(wydotTim);
    }

    @Test
    void updateActiveTim_SatRecordId() {
        // prepare
        Long activeTimId = -1L;
        String satRecordId = "asdf";
        String url = String.format("%s/active-tim/update-sat-record-id/%d/%s", baseUrl, activeTimId, satRecordId);
        String jsonString = "true";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        Boolean data = uut.updateActiveTim_SatRecordId(activeTimId, satRecordId);

        // verify
        mockServer.verify();
        Assertions.assertTrue(data, "Update failed when should have succeeded");
    }

    @Test
    void addItisCodesToActiveTim() {
        // prepare
        ActiveTim activeTim = new ActiveTim();
        activeTim.setActiveTimId(-1L);
        String url = String.format("%s/active-tim/itis-codes/%d", baseUrl, activeTim.getActiveTimId());
        String jsonString = "[0, 1, 2]";
        mockServer.expect(requestTo(url))
            .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        uut.addItisCodesToActiveTim(activeTim);

        // verify
        mockServer.verify();
        Assertions.assertEquals(3, activeTim.getItisCodes().size());
        Assertions.assertEquals(Integer.valueOf(0), activeTim.getItisCodes().get(0));
        Assertions.assertEquals(Integer.valueOf(1), activeTim.getItisCodes().get(1));
        Assertions.assertEquals(Integer.valueOf(2), activeTim.getItisCodes().get(2));
    }

    @Test
    void deleteActiveTim() {
        // prepare
        Long activeTimId = -1L;
        String url = String.format("%s/active-tim/delete-id/%d", baseUrl, activeTimId);
        String jsonString = "true";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        boolean data = uut.deleteActiveTim(activeTimId);

        // verify
        mockServer.verify();
        Assertions.assertTrue(data, "Reported failure when success");
    }

    @Test
    void deleteActiveTimsById() throws SQLException {
        // prepare
        List<Long> activeTimIds = new ArrayList<Long>();
        activeTimIds.add(-1L);
        activeTimIds.add(-2L);
        String url = String.format("%s/active-tim/delete-ids", baseUrl);
        String jsonString = "true";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        boolean success = uut.deleteActiveTimsById(activeTimIds);

        // verify
        Assertions.assertTrue(success);
    }

    @Test
    void getActiveTimIndicesByRsu() {
        // prepare
        String rsuTarget = "10.10.10.10";
        String url = String.format("%s/active-tim/indices-rsu/%s", baseUrl, rsuTarget);
        String jsonString = "[0, 1, 2]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<Integer> data = uut.getActiveTimIndicesByRsu(rsuTarget);

        // verify
        mockServer.verify();
        Assertions.assertEquals(3, data.size());
        Assertions.assertEquals(Integer.valueOf(0), data.get(0));
        Assertions.assertEquals(Integer.valueOf(1), data.get(1));
        Assertions.assertEquals(Integer.valueOf(2), data.get(2));
    }

    @Test
    void getActiveTimsByWydotTim() throws SQLException {
        // prepare
        setupWydotTims();
        String url = String.format("%s/active-tim/get-by-wydot-tim/%d", baseUrl, timTypeId);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getActiveTimsByWydotTim(wydotTims, timTypeId);

        // verify
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
    }

    @Test
    void getActiveTimsByClientIdDirection_SingleTim() {
        // prepare
        String clientId = "clientId";
        String direction = "westward";
        String url = String.format("%s/active-tim/client-id-direction/%s/%d/%s", baseUrl, clientId, timTypeId,
                direction);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getActiveTimsByClientIdDirection(clientId, timTypeId, direction);

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
    }

    @Test
    void getActiveTimByClientIdDirection_MultipleTims() {
        // prepare
        String clientId = "clientId";
        String direction = "westward";
        String url = String.format("%s/active-tim/client-id-direction/%s/%d/%s", baseUrl, clientId, timTypeId, direction);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"},"
                + "{\"activeTimId\":-2,\"direction\":\"d\",\"clientId\":\"unit_test_id2\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getActiveTimsByClientIdDirection(clientId, timTypeId, direction);

        // verify
        mockServer.verify();
        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
        Assertions.assertEquals(-2L, data.get(1).getActiveTimId());
    }

    @Test
    void getActiveTimByClientIdDirection_NoTims() {
        // prepare
        String clientId = "clientId";
        String direction = "westward";
        String url = String.format("%s/active-tim/client-id-direction/%s/%d/%s", baseUrl, clientId, timTypeId,
                direction);
        String jsonString = "[]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getActiveTimsByClientIdDirection(clientId, timTypeId, direction);

        // verify
        mockServer.verify();
        Assertions.assertEquals(0, data.size());
    }

    @Test
    void getActiveTimByClientIdDirection_ObjectInsteadOfArray() {
        // prepare
        String clientId = "clientId";
        String direction = "westward";
        String url = String.format("%s/active-tim/client-id-direction/%s/%d/%s", baseUrl, clientId, timTypeId,
                direction);
        String jsonString = "{\"key\": \"value\"}";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getActiveTimsByClientIdDirection(clientId, timTypeId, direction);

        // verify
        mockServer.verify();
        Assertions.assertEquals(0, data.size());
    }

    @Test
    void getBufferTimsByClientId_SingleTim() {
        // prepare
        String clientId = "clientId";
        String url = String.format("%s/active-tim/buffer-tims/%s", baseUrl, clientId);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getBufferTimsByClientId(clientId);

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
    }

    @Test
    void getBufferTimsByClientId_MultipleTims() {
        // prepare
        String clientId = "clientId";
        String url = String.format("%s/active-tim/buffer-tims/%s", baseUrl, clientId);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"},"
                + "{\"activeTimId\":-2,\"direction\":\"d\",\"clientId\":\"unit_test_id2\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getBufferTimsByClientId(clientId);

        // verify
        mockServer.verify();
        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
        Assertions.assertEquals(-2L, data.get(1).getActiveTimId());
    }

    @Test
    void getBufferTimsByClientId_NoTims() {
        // prepare
        String clientId = "clientId";
        String url = String.format("%s/active-tim/buffer-tims/%s", baseUrl, clientId);
        String jsonString = "[]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getBufferTimsByClientId(clientId);

        // verify
        mockServer.verify();
        Assertions.assertEquals(0, data.size());
    }

    @Test
    void getBufferTimsByClientId_ObjectInsteadOfArray() {
        // prepare
        String clientId = "clientId";
        String url = String.format("%s/active-tim/buffer-tims/%s", baseUrl, clientId);
        String jsonString = "{\"key\": \"value\"}";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getBufferTimsByClientId(clientId);

        // verify
        mockServer.verify();
        Assertions.assertEquals(0, data.size());
    }

    @Test
    void getExpiredActiveTims() {
        // prepare
        String url = String.format("%s/active-tim/expired?limit=500", baseUrl);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getExpiredActiveTims(500);

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
    }

    @Test
    void getActivesTimByType() {
        // prepare
        String url = String.format("%s/active-tim/tim-type-id/%d", baseUrl, timTypeId);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getActivesTimByType(timTypeId);

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
    }

    @Test
    void getActiveRsuTim() {
        // prepare
        String clientId = "clientId";
        String direction = "westward";
        String ipv4Address = "10.10.10.10";
        String url = String.format("%s/active-tim/active-rsu-tim", baseUrl);
        ActiveRsuTimQueryModel artqm = new ActiveRsuTimQueryModel(direction, clientId, ipv4Address);
        String jsonString = "{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        ActiveTim data = uut.getActiveRsuTim(artqm);

        // verify
        mockServer.verify();
        Assertions.assertNotNull(data);
        Assertions.assertEquals(-1L, data.getActiveTimId());
    }

    @Test
    void getExpiringActiveTims() {
        // prepare
        String url = String.format("%s/active-tim/expiring", baseUrl);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<TimUpdateModel> data = uut.getExpiringActiveTims();

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
    }

    @Test
    void getActiveTimsMissingItisCodes() throws SQLException {
        // prepare
        String url = String.format("%s/active-tim/missing-itis", baseUrl);
        String jsonString = "[{\"timId\":1,\"direction\":\"both\",\"route\":\"I 80\",\"clientId\":\"123\",\"satRecordId\":\"HEX\",\"activeTimId\":1}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> ats = uut.getActiveTimsMissingItisCodes();

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, ats.size());
        ActiveTim tim = ats.get(0);
        Assertions.assertEquals(1, tim.getTimId());
        Assertions.assertEquals("both", tim.getDirection());
        Assertions.assertEquals("I 80", tim.getRoute());
        Assertions.assertEquals("123", tim.getClientId());
        Assertions.assertEquals("HEX", tim.getSatRecordId());
        Assertions.assertEquals(1, tim.getActiveTimId());
    }

    @Test
    void getActiveTimsNotSent() {
        // prepare
        String url = String.format("%s/active-tim/not-sent", baseUrl);
        String jsonString = "[{\"timId\":1,\"direction\":\"both\",\"route\":\"I 80\",\"clientId\":\"123\",\"satRecordId\":\"HEX\",\"activeTimId\":1}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> ats = uut.getActiveTimsNotSent();

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, ats.size());
        ActiveTim tim = ats.get(0);
        Assertions.assertEquals(1, tim.getTimId());
        Assertions.assertEquals("both", tim.getDirection());
        Assertions.assertEquals("I 80", tim.getRoute());
        Assertions.assertEquals("123", tim.getClientId());
        Assertions.assertEquals("HEX", tim.getSatRecordId());
        Assertions.assertEquals(1, tim.getActiveTimId());
    }

    @Test
    void getActiveTimsForSDX_success() {
        // prepare
        String url = String.format("%s/active-tim/all-sdx", baseUrl);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> result = uut.getActiveTimsForSDX();

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(-1L, result.get(0).getActiveTimId());
    }

    @Test
    void getActiveTimsForSDX_throwsError() {
        // prepare
        String url = String.format("%s/active-tim/all-sdx", baseUrl);
        String jsonString = "{}";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        Assertions.assertThrows(RestClientException.class, () -> {
            uut.getActiveTimsForSDX();
        });
    }

    @Test
    void getActiveTimsWithItisCodesWithExclusions_success() {
        // prepare
        String url = String.format("%s/active-tim/all-with-itis?excludeVslAndParking=true", baseUrl);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> result = uut.getActiveTimsWithItisCodes(true);

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(-1L, result.get(0).getActiveTimId());
    }

    @Test
    void getActiveTimsWithItisCodes_success() {
        // prepare
        String url = String.format("%s/active-tim/all-with-itis?excludeVslAndParking=false", baseUrl);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> result = uut.getActiveTimsWithItisCodes(false);

        // verify
        mockServer.verify();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(-1L, result.get(0).getActiveTimId());
    }

    @Test
    void getActiveTimsWithItisCodes_throwsError() {
        // prepare
        String url = String.format("%s/active-tim/all-with-itis?excludeVslAndParking=true", baseUrl);
        String jsonString = "{}";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        Assertions.assertThrows(RestClientException.class, () -> {
            uut.getActiveTimsWithItisCodes(true);
        });
    }

    @Test
    void updateActiveTimExpiration_SUCCESS() {
        // prepare
        String packetID = "3C8E8DF2470B1A772E";
        String expDate = "2020-10-20T16:26:07.000Z";
        String url = String.format("%s/active-tim/update-expiration/%s/%s", baseUrl, packetID, expDate);
        String jsonString = "true";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        Boolean data = uut.updateActiveTimExpiration(packetID, expDate);

        // verify
        mockServer.verify();
        Assertions.assertTrue(data, "Update failed when should have succeeded");
    }

    @Test
    void updateActiveTimExpiration_FAIL() {
        // prepare
        String packetID = "3C8E8DF2470B1A772E";
        String expDate = "2020-10-20T16:26:07.000Z";
        String url = String.format("%s/active-tim/update-expiration/%s/%s", baseUrl, packetID, expDate);
        String jsonString = "false";
        mockServer.expect(requestTo(url)).andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        Boolean data = uut.updateActiveTimExpiration(packetID, expDate);

        // verify
        mockServer.verify();
        Assertions.assertFalse(data, "Update succeeded when should have failed");
    }

    @Test
    void getMinExpiration_SUCCESS() {
        // prepare
        String packetID = "3C8E8DF2470B1A772E";
        String expDate = "2020-10-20T16:26:07.000Z";
        String minDate = "27-OCT-20 06.21.00.000 PM";
        String url = String.format("%s/active-tim/get-min-expiration/%s/%s", baseUrl, packetID, expDate);
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(minDate, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        String data = uut.getMinExpiration(packetID, expDate);

        // verify
        mockServer.verify();
        Assertions.assertEquals(minDate, data);
    }

    @Test
    void markForDeletion_True() {
        // prepare
        long activeTimId = 1L;
        String url = String.format("%s/active-tim/mark-for-deletion/%d", baseUrl, activeTimId);
        String jsonString = "true";
        mockServer.expect(requestTo(url))
            .andRespond(
                withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        uut.markForDeletion(activeTimId);

        // verify
        mockServer.verify();
    }

    @Test
    void markForDeletion_False() {
        // prepare
        long activeTimId = 1L;
        String url = String.format("%s/active-tim/mark-for-deletion/%d", baseUrl, activeTimId);
        String jsonString = "false";
        mockServer.expect(requestTo(url))
            .andRespond(
                withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        uut.markForDeletion(activeTimId);

        // verify
        mockServer.verify();
    }

    @Test
    void getAllRecords_SuccessfulRetrieval_ShouldReturnRecords() {
        // prepare
        String url = String.format("%s/active-tim/all", baseUrl);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"unit_test_id1\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getAllRecords();

        // verify
        mockServer.verify();
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
        Assertions.assertEquals("d", data.get(0).getDirection());
        Assertions.assertEquals("unit_test_id1", data.get(0).getClientId());
    }

    @Test
    void getActivePlannedConditionTims_Success() {
        // prepare
        String url = String.format("%s/active-tim/get-active-planned-condition-tims", baseUrl);
        String jsonString = "[{\"activeTimId\":-1,\"direction\":\"d\",\"clientId\":\"planned-10_trgd_10\"}]";
        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(jsonString, org.springframework.http.MediaType.APPLICATION_JSON));

        // execute
        List<ActiveTim> data = uut.getActivePlannedConditionTims();

        // verify
        mockServer.verify();
        Assertions.assertNotNull(data);
        Assertions.assertEquals(1, data.size());
        Assertions.assertEquals(-1L, data.get(0).getActiveTimId());
        Assertions.assertEquals("d", data.get(0).getDirection());
        Assertions.assertEquals("planned-10_trgd_10", data.get(0).getClientId());
    }
}