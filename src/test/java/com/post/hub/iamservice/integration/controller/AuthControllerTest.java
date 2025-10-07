package com.post.hub.iamservice.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.post.hub.iamservice.integration.BaseIntegrationTest;
import com.post.hub.iamservice.model.request.user.LoginRequest;
import com.post.hub.iamservice.model.request.user.RegistrationUserRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import lombok.Setter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    @Setter
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Order(1)
    void loginUserAsSuperAdmin_OK_200() throws Exception {
        LoginRequest request = new LoginRequest("super_admin@gmail.com", "password1");

        MvcResult requestResult = mvc.perform(MockMvcRequestBuilders
                        .post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        IamResponse<?> response = parseResponse(requestResult.getResponse());

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getPayload());
    }

    @Test
    @Order(2)
    void loginUser_INVALID_CREDENTIALS_401() throws Exception {
        LoginRequest request = new LoginRequest("wronguser@example.com", "WrongPassword123!");

        mvc.perform(MockMvcRequestBuilders
                        .post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @Order(3)
    @Transactional
    void registerUser_OK_200() throws Exception {
        RegistrationUserRequest request = new RegistrationUserRequest(
                "newuser",
                "newuser@example.com",
                "NewPassword123!",
                "NewPassword123!"
        );

        MvcResult requestResult = mvc.perform(MockMvcRequestBuilders
                        .post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        IamResponse<?> response = parseResponse(requestResult.getResponse());

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getPayload());
    }

    @Test
    @Order(4)
    void registerUser_USER_ALREADY_EXISTS_400() throws Exception {
        RegistrationUserRequest request = new RegistrationUserRequest(
                "user",
                "user@gmail.com",
                "Test1234!",
                "Test1234!"
        );

        mvc.perform(MockMvcRequestBuilders
                        .post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    private IamResponse<?> parseResponse(MockHttpServletResponse response) throws IOException {
        return objectMapper.readValue(response.getContentAsByteArray(), IamResponse.class);
    }
}
