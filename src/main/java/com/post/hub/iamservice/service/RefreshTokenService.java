package com.post.hub.iamservice.service;

import com.post.hub.iamservice.model.entities.RefreshToken;
import com.post.hub.iamservice.model.entities.User;

public interface RefreshTokenService {

    RefreshToken generateOrUpdateRefreshToken(User user);

    RefreshToken validateAndRefreshToken(String refreshToken);

}
