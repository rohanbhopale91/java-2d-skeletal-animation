package com.animstudio.io.imports;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;
import com.animstudio.io.JsonDeserializer;
import com.animstudio.io.ProjectFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Imports skeleton data from various formats.
 * Supports JSON-based formats and Spine compatibility.
 */
public class SkeletonImporter {
    
    /**
     * Import skeleton from AnimStudio JSON format.
     */
    public Skeleton importFromJson(File file) throws IOException {
        JsonDeserializer deserializer = new JsonDeserializer();
        ProjectFile project = deserializer.deserializeFromFile(file);
        return project.getSkeleton();
    }
    
    /**
     * Import skeleton from simple JSON format.
     * Format: { "bones": [ { "name": "...", "parent": "...", "x": 0, "y": 0, "length": 50, "rotation": 0 } ] }
     */
    public Skeleton importSimpleJson(File file) throws IOException {
        String json = readFile(file);
        return parseSimpleFormat(json);
    }
    
    /**
     * Import skeleton from Spine JSON format (partial compatibility).
     */
    public Skeleton importSpineJson(File file) throws IOException {
        String json = readFile(file);
        return parseSpineFormat(json);
    }
    
    /**
     * Import skeleton from DragonBones JSON format (partial compatibility).
     */
    public Skeleton importDragonBonesJson(File file) throws IOException {
        String json = readFile(file);
        return parseDragonBonesFormat(json);
    }
    
    /**
     * Auto-detect format and import.
     */
    public Skeleton importAuto(File file) throws IOException {
        String json = readFile(file);
        
        // Try to detect format
        if (json.contains("\"skeleton\"") && json.contains("\"spine\"")) {
            return parseSpineFormat(json);
        } else if (json.contains("\"armature\"") || json.contains("\"dragonBones\"")) {
            return parseDragonBonesFormat(json);
        } else if (json.contains("\"meta\"") && json.contains("\"formatVersion\"")) {
            JsonDeserializer deserializer = new JsonDeserializer();
            ProjectFile project = deserializer.deserialize(json);
            return project.getSkeleton();
        } else {
            return parseSimpleFormat(json);
        }
    }
    
    private Skeleton parseSimpleFormat(String json) {
        // Simple hand-written parser for basic format
        Skeleton skeleton = new Skeleton("Imported");
        Map<String, Bone> boneMap = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        
        // Extract bones array
        int bonesStart = json.indexOf("\"bones\"");
        if (bonesStart < 0) return skeleton;
        
        int arrayStart = json.indexOf('[', bonesStart);
        int arrayEnd = findMatchingBracket(json, arrayStart);
        if (arrayStart < 0 || arrayEnd < 0) return skeleton;
        
        String bonesArray = json.substring(arrayStart + 1, arrayEnd);
        
        // Parse each bone object
        int pos = 0;
        while (pos < bonesArray.length()) {
            int objStart = bonesArray.indexOf('{', pos);
            if (objStart < 0) break;
            
            int objEnd = findMatchingBrace(bonesArray, objStart);
            if (objEnd < 0) break;
            
            String boneJson = bonesArray.substring(objStart, objEnd + 1);
            
            String name = extractString(boneJson, "name");
            String parent = extractString(boneJson, "parent");
            double x = extractNumber(boneJson, "x", 0);
            double y = extractNumber(boneJson, "y", 0);
            double length = extractNumber(boneJson, "length", 50);
            double rotation = extractNumber(boneJson, "rotation", 0);
            
            if (name != null) {
                Bone bone = new Bone(name);
                bone.setX(x);
                bone.setY(y);
                bone.setLength(length);
                bone.setRotation(Math.toRadians(rotation));
                
                boneMap.put(name, bone);
                if (parent != null) {
                    parentMap.put(name, parent);
                }
            }
            
            pos = objEnd + 1;
        }
        
        // Setup hierarchy
        for (Map.Entry<String, String> entry : parentMap.entrySet()) {
            Bone child = boneMap.get(entry.getKey());
            Bone parentBone = boneMap.get(entry.getValue());
            if (child != null && parentBone != null) {
                child.setParent(parentBone);
            }
        }
        
        // Add to skeleton (roots first)
        for (Bone bone : boneMap.values()) {
            if (bone.getParent() == null) {
                skeleton.addBone(bone);
            }
        }
        for (Bone bone : boneMap.values()) {
            if (bone.getParent() != null) {
                skeleton.addBone(bone);
            }
        }
        
        return skeleton;
    }
    
    private Skeleton parseSpineFormat(String json) {
        // Parse Spine-style JSON (simplified)
        Skeleton skeleton = new Skeleton("SpineImport");
        Map<String, Bone> boneMap = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        
        int bonesStart = json.indexOf("\"bones\"");
        if (bonesStart < 0) return skeleton;
        
        int arrayStart = json.indexOf('[', bonesStart);
        int arrayEnd = findMatchingBracket(json, arrayStart);
        if (arrayStart < 0 || arrayEnd < 0) return skeleton;
        
        String bonesArray = json.substring(arrayStart + 1, arrayEnd);
        
        int pos = 0;
        while (pos < bonesArray.length()) {
            int objStart = bonesArray.indexOf('{', pos);
            if (objStart < 0) break;
            
            int objEnd = findMatchingBrace(bonesArray, objStart);
            if (objEnd < 0) break;
            
            String boneJson = bonesArray.substring(objStart, objEnd + 1);
            
            // Spine format uses different field names
            String name = extractString(boneJson, "name");
            String parent = extractString(boneJson, "parent");
            double x = extractNumber(boneJson, "x", 0);
            double y = extractNumber(boneJson, "y", 0);
            double length = extractNumber(boneJson, "length", 0);
            double rotation = extractNumber(boneJson, "rotation", 0);
            double scaleX = extractNumber(boneJson, "scaleX", 1);
            double scaleY = extractNumber(boneJson, "scaleY", 1);
            
            if (name != null) {
                Bone bone = new Bone(name);
                bone.setX(x);
                bone.setY(y);
                bone.setLength(length);
                bone.setRotation(Math.toRadians(rotation));
                bone.setScaleX(scaleX);
                bone.setScaleY(scaleY);
                
                boneMap.put(name, bone);
                if (parent != null) {
                    parentMap.put(name, parent);
                }
            }
            
            pos = objEnd + 1;
        }
        
        // Setup hierarchy
        for (Map.Entry<String, String> entry : parentMap.entrySet()) {
            Bone child = boneMap.get(entry.getKey());
            Bone parentBone = boneMap.get(entry.getValue());
            if (child != null && parentBone != null) {
                child.setParent(parentBone);
            }
        }
        
        // Add to skeleton
        for (Bone bone : boneMap.values()) {
            if (bone.getParent() == null) {
                skeleton.addBone(bone);
            }
        }
        for (Bone bone : boneMap.values()) {
            if (bone.getParent() != null) {
                skeleton.addBone(bone);
            }
        }
        
        return skeleton;
    }
    
    private Skeleton parseDragonBonesFormat(String json) {
        // Parse DragonBones-style JSON (simplified)
        Skeleton skeleton = new Skeleton("DragonBonesImport");
        
        // Find armature section
        int armatureStart = json.indexOf("\"armature\"");
        if (armatureStart < 0) return skeleton;
        
        // Find bone array within armature
        int boneStart = json.indexOf("\"bone\"", armatureStart);
        if (boneStart < 0) return skeleton;
        
        int arrayStart = json.indexOf('[', boneStart);
        int arrayEnd = findMatchingBracket(json, arrayStart);
        if (arrayStart < 0 || arrayEnd < 0) return skeleton;
        
        String bonesArray = json.substring(arrayStart + 1, arrayEnd);
        
        Map<String, Bone> boneMap = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        
        int pos = 0;
        while (pos < bonesArray.length()) {
            int objStart = bonesArray.indexOf('{', pos);
            if (objStart < 0) break;
            
            int objEnd = findMatchingBrace(bonesArray, objStart);
            if (objEnd < 0) break;
            
            String boneJson = bonesArray.substring(objStart, objEnd + 1);
            
            String name = extractString(boneJson, "name");
            String parent = extractString(boneJson, "parent");
            double length = extractNumber(boneJson, "length", 50);
            
            // DragonBones uses transform object
            int transformStart = boneJson.indexOf("\"transform\"");
            double x = 0, y = 0, rotation = 0, scaleX = 1, scaleY = 1;
            
            if (transformStart >= 0) {
                int transObjStart = boneJson.indexOf('{', transformStart);
                int transObjEnd = findMatchingBrace(boneJson, transObjStart);
                if (transObjStart >= 0 && transObjEnd >= 0) {
                    String transJson = boneJson.substring(transObjStart, transObjEnd + 1);
                    x = extractNumber(transJson, "x", 0);
                    y = extractNumber(transJson, "y", 0);
                    rotation = extractNumber(transJson, "skX", 0); // DragonBones uses skX for rotation
                    scaleX = extractNumber(transJson, "scX", 1);
                    scaleY = extractNumber(transJson, "scY", 1);
                }
            }
            
            if (name != null) {
                Bone bone = new Bone(name);
                bone.setX(x);
                bone.setY(y);
                bone.setLength(length);
                bone.setRotation(Math.toRadians(rotation));
                bone.setScaleX(scaleX);
                bone.setScaleY(scaleY);
                
                boneMap.put(name, bone);
                if (parent != null) {
                    parentMap.put(name, parent);
                }
            }
            
            pos = objEnd + 1;
        }
        
        // Setup hierarchy
        for (Map.Entry<String, String> entry : parentMap.entrySet()) {
            Bone child = boneMap.get(entry.getKey());
            Bone parentBone = boneMap.get(entry.getValue());
            if (child != null && parentBone != null) {
                child.setParent(parentBone);
            }
        }
        
        // Add to skeleton
        for (Bone bone : boneMap.values()) {
            if (bone.getParent() == null) {
                skeleton.addBone(bone);
            }
        }
        for (Bone bone : boneMap.values()) {
            if (bone.getParent() != null) {
                skeleton.addBone(bone);
            }
        }
        
        return skeleton;
    }
    
    // Helper methods
    
    private String readFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
    
    private String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex < 0) return null;
        
        int colonIndex = json.indexOf(':', keyIndex + search.length());
        if (colonIndex < 0) return null;
        
        int quoteStart = json.indexOf('"', colonIndex + 1);
        if (quoteStart < 0) return null;
        
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return null;
        
        return json.substring(quoteStart + 1, quoteEnd);
    }
    
    private double extractNumber(String json, String key, double defaultValue) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex < 0) return defaultValue;
        
        int colonIndex = json.indexOf(':', keyIndex + search.length());
        if (colonIndex < 0) return defaultValue;
        
        // Skip whitespace
        int start = colonIndex + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        // Find end of number
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
                end++;
            } else {
                break;
            }
        }
        
        if (end > start) {
            try {
                return Double.parseDouble(json.substring(start, end));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private int findMatchingBracket(String str, int openIndex) {
        if (openIndex < 0 || str.charAt(openIndex) != '[') return -1;
        
        int depth = 1;
        for (int i = openIndex + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
    
    private int findMatchingBrace(String str, int openIndex) {
        if (openIndex < 0 || str.charAt(openIndex) != '{') return -1;
        
        int depth = 1;
        for (int i = openIndex + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
