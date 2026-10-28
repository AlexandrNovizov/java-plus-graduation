package ru.practicum.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = RequestFeignClientTest.TestConfig.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class RequestFeignClientTest {

    @SpringBootApplication
    @EnableFeignClients(clients = RequestFeignClient.class)
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private RequestFeignClient feignClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    public static void init() {
        wireMockServer = new WireMockServer(80);
        wireMockServer.start();
    }

    @BeforeEach
    public void setUp() {
        wireMockServer.resetAll();
    }

    @AfterAll
    public static void cleanUp() {
        wireMockServer.stop();
    }

    @Test
    void getParticipationRequestsByEventId_success() throws Exception {
        Long eventId = 1L;
        List<ParticipationRequestDto> expected = List.of(
                new ParticipationRequestDto(1L, Instant.now().toString(), 1L, 10L, "PENDING"),
                new ParticipationRequestDto(2L, Instant.now().toString(), 1L, 20L, "CONFIRMED")
        );

        String json = objectMapper.writeValueAsString(expected);

        wireMockServer.stubFor(get(urlEqualTo("/internal/requests/events/" + eventId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        List<ParticipationRequestDto> result = feignClient.getParticipationRequestsByEventId(eventId);

        assertEquals(expected, result);
    }

    @Test
    void findByIdsAndEventId_success() throws Exception {
        Long eventId = 2L;
        Set<Long> requestIds = Set.of(101L, 102L);
        List<ParticipationRequestDto> expected = List.of(
                new ParticipationRequestDto(101L, Instant.now().toString(), 2L, 30L, "REJECTED"),
                new ParticipationRequestDto(102L, Instant.now().toString(), 2L, 40L, "CANCELED")
        );

        String json = objectMapper.writeValueAsString(expected);

        // WireMock: query param ids передаётся как повторяемый параметр (ids=101&ids=102)
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/requests/events/" + eventId))
                .withQueryParam("ids", equalTo("101"))
                .withQueryParam("ids", equalTo("102"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        List<ParticipationRequestDto> result = feignClient.findByIdsAndEventId(requestIds, eventId);

        assertEquals(expected, result);
    }

    @Test
    void countByEventIdAndStatus_success() throws Exception {
        Long eventId = 3L;
        RequestStatus status = RequestStatus.CONFIRMED;
        EventRequestsCountDto expected = new EventRequestsCountDto(eventId, 5L);

        String json = objectMapper.writeValueAsString(expected);

        wireMockServer.stubFor(get(urlPathEqualTo("/internal/requests/events/" + eventId + "/count"))
                .withQueryParam("status", equalTo(status.name()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        EventRequestsCountDto result = feignClient.countByEventIdAndStatus(eventId, status);

        assertEquals(expected, result);
    }

    @Test
    void countByEventIdsAndStatus_success() throws Exception {
        Set<Long> eventIds = Set.of(4L, 5L);
        RequestStatus status = RequestStatus.PENDING;
        List<EventRequestsCountDto> expected = List.of(
                new EventRequestsCountDto(4L, 7L),
                new EventRequestsCountDto(5L, 3L)
        );

        String json = objectMapper.writeValueAsString(expected);

        // ids=4&ids=5
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/requests/events/count"))
                .withQueryParam("ids", equalTo("4"))
                .withQueryParam("ids", equalTo("5"))
                .withQueryParam("status", equalTo(status.name()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        List<EventRequestsCountDto> result = feignClient.countByEventIdsAndStatus(eventIds, status);

        assertEquals(expected, result);
    }

    @Test
    void updateRequestsStatus_success() throws Exception {
        Set<Long> requestIds = Set.of(201L, 202L);
        RequestStatus status = RequestStatus.CONFIRMED;
        List<ParticipationRequestDto> expected = List.of(
                new ParticipationRequestDto(201L, Instant.now().toString(), 6L, 50L, status.name()),
                new ParticipationRequestDto(202L, Instant.now().toString(), 6L, 60L, status.name())
        );

        String json = objectMapper.writeValueAsString(expected);

        // PUT /internal/requests?ids=201&ids=202&status=CONFIRMED
        wireMockServer.stubFor(put(urlPathEqualTo("/internal/requests"))
                .withQueryParam("ids", equalTo("201"))
                .withQueryParam("ids", equalTo("202"))
                .withQueryParam("status", equalTo(status.name()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        List<ParticipationRequestDto> result = feignClient.updateRequestsStatus(requestIds, status);

        assertEquals(expected, result);
    }
}
