package com.post.hub.iamservice.security.validation;

import com.post.hub.iamservice.model.request.user.ChangePasswordRequest;
import com.post.hub.iamservice.model.request.user.RegistrationUserRequest;
import com.post.hub.iamservice.utils.PasswordMatches;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj instanceof RegistrationUserRequest request) {
            return request.getPassword().equals(request.getConfirmPassword());
        }
        if (obj instanceof ChangePasswordRequest request) {
            return request.getPassword().equals(request.getConfirmPassword());
        }
        return false;
    }
}
