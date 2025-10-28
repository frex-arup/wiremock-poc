package com.example.mockApiServer.service.storage;

import java.io.IOException;
import java.util.List;

/**
 * Interface for snapshot storage backends
 */
public interface StorageService {
    
    /**
     * Save a snapshot with the given name
     * @param name The name/identifier for the snapshot
     * @param data The snapshot data as byte array
     * @throws IOException if save operation fails
     */
    void saveSnapshot(String name, byte[] data) throws IOException;
    
    /**
     * Load a snapshot by name
     * @param name The name/identifier of the snapshot
     * @return The snapshot data as byte array
     * @throws IOException if load operation fails
     */
    byte[] loadSnapshot(String name) throws IOException;
    
    /**
     * List all available snapshots
     * @return List of snapshot names
     * @throws IOException if listing fails
     */
    List<String> listSnapshots() throws IOException;
    
    /**
     * Delete a snapshot by name
     * @param name The name/identifier of the snapshot to delete
     * @return true if deletion was successful, false otherwise
     * @throws IOException if delete operation fails
     */
    boolean deleteSnapshot(String name) throws IOException;
    
    /**
     * Check if a snapshot exists
     * @param name The name/identifier of the snapshot
     * @return true if snapshot exists, false otherwise
     */
    boolean snapshotExists(String name) throws IOException;
}
