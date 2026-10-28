package ru.practicum.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.practicum.dto.stat.EndpointHit;
import ru.practicum.dto.stat.StatsRequest;
import ru.practicum.dto.stat.ViewStats;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(classes = StatFeignClientTest.TestConfig.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class StatFeignClientTest {

    @SpringBootApplication
    @EnableFeignClients(clients = StatFeignClient.class)
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private StatFeignClient feignClient;

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
    @SneakyThrows
    @DisplayName("Успешная отправка запроса на сервер.")
    void hit_shouldSendPostRequest() {
        EndpointHit endpointHit = new EndpointHit(
                1L,
                "ewm-main",
                "/events/1",
                "192.168.1.1",
                "2026-06-18 10:00:00");

        wireMockServer.stubFor(post(urlEqualTo("/hit"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson(
                        objectMapper.writeValueAsString(endpointHit)
                ))
                .willReturn(aResponse().withStatus(201)));

        feignClient.hit(endpointHit);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/hit")));
    }

    @Test
    @SneakyThrows
    @DisplayName("Успешный запрос статистики с сервера.")
    void getViewStats_shouldReturnStatsList() {
        String uri = "/events/1";
        StatsRequest request = new StatsRequest(
                "2020-05-05 00:00:00",
                "2035-05-05 00:00:00",
                List.of(uri),
                true
        );

        List<ViewStats> expectedStats = List.of(new ViewStats("ewm-main","/events/1",5L));

        wireMockServer.stubFor(get(urlPathEqualTo("/stats"))
                .withQueryParam("start", WireMock.equalTo(request.getStart()))
                .withQueryParam("end", WireMock.equalTo(request.getEnd()))
                .withQueryParam("uris", WireMock.equalTo(uri))
                .withQueryParam("unique", WireMock.equalTo("true"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expectedStats))
                ));

        List<ViewStats> response = feignClient.getViewStats(request);

        wireMockServer.verify(
                getRequestedFor(urlPathEqualTo("/stats"))
                        .withQueryParam("start", WireMock.equalTo(request.getStart()))
                        .withQueryParam("end", WireMock.equalTo(request.getEnd()))
                        .withQueryParam("uris", WireMock.equalTo(uri))
                        .withQueryParam("unique", WireMock.equalTo("true"))
        );

        assertThat(response, hasSize(1));
        assertThat(response.getFirst().getApp(), equalTo("ewm-main"));
        assertThat(response.getFirst().getApp(), equalTo("ewm-main"));
        assertThat(response.getFirst().getHits(), equalTo(5L));
    }
}
