package com.examplatform.auth.service;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoogleOidcUserService
        implements org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
                OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {
        OidcUser user = delegate.loadUser(userRequest);

        if (!StringUtils.hasText(user.getSubject())
                || !StringUtils.hasText(user.getClaimAsString("email"))
                || !Boolean.TRUE.equals(
                        user.getClaimAsBoolean("email_verified"))) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Google account must provide a verified email address");
        }

        return user;
    }
}
