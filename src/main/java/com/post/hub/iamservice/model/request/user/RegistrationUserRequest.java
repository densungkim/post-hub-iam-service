package com.post.hub.iamservice.model.request.user;

import com.post.hub.iamservice.utils.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@PasswordMatches
public class RegistrationUserRequest implements Serializable {

    @NotBlank(message = "Username cannot be empty")
    private String username;

    @Email
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    @NotBlank(message = "ConfirmPassword cannot be empty")
    private String confirmPassword;

}
