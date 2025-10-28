package com.example.mockApiServer.exception;

/**
 * Custom exception for snapshot-related errors
 */
public class SnapshotException extends RuntimeException {
    
    public SnapshotException(String message) {
        super(message);
    }
    
    public SnapshotException(String message, Throwable cause) {
        super(message, cause);
    }
}
