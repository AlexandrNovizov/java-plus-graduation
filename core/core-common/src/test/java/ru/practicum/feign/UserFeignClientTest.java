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
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserParamDto;

import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = UserFeignClientTest.TestConfig.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class UserFeignClientTest {

    @SpringBootApplication
    @EnableFeignClients(clients = UserFeignClient.class)
    static class TestConfig {
    }

    private static WireMockServer wireMockServer;

    @Autowired
    private UserFeignClient feignClient;

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
    void testGetUsers_success() throws Exception {
        var dto = new UserParamDto();
        dto.setIds(Set.of(1L, 2L));
        dto.setFrom(0);
        dto.setSize(10);

        var users = List.of(
                UserDto.builder().id(1L).email("user1@example.com").name("User One").build(),
                UserDto.builder().id(2L).email("user2@example.com").name("User Two").build()
        );
        String json = objectMapper.writeValueAsString(users);

        wireMockServer.stubFor(
                get(urlPathEqualTo("/admin/users"))
                        .withQueryParam("ids", equalTo("1"))
                        .withQueryParam("ids", equalTo("2"))
                        .withQueryParam("from", equalTo("0"))
                        .withQueryParam("size", equalTo("10"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(json))
        );

        List<UserDto> result = feignClient.getUsers(dto);

        assertEquals(2, result.size());
        assertEquals("user1@example.com", result.get(0).getEmail());
        assertEquals("user2@example.com", result.get(1).getEmail());
    }

    @Test
    void testFindById_success() throws Exception {
        Long userId = 42L;

        var user = UserFullDto.builder()
                .id(userId)
                .email("admin@example.com")
                .name("Admin User")
                .adminWarnings(0)
                .rank("ADMIN")
                .build();
        String json = objectMapper.writeValueAsString(user);

        wireMockServer.stubFor(
                get(urlPathMatching("/internal/users/" + userId))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(json))
        );

        UserFullDto result = feignClient.findById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("admin@example.com", result.getEmail());
    }

    @Test
    void testUpdateUser_success() throws Exception {
        var input = UserFullDto.builder()
                .id(99L)
                .email("updated@example.com")
                .name("Updated Name")
                .commentsCount(5)
                .build();

        String requestJson = objectMapper.writeValueAsString(input);
        String responseJson = objectMapper.writeValueAsString(input); // обычно возвращают тот же DTO

        wireMockServer.stubFor(
                put(urlPathEqualTo("/internal/users"))
                        .withHeader("Content-Type", matching("application/json.*"))
                        .withRequestBody(equalToJson(requestJson, true, false)) // ignoreArrayOrder, ignoreExtraFields
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(responseJson))
        );

        UserFullDto result = feignClient.updateUser(input);

        assertNotNull(result);
        assertEquals(input.getId(), result.getId());
        assertEquals(input.getEmail(), result.getEmail());
    }

    @Test
    void testCheckUserExists_success() {
        Long userId = 77L;

        wireMockServer.stubFor(
                get(urlPathMatching("/internal/users/exists/" + userId))
                        .willReturn(aResponse().withStatus(200))
        );

        // Метод возвращает void, проверяем отсутствие исключения
        feignClient.checkUserExists(userId);
    }
}
