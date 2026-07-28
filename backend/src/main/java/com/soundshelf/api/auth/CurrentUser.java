package com.soundshelf.api.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Single place where a request turns into a user id. Controllers never accept a
 * user id from the client, so there is no path to another user's rows.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long idOf(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalStateException("No authenticated principal on the request");
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Token subject is not a user id", ex);
        }
    }
}
