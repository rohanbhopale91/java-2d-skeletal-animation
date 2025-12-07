package com.animstudio.io;

/**
 * Format version history and migration support.
 * 
 * Version History:
 * - Version 1: Initial format (current)
 *   - Basic skeleton, animations, keyframes
 * 
 * - Version 2: (Future)
 *   - Added IK constraints
 *   - Added mesh deformation
 *   - Added asset library
 */
public class FormatVersion {
    
    public static final int CURRENT_VERSION = 1;
    
    // Minimum version we can read
    public static final int MIN_READABLE_VERSION = 1;
    
    // Maximum version we know how to read
    public static final int MAX_READABLE_VERSION = 1;
    
    /**
     * Check if a format version can be read by this version of the software.
     */
    public static boolean canRead(int version) {
        return version >= MIN_READABLE_VERSION && version <= MAX_READABLE_VERSION;
    }
    
    /**
     * Check if a format version needs migration to the current version.
     */
    public static boolean needsMigration(int version) {
        return version < CURRENT_VERSION;
    }
    
    /**
     * Get a description of what changed in a specific version.
     */
    public static String getVersionDescription(int version) {
        switch (version) {
            case 1:
                return "Initial format: skeleton, animations, keyframes";
            default:
                return "Unknown version";
        }
    }
    
    /**
     * Get migration notes when upgrading from one version to another.
     */
    public static String getMigrationNotes(int fromVersion, int toVersion) {
        if (fromVersion >= toVersion) {
            return "No migration needed";
        }
        
        StringBuilder notes = new StringBuilder();
        notes.append("Migrating from version ").append(fromVersion)
             .append(" to version ").append(toVersion).append(":\n");
        
        // Add notes for each version upgrade
        for (int v = fromVersion + 1; v <= toVersion; v++) {
            notes.append("- ").append(getVersionDescription(v)).append("\n");
        }
        
        return notes.toString();
    }
}
