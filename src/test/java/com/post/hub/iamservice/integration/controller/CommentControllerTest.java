package com.post.hub.iamservice.integration.controller;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.post.hub.iamservice.integration.BaseIntegrationTest;
import com.post.hub.iamservice.model.dto.comment.CommentDTO;
import com.post.hub.iamservice.model.dto.comment.CommentSearchDTO;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.InvalidDataException;
import com.post.hub.iamservice.model.request.comment.CommentRequest;
import com.post.hub.iamservice.model.request.comment.CommentSearchRequest;
import com.post.hub.iamservice.model.request.comment.UpdateCommentRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.security.JwtTokenProvider;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
public class CommentControllerTest extends BaseIntegrationTest {

    private static final int EXISTING_COMMENT_ID = 1;
    private static final int MISSING_COMMENT_ID = 9_999_999;
    private static final int EXISTING_POST_ID = 1;

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

    @BeforeAll
    @Transactional
    void authorize() {
        objectMapper.registerModule(new JavaTimeModule());

        User admin = userRepository.findById(1)
                .orElseThrow(() -> new InvalidDataException("User with ID: 1 not found"));
        Hibernate.initialize(admin.getRoles());
        this.currentJwt = "Bearer " + jwtTokenProvider.generateToken(admin);
    }

    @Test
    void getComments_OK_200() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/comments")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        IamResponse<PaginationResponse<CommentSearchDTO>> response =
                parseCommentSearchPageResponse(result.getResponse().getContentAsByteArray());

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getPayload());
        Assertions.assertNotNull(response.getPayload().getContent());
    }

    @Test
    void getComments_Unauthorized_401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/comments"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void getCommentById_OK_200() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/comments/{id}", EXISTING_COMMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        Assertions.assertEquals(200, result.getResponse().getStatus());

        IamResponse<CommentDTO> response =
                parseCommentDTOResponse(result.getResponse().getContentAsByteArray());

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getPayload());
        Assertions.assertEquals(EXISTING_COMMENT_ID, response.getPayload().getId());
    }

    @Test
    void getCommentById_NotFound_404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/comments/{id}", MISSING_COMMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @Transactional
    void createComment_OK_200() throws Exception {
        CommentRequest request = new CommentRequest(EXISTING_POST_ID, "Simple Comment Message " + System.nanoTime());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/comments")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        IamResponse<CommentDTO> response = parseCommentDTOResponse(result.getResponse().getContentAsByteArray());

        CommentDTO resultBody = response.getPayload();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(resultBody);
        Assertions.assertEquals(request.getMessage(), resultBody.getMessage());
        Assertions.assertEquals(request.getPostId(), resultBody.getPostId());
    }

    @Test
    @Transactional
    void createComment_BadRequest_400_onInvalidPayload() throws Exception {
        CommentRequest invalid = new CommentRequest(null, " ");

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/comments")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @Transactional
    void createComment_Unauthorized_401() throws Exception {
        CommentRequest request = new CommentRequest(EXISTING_POST_ID, "Hello");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @Transactional
    void updateComment_OK_200() throws Exception {
        UpdateCommentRequest request = new UpdateCommentRequest(EXISTING_POST_ID, "Updated Comment Message " + System.nanoTime());

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/comments/{id}", EXISTING_COMMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Transactional
    void updateComment_BadRequest_400_onInvalidPayload() throws Exception {
        UpdateCommentRequest invalid = new UpdateCommentRequest(null, " ");

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/comments/{id}", EXISTING_COMMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalid))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @Transactional
    void updateComment_NotFound_404() throws Exception {
        UpdateCommentRequest req = new UpdateCommentRequest(EXISTING_POST_ID, "Upd");

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/comments/{id}", MISSING_COMMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @Transactional
    void deleteComment_NoContent_204() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/comments/{id}", EXISTING_COMMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    @Transactional
    void deleteComment_NotFound_404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/comments/{id}", MISSING_COMMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void deleteComment_Unauthorized_401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/comments/{id}", EXISTING_COMMENT_ID))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @Transactional
    void searchComments_200_OK_withKeywordAndLimit() throws Exception {
        String prefix = "KEYWORD_" + System.nanoTime();
        mockMvc.perform(MockMvcRequestBuilders
                .post("/comments")
                .header(HttpHeaders.AUTHORIZATION, currentJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new CommentRequest(EXISTING_POST_ID, "hello " + prefix))));
        mockMvc.perform(MockMvcRequestBuilders
                .post("/comments")
                .header(HttpHeaders.AUTHORIZATION, currentJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new CommentRequest(EXISTING_POST_ID, prefix + " world"))));

        CommentSearchRequest request = new CommentSearchRequest();
        request.setKeyword(prefix);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/comments/search")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .param("page", "0")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        Assertions.assertEquals(200, result.getResponse().getStatus());

        IamResponse<PaginationResponse<CommentSearchDTO>> response =
                parseCommentSearchPageResponse(result.getResponse().getContentAsByteArray());

        PaginationResponse<CommentSearchDTO> payload = response.getPayload();

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(payload);
        Assertions.assertNotNull(payload.getContent());
        Assertions.assertFalse(payload.getContent().isEmpty());
        Assertions.assertTrue(payload.getContent().size() <= 5);
        Assertions.assertTrue(payload.getContent().stream().allMatch(c ->
                c.getMessage() != null && c.getMessage().contains(prefix)
        ));
    }

    private IamResponse<CommentDTO> parseCommentDTOResponse(byte[] contentAsByteArray) {
        try {
            JavaType javaType = objectMapper.getTypeFactory()
                    .constructParametricType(IamResponse.class, CommentDTO.class);
            return objectMapper.readValue(contentAsByteArray, javaType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private IamResponse<PaginationResponse<CommentSearchDTO>> parseCommentSearchPageResponse(byte[] contentAsByteArray) {
        try {
            JavaType pageType = objectMapper.getTypeFactory()
                    .constructParametricType(PaginationResponse.class, CommentSearchDTO.class);
            JavaType wrapper = objectMapper.getTypeFactory()
                    .constructParametricType(IamResponse.class, pageType);
            return objectMapper.readValue(contentAsByteArray, wrapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
