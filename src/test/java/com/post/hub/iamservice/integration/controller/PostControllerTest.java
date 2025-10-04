package com.post.hub.iamservice.integration.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.post.hub.iamservice.IamServiceApplication;
import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.model.dto.post.PostDTO;
import com.post.hub.iamservice.model.dto.post.PostSearchDTO;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.InvalidDataException;
import com.post.hub.iamservice.model.request.post.NewPostRequest;
import com.post.hub.iamservice.model.request.post.PostSearchRequest;
import com.post.hub.iamservice.model.request.post.UpdatePostRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.security.JwtTokenProvider;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(classes = IamServiceApplication.class)
@AutoConfigureMockMvc
@ExtendWith({MockitoExtension.class, SpringExtension.class})
@TestPropertySource(properties = {"logging.level.com.post.hub.iamservice.advice.CommonControllerAdvice=OFF"})
@Tag("integration")
public class PostControllerTest {

    private static final int EXISTING_POST_ID = 1;
    private static final int MISSING_POST_ID = 9_999_999;

    @Autowired
    @Setter
    private MockMvc mockMvc;

    @Autowired
    @Setter
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    @Setter
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String currentJwt;
    private String invalidJwt;

    @BeforeAll
    @Transactional
    void authorize() {
        User admin = userRepository.findById(1)
                .orElseThrow(() -> new InvalidDataException("User with ID: 1 not found"));
        Hibernate.initialize(admin.getRoles());
        this.currentJwt = "Bearer " + jwtTokenProvider.generateToken(admin);
        this.invalidJwt = "Invalid " + jwtTokenProvider.generateToken(admin);
    }

    @Test
    void getAllPosts_200_OK() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        IamResponse<PaginationResponse<PostSearchDTO>> response =
                parsePostSearchPageResponse(result.getResponse().getContentAsByteArray());

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getPayload());
        Assertions.assertNotNull(response.getPayload().getContent());
    }

    @Test
    void getAllPosts_Unauthorized_401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts")
                        .header(HttpHeaders.AUTHORIZATION, invalidJwt))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void getPostById_200_OK() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/{id}", EXISTING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        IamResponse<PostDTO> response =
                parsePostDTOResponse(result.getResponse().getContentAsByteArray());

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getPayload());
        Assertions.assertEquals(EXISTING_POST_ID, response.getPayload().getId());
    }

    @Test
    void getPostById_Unauthorized_401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/{id}", EXISTING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, invalidJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void getPostById_NotFound_404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/{id}", MISSING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @Transactional
    void createPost_OK_200() throws Exception {
        NewPostRequest request = new NewPostRequest("Simple Title", "Simple content", 50);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/posts")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        IamResponse<PostDTO> response = parsePostDTOResponse(result.getResponse().getContentAsByteArray());

        PostDTO body = response.getPayload();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(body);
        Assertions.assertEquals(request.getTitle(), body.getTitle());
        Assertions.assertEquals(request.getContent(), body.getContent());
        Assertions.assertEquals(request.getLikes(), body.getLikes());
    }

    @Test
    @Transactional
    void createPost_InvalidToken_Unauthorized_401() throws Exception {
        NewPostRequest request = new NewPostRequest("Invalid JWT case", "content", 0);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/posts")
                        .header(HttpHeaders.AUTHORIZATION, invalidJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @Transactional
    void createPost_BadRequest_400_onInvalidPayload() throws Exception {
        NewPostRequest invalid = new NewPostRequest(" ", "", null);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/posts")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @Transactional
    void updatePost_200_OK() throws Exception {
        UpdatePostRequest request = new UpdatePostRequest("Updated Title", "Updated content", 100);

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/{id}", EXISTING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Transactional
    void updatePost_InvalidToken_Unauthorized_401() throws Exception {
        UpdatePostRequest request = new UpdatePostRequest("Try update", "Try", 1);

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/{id}", EXISTING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, invalidJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @Transactional
    void updatePost_BadRequest_400_onInvalidPayload() throws Exception {
        UpdatePostRequest invalid = new UpdatePostRequest("", " ", -1);

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/{id}", EXISTING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @Transactional
    void updatePost_NotFound_404() throws Exception {
        UpdatePostRequest request = new UpdatePostRequest("X", "Y", 1);

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/{id}", MISSING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @Transactional
    void deletePost_204_NoContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/{id}", EXISTING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    @Transactional
    void deletePost_InvalidToken_Unauthorized_401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/{id}", EXISTING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, invalidJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @Transactional
    void deletePost_NotFound_404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/{id}", MISSING_POST_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void searchPosts_200_OK() throws Exception {
        PostSearchRequest request = new PostSearchRequest(); // пустой запрос допустим

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/posts/search")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .param("page", "0")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        IamResponse<PaginationResponse<PostSearchDTO>> response =
                parsePostSearchPageResponse(result.getResponse().getContentAsByteArray());

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getPayload());
        Assertions.assertNotNull(response.getPayload().getContent());
    }

    @Test
    void searchPosts_Unauthorized_401() throws Exception {
        PostSearchRequest request = new PostSearchRequest();

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/posts/search")
                        .header(HttpHeaders.AUTHORIZATION, invalidJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    private IamResponse<PostDTO> parsePostDTOResponse(byte[] contentAsByteArray) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.readValue(contentAsByteArray, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private IamResponse<PaginationResponse<PostSearchDTO>> parsePostSearchPageResponse(byte[] contentAsByteArray) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.readValue(contentAsByteArray, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
