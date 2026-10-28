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
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;

import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EventFeignClientTest.TestConfig.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class EventFeignClientTest {

    @SpringBootApplication
    @EnableFeignClients(clients = EventFeignClient.class)
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private EventFeignClient feignClient;

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
    void findById_returnsEventFullDto() throws Exception {
        Long eventId = 123L;
        EventFullDto dto = EventFullDto.builder()
                .id(eventId)
                .title("Конференция Java")
                .build();
        String json = objectMapper.writeValueAsString(dto);

        wireMockServer.stubFor(get(urlEqualTo("/internal/events/" + eventId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        EventFullDto result = feignClient.findById(eventId);

        assertEquals(dto.getId(), result.getId());
        assertEquals(dto.getTitle(), result.getTitle());
    }

    @Test
    void existsByCategoryId_returnsTrue() {
        Long categoryId = 456L;
        String json = "true";

        wireMockServer.stubFor(get(urlEqualTo("/internal/events/category/" + categoryId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        boolean result = feignClient.existsByCategoryId(categoryId);
        assertTrue(result);
    }

    @Test
    void existsByCategoryId_returnsFalse() {
        Long categoryId = 789L;
        String json = "false";

        wireMockServer.stubFor(get(urlEqualTo("/internal/events/category/" + categoryId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        boolean result = feignClient.existsByCategoryId(categoryId);
        assertFalse(result);
    }

    @Test
    void isEventExists_returnsTrue() {
        Long eventId = 321L;
        String json = "true";

        wireMockServer.stubFor(get(urlEqualTo("/internal/events/" + eventId + "/exists"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        boolean result = feignClient.isEventExists(eventId);
        assertTrue(result);
    }

    @Test
    void getByIds_returnsListOfEventShortDto() throws Exception {
        Set<Long> ids = Set.of(1L, 2L, 3L);
        EventShortDto dto1 = EventShortDto.builder().id(1L).title("Meetup Kotlin").build();
        EventShortDto dto2 = EventShortDto.builder().id(2L).title("Вебинар Spring").build();
        EventShortDto dto3 = EventShortDto.builder().id(3L).title("Хакатон AI").build();
        List<EventShortDto> list = List.of(dto1, dto2, dto3);
        String json = objectMapper.writeValueAsString(list);

        // WireMock корректно обрабатывает query-параметры при совпадении URL
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/events"))
                .withQueryParam("ids", equalTo("1"))
                .withQueryParam("ids", equalTo("2"))
                .withQueryParam("ids", equalTo("3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        List<EventShortDto> result = feignClient.getByIds(ids);

        assertEquals(3, result.size());
        assertEquals(dto1.getId(), result.get(0).getId());
        assertEquals(dto2.getId(), result.get(1).getId());
        assertEquals(dto3.getId(), result.get(2).getId());
    }
}
