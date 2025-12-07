package com.animstudio.automation.script;

import com.animstudio.automation.AnimationRule;
import com.animstudio.automation.rules.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses JSON automation scripts into rules.
 * 
 * Script format example:
 * {
 *   "rules": [
 *     {
 *       "type": "breathing",
 *       "bone": "chest",
 *       "intensity": 1.0,
 *       "speed": 0.3
 *     },
 *     {
 *       "type": "blink",
 *       "bone": "leftEye",
 *       "interval": 3.0,
 *       "duration": 0.15
 *     }
 *   ]
 * }
 */
public class ScriptParser {
    
    /**
     * Parse rules from a JSON file.
     */
    public List<AnimationRule> parseFile(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return parse(reader);
        }
    }
    
    /**
     * Parse rules from a JSON string.
     */
    public List<AnimationRule> parseString(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return parseRules(root);
    }
    
    /**
     * Parse rules from a reader.
     */
    public List<AnimationRule> parse(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        return parseRules(root);
    }
    
    private List<AnimationRule> parseRules(JsonObject root) {
        List<AnimationRule> rules = new ArrayList<>();
        
        if (root.has("rules")) {
            JsonArray rulesArray = root.getAsJsonArray("rules");
            for (JsonElement elem : rulesArray) {
                AnimationRule rule = parseRule(elem.getAsJsonObject());
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }
        
        return rules;
    }
    
    private AnimationRule parseRule(JsonObject obj) {
        String type = obj.has("type") ? obj.get("type").getAsString() : "";
        
        switch (type.toLowerCase()) {
            case "breathing":
                return parseBreathingRule(obj);
            case "blink":
                return parseBlinkRule(obj);
            case "wiggle":
                return parseWiggleRule(obj);
            case "lookat":
                return parseLookAtRule(obj);
            case "walkcycle":
                return parseWalkCycleRule(obj);
            default:
                System.err.println("Unknown rule type: " + type);
                return null;
        }
    }
    
    private BreathingRule parseBreathingRule(JsonObject obj) {
        BreathingRule rule = new BreathingRule();
        
        if (obj.has("bone")) {
            rule.setTargetBone(obj.get("bone").getAsString());
        }
        if (obj.has("intensity")) {
            rule.setIntensity(obj.get("intensity").getAsDouble());
        }
        if (obj.has("breathRate")) {
            rule.setBreathRate(obj.get("breathRate").getAsDouble());
        }
        if (obj.has("scaleAmount")) {
            rule.setScaleAmount(obj.get("scaleAmount").getAsDouble());
        }
        if (obj.has("affectX")) {
            rule.setAffectX(obj.get("affectX").getAsBoolean());
        }
        if (obj.has("affectY")) {
            rule.setAffectY(obj.get("affectY").getAsBoolean());
        }
        if (obj.has("enabled")) {
            rule.setEnabled(obj.get("enabled").getAsBoolean());
        }
        
        return rule;
    }
    
    private BlinkRule parseBlinkRule(JsonObject obj) {
        BlinkRule rule = new BlinkRule();
        
        if (obj.has("bone")) {
            rule.setTargetBone(obj.get("bone").getAsString());
        }
        if (obj.has("intensity")) {
            rule.setIntensity(obj.get("intensity").getAsDouble());
        }
        if (obj.has("minInterval") && obj.has("maxInterval")) {
            rule.setBlinkInterval(
                obj.get("minInterval").getAsDouble(),
                obj.get("maxInterval").getAsDouble()
            );
        }
        if (obj.has("duration")) {
            rule.setBlinkDuration(obj.get("duration").getAsDouble());
        }
        if (obj.has("enabled")) {
            rule.setEnabled(obj.get("enabled").getAsBoolean());
        }
        
        return rule;
    }
    
    private WiggleRule parseWiggleRule(JsonObject obj) {
        WiggleRule rule = new WiggleRule();
        
        if (obj.has("bone")) {
            rule.setTargetBone(obj.get("bone").getAsString());
        }
        if (obj.has("intensity")) {
            rule.setIntensity(obj.get("intensity").getAsDouble());
        }
        if (obj.has("frequency")) {
            rule.setFrequency(obj.get("frequency").getAsDouble());
        }
        if (obj.has("rotationAmount")) {
            rule.setRotationAmount(obj.get("rotationAmount").getAsDouble());
        }
        if (obj.has("translationAmount")) {
            rule.setTranslationAmount(obj.get("translationAmount").getAsDouble());
        }
        if (obj.has("smoothNoise")) {
            rule.setSmoothNoise(obj.get("smoothNoise").getAsBoolean());
        }
        if (obj.has("enabled")) {
            rule.setEnabled(obj.get("enabled").getAsBoolean());
        }
        
        return rule;
    }
    
    private LookAtRule parseLookAtRule(JsonObject obj) {
        LookAtRule rule = new LookAtRule();
        
        if (obj.has("bone")) {
            rule.setTargetBone(obj.get("bone").getAsString());
        }
        if (obj.has("intensity")) {
            rule.setIntensity(obj.get("intensity").getAsDouble());
        }
        if (obj.has("targetX")) {
            rule.setTarget(obj.get("targetX").getAsDouble(), rule.getTargetY());
        }
        if (obj.has("targetY")) {
            rule.setTarget(rule.getTargetX(), obj.get("targetY").getAsDouble());
        }
        if (obj.has("maxRotation")) {
            rule.setMaxRotation(obj.get("maxRotation").getAsDouble());
        }
        if (obj.has("smoothing")) {
            rule.setSmoothing(obj.get("smoothing").getAsDouble());
        }
        if (obj.has("clampRotation")) {
            rule.setClampRotation(obj.get("clampRotation").getAsBoolean());
        }
        if (obj.has("enabled")) {
            rule.setEnabled(obj.get("enabled").getAsBoolean());
        }
        
        return rule;
    }
    
    private WalkCycleRule parseWalkCycleRule(JsonObject obj) {
        WalkCycleRule rule = new WalkCycleRule();
        
        if (obj.has("intensity")) {
            rule.setIntensity(obj.get("intensity").getAsDouble());
        }
        if (obj.has("cycleSpeed")) {
            rule.setCycleSpeed(obj.get("cycleSpeed").getAsDouble());
        }
        if (obj.has("thighSwing")) {
            rule.setThighSwing(obj.get("thighSwing").getAsDouble());
        }
        if (obj.has("shinBend")) {
            rule.setShinBend(obj.get("shinBend").getAsDouble());
        }
        if (obj.has("footRoll")) {
            rule.setFootRoll(obj.get("footRoll").getAsDouble());
        }
        if (obj.has("leftThigh") && obj.has("leftShin") && obj.has("leftFoot")) {
            rule.setLeftLegBones(
                obj.get("leftThigh").getAsString(),
                obj.get("leftShin").getAsString(),
                obj.get("leftFoot").getAsString()
            );
        }
        if (obj.has("rightThigh") && obj.has("rightShin") && obj.has("rightFoot")) {
            rule.setRightLegBones(
                obj.get("rightThigh").getAsString(),
                obj.get("rightShin").getAsString(),
                obj.get("rightFoot").getAsString()
            );
        }
        if (obj.has("enabled")) {
            rule.setEnabled(obj.get("enabled").getAsBoolean());
        }
        
        return rule;
    }
}
