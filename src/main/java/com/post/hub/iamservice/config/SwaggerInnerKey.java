package com.post.hub.iamservice.config;

import com.post.hub.iamservice.service.model.AuthenticationConstants;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Parameter(
        in = ParameterIn.HEADER,
        description = "Inner key header",
        name = AuthenticationConstants.ACCESS_KEY_HEADER_NAME,
        schema = @Schema(
                type = "String",
                defaultValue = "0"
        )
)
public @interface SwaggerInnerKey {
}

