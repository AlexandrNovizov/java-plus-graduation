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
import ru.practicum.dto.category.CategoryDto;

import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = CategoryFeignClientTest.TestConfig.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class CategoryFeignClientTest {

    @SpringBootApplication
    @EnableFeignClients(clients = CategoryFeignClient.class)
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private CategoryFeignClient feignClient;

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
    void findById_shouldReturnCategoryDto() throws Exception {
        Long categoryId = 123L;
        String title = "Electronics";
        CategoryDto expected = new CategoryDto(categoryId, title);

        String json = objectMapper.writeValueAsString(expected);

        wireMockServer.stubFor(get(urlEqualTo("/categories/" + categoryId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        CategoryDto result = feignClient.findById(categoryId);

        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getName(), result.getName());
    }

    @Test
    void findByIds_shouldReturnListOfCategoryDto() throws Exception {
        Set<Long> ids = Set.of(1L, 2L, 3L);
        List<CategoryDto> expected = List.of(
                new CategoryDto(1L, "Books"),
                new CategoryDto(2L, "Movies"),
                new CategoryDto(3L, "Games")
        );

        String json = objectMapper.writeValueAsString(expected);

        // WireMock корректно обрабатывает query param "ids" как повторяемый параметр
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/categories"))
                .withQueryParam("ids", equalTo("1"))
                .withQueryParam("ids", equalTo("2"))
                .withQueryParam("ids", equalTo("3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(json)));

        List<CategoryDto> result = feignClient.findByIds(ids);

        assertEquals(3, result.size());
        assertEquals(expected.get(0).getId(), result.get(0).getId());
        assertEquals(expected.get(1).getId(), result.get(1).getId());
        assertEquals(expected.get(2).getId(), result.get(2).getId());
    }
}
