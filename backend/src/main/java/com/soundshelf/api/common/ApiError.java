package com.soundshelf.api.common;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * The single error shape returned by every endpoint. {@code fieldErrors} is only
 * populated for validation failures and is omitted from the JSON otherwise.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, null);
    }

    public static ApiError validation(String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(OffsetDateTime.now(), 400, "Validation Failed", message, path, fieldErrors);
    }
}
