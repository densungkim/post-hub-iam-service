package com.post.hub.iamservice.utils;

import com.post.hub.iamservice.model.constants.ApiConstants;
import com.post.hub.iamservice.model.constants.ApiErrorMessage;
import com.post.hub.iamservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.StackWalker;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiUtils {
    private final JwtTokenProvider jwtTokenProvider;

    public static String getMethodName() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.skip(1)
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse(ApiConstants.UNDEFINED));
    }

    public static String generateUuidWithoutDash() {
        return UUID.randomUUID().toString().replace(ApiConstants.DASH, StringUtils.EMPTY);
    }

    public Integer getUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException(ApiErrorMessage.HAVE_NO_ACCESS.getMessage());
        }

        Object credentials = authentication.getCredentials();
        if (!(credentials instanceof String token) || StringUtils.isBlank(token)) {
            throw new AuthenticationCredentialsNotFoundException(ApiErrorMessage.HAVE_NO_ACCESS.getMessage());
        }

        return Integer.parseInt(jwtTokenProvider.getUserId(token));
    }
}
