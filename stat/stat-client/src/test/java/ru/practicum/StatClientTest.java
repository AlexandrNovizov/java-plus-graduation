package ru.practicum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.practicum.feign.FeignStatClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.CoreMatchers.equalTo;

@SpringBootTest(classes = StatClientTest.TestConfig.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class StatClientTest {

    @SpringBootApplication
    @EnableFeignClients(clients = FeignStatClient.class)
    @Import({WireMockConfig.class})
    static class TestConfig {
    }

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private StatClient statClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        wireMockServer.resetAll();
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

        statClient.hit(endpointHit);

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

        List<ViewStats> response = statClient.getViewStats(request);

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

    @Test
    @DisplayName("Вернуть пустой список в случае срабатывания исключения.")
    void getViewStats_shouldReturnEmptyListWhenExceptionIsThrown() {
        StatsRequest request = new StatsRequest(
                "2020-05-05 00:00:00",
                "2035-05-05 00:00:00",
                List.of("/events/1"),
                true
        );

        wireMockServer.stubFor(get(urlPathEqualTo("/stats"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        List<ViewStats> response = statClient.getViewStats(request);

        assertThat(response, empty());

        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/stats")));
    }
}