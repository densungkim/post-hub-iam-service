package com.post.hub.iamservice.controller;

import com.post.hub.iamservice.model.constants.ApiLogMessage;
import com.post.hub.iamservice.model.dto.user.UserProfileDTO;
import com.post.hub.iamservice.model.request.user.ChangePasswordRequest;
import com.post.hub.iamservice.model.request.user.LoginRequest;
import com.post.hub.iamservice.model.request.user.RegistrationUserRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.service.AuthService;
import com.post.hub.iamservice.utils.ApiUtils;
import com.post.hub.iamservice.utils.CookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("${endpoint.auth}")
public class AuthController {
    private final AuthService authService;

    @PostMapping("${endpoint.login}")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful authorization",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"token\": \"eyJhbGcIoIJIuz...\" }")
                    )
            )
    })
    @Operation(summary = "User login", description = "Authenticates the user and returns an access/refresh token")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<UserProfileDTO> result = authService.login(request);

        String accessSetCookie = CookieUtils.accessCookie(result.getPayload().getToken(), Duration.ofMinutes(60));
        String refreshSetCookie = CookieUtils.refreshCookie(result.getPayload().getRefreshToken(), Duration.ofDays(7));
        response.addHeader(HttpHeaders.SET_COOKIE, accessSetCookie);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshSetCookie);

        return ResponseEntity.ok(result);
    }

    @PostMapping("${endpoint.register}")
    @Operation(summary = "Register a new user", description = "Creates new user and returns authentication details")
    public ResponseEntity<?> register(
            @RequestBody @Valid RegistrationUserRequest request,
            HttpServletResponse response
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<UserProfileDTO> result = authService.registerUser(request);

        String accessSetCookie = CookieUtils.accessCookie(result.getPayload().getToken(), Duration.ofMinutes(60));
        String refreshSetCookie = CookieUtils.refreshCookie(result.getPayload().getRefreshToken(), Duration.ofDays(7));
        response.addHeader(HttpHeaders.SET_COOKIE, accessSetCookie);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshSetCookie);

        return ResponseEntity.ok(result);
    }

    @PostMapping("${endpoint.refresh.token}")
    @Operation(summary = "Refresh access token", description = "Issues a new short-lived access token")
    public ResponseEntity<IamResponse<UserProfileDTO>> refreshToken(
            @CookieValue(name = CookieUtils.REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<UserProfileDTO> result = authService.refreshAccessToken(refreshToken);

        String accessSetCookie = CookieUtils.accessCookie(result.getPayload().getToken(), Duration.ofMinutes(60));
        String refreshSetCookie = CookieUtils.refreshCookie(result.getPayload().getRefreshToken(), Duration.ofDays(7));
        response.addHeader(HttpHeaders.SET_COOKIE, accessSetCookie);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshSetCookie);
        return ResponseEntity.ok(result);
    }

    @PostMapping("${endpoint.password.reset}")
    @Operation(summary = "Change user password", description = "Allows authenticated user to change their password")
    public ResponseEntity<IamResponse<String>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<String> result = authService.changePassword(request);
        return ResponseEntity.ok(result);
    }


    @GetMapping("${endpoint.logout}")
    @Operation(summary = "Logout", description = "Logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.deleteAccessCookie());
        response.addHeader(HttpHeaders.SET_COOKIE, CookieUtils.deleteRefreshCookie());

        return ResponseEntity.ok().build();
    }

}
