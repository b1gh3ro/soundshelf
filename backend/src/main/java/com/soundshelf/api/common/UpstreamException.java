package com.soundshelf.api.common;

/** Raised when the iTunes catalog is unreachable, slow, or returns something unusable. */
public class UpstreamException extends RuntimeException {
    public UpstreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpstreamException(String message) {
        super(message);
    }
}
