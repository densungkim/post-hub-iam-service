package com.post.hub.iamservice.controller;

import com.post.hub.iamservice.config.SwaggerInnerKey;
import com.post.hub.iamservice.model.constants.ApiLogMessage;
import com.post.hub.iamservice.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/iamServiceInner")
@RequiredArgsConstructor
public class InnerController {

    @SwaggerInnerKey
    @GetMapping("/healthCheck")
    @Operation(summary = "HealthCheck for service")
    public ResponseEntity<Void> healthCheck() {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        return ResponseEntity.ok().build();
    }
}
