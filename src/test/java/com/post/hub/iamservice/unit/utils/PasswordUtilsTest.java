package com.post.hub.iamservice.unit.utils;

import com.post.hub.iamservice.model.constants.ApiConstants;
import com.post.hub.iamservice.utils.PasswordUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("unit")
class PasswordUtilsTest {

    @Test
    void isNotValid_shouldReturnFalse_forClearlyValidPassword() {
        String pwd = "Strong/Pass9";
        Assertions.assertFalse(PasswordUtils.isNotValidPassword(pwd), "Valid password must return FALSE");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void isNotValid_shouldReturnTrue_forNullOrBlank(String pwd) {
        Assertions.assertTrue(PasswordUtils.isNotValidPassword(pwd), "Null/blank must be TRUE");
    }

    @Test
    void isNotValid_shouldReturnTrue_whenShorterThanRequired() {
        String shortPwd = "Aa1/a";
        Assertions.assertTrue(PasswordUtils.isNotValidPassword(shortPwd), "Shorter than min length must be TRUE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"lowercase1/ok", "password9/xyz", "abcde123/abc"})
    void isNotValid_true_whenNoUppercase(String pwd) {
        Assertions.assertTrue(pwd.length() >= ApiConstants.REQUIRED_MIN_PASSWORD_LENGTH,
                "Sample must satisfy minimal length for composition test");
        Assertions.assertTrue(PasswordUtils.isNotValidPassword(pwd),
                "Missing uppercase must be TRUE: " + pwd);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPPERCASE1/OK", "ABCDEF9/ABC", "NOLOWER1/ZZZ"})
    void isNotValid_true_whenNoLowercase(String pwd) {
        Assertions.assertTrue(pwd.length() >= ApiConstants.REQUIRED_MIN_PASSWORD_LENGTH,
                "Sample must satisfy minimal length for composition test");
        Assertions.assertTrue(PasswordUtils.isNotValidPassword(pwd),
                "Missing lowercase must be TRUE: " + pwd);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NoDigits/AAa", "Password/AAA", "Abcdefg/XYz"})
    void isNotValid_true_whenNoDigit(String pwd) {
        Assertions.assertTrue(pwd.length() >= ApiConstants.REQUIRED_MIN_PASSWORD_LENGTH,
                "Sample must satisfy minimal length for composition test");
        Assertions.assertTrue(PasswordUtils.isNotValidPassword(pwd),
                "Missing digit must be TRUE: " + pwd);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NoSpecial1Aaa", "Abcdefg1AA", "PASSword1B"})
    void isNotValid_true_whenNoSpecial(String pwd) {
        Assertions.assertTrue(pwd.length() >= ApiConstants.REQUIRED_MIN_PASSWORD_LENGTH,
                "Sample must satisfy minimal length for composition test");
        Assertions.assertTrue(PasswordUtils.isNotValidPassword(pwd),
                "Missing special character must be TRUE: " + pwd);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abcdef1 /xyz", "OkPass9\t/AA",})
    void isNotValid_true_whenContainsForbiddenCharacters(String pwd) {
        Assertions.assertTrue(pwd.trim().length() >= ApiConstants.REQUIRED_MIN_PASSWORD_LENGTH,
                "Trimmed sample must satisfy minimal length");
        Assertions.assertTrue(PasswordUtils.isNotValidPassword(pwd),
                "Forbidden characters must be TRUE: " + pwd);
    }

    @Test
    void isNotValid_shouldReturnFalse_whenValidWithLeadingTrailingSpaces() {
        String core = "Aa1/validPASS";
        String padded = "   " + core + "   ";
        Assertions.assertFalse(PasswordUtils.isNotValidPassword(padded),
                "Leading/trailing spaces are trimmed; valid core must be FALSE");
    }
}
