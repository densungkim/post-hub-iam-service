package com.post.hub.iamservice.integration.controller;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.post.hub.iamservice.IamServiceApplication;
import com.post.hub.iamservice.model.dto.comment.CommentDTO;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.InvalidDataException;
import com.post.hub.iamservice.model.request.comment.CommentRequest;
import com.post.hub.iamservice.model.request.comment.UpdateCommentRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.security.JwtTokenProvider;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@Tag("integration")
public class CommentControllerTest {

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
        User admin = userRepository.findById(1)
                .orElseThrow(() -> new InvalidDataException("User with ID: 1 not found"));
        Hibernate.initialize(admin.getRoles());
        this.currentJwt = "Bearer " + jwtTokenProvider.generateToken(admin);
    }

    @Test
    void getComments_OK_200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/comments")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Transactional
    void createComment_OK_200() throws Exception {
        CommentRequest request = new CommentRequest(1, "Simple Comment Message");

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
    }

    @Test
    @Transactional
    void updateComment_OK_200() throws Exception {
        UpdateCommentRequest request = new UpdateCommentRequest(1, "Updated Comment Message");

        mockMvc.perform(MockMvcRequestBuilders
                        .put("/comments/1")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Transactional
    void deleteComment_OK_200() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/comments/1")
                        .header(HttpHeaders.AUTHORIZATION, currentJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    private IamResponse<CommentDTO> parseCommentDTOResponse(byte[] contentAsByteArray) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(IamResponse.class, CommentDTO.class);
            return objectMapper.readValue(contentAsByteArray, javaType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
