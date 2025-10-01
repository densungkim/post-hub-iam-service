package com.post.hub.iamservice.model.request.user;

import com.post.hub.iamservice.utils.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@PasswordMatches
public class ChangePasswordRequest {

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "Confirm password cannot be blank")
    private String confirmPassword;
}
