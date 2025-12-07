package com.animstudio.automation;

import com.animstudio.core.model.Bone;
import com.animstudio.core.model.Skeleton;

import java.util.*;
import java.util.logging.Logger;

/**
 * Engine that manages and applies procedural animation rules.
 */
public class RuleEngine {
    
    private static final Logger LOGGER = Logger.getLogger(RuleEngine.class.getName());
    
    private final List<AnimationRule> rules;
    private final Map<String, AnimationRule> rulesByName;
    private boolean enabled = true;
    private double globalIntensity = 1.0;
    
    public RuleEngine() {
        rules = new ArrayList<>();
        rulesByName = new HashMap<>();
    }
    
    /**
     * Add a rule to the engine.
     */
    public void addRule(AnimationRule rule) {
        if (rule == null) return;
        rules.add(rule);
        rulesByName.put(rule.getName(), rule);
        LOGGER.fine("Added rule: " + rule.getName());
    }
    
    /**
     * Remove a rule from the engine.
     */
    public void removeRule(AnimationRule rule) {
        if (rule == null) return;
        rules.remove(rule);
        rulesByName.remove(rule.getName());
    }
    
    /**
     * Remove a rule by name.
     */
    public void removeRule(String name) {
        AnimationRule rule = rulesByName.remove(name);
        if (rule != null) {
            rules.remove(rule);
        }
    }
    
    /**
     * Get a rule by name.
     */
    public AnimationRule getRule(String name) {
        return rulesByName.get(name);
    }
    
    /**
     * Get all rules.
     */
    public List<AnimationRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
    
    /**
     * Clear all rules.
     */
    public void clearRules() {
        rules.clear();
        rulesByName.clear();
    }
    
    /**
     * Enable or disable the entire engine.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set global intensity multiplier for all rules.
     */
    public void setGlobalIntensity(double intensity) {
        this.globalIntensity = Math.max(0, Math.min(1, intensity));
    }
    
    public double getGlobalIntensity() {
        return globalIntensity;
    }
    
    /**
     * Update and apply all enabled rules to the skeleton.
     */
    public void update(Skeleton skeleton, double time, double deltaTime) {
        if (!enabled || skeleton == null) return;
        
        for (AnimationRule rule : rules) {
            if (rule.isEnabled()) {
                try {
                    rule.apply(skeleton, time, deltaTime);
                } catch (Exception e) {
                    LOGGER.warning("Rule '" + rule.getName() + "' failed: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Reset all rules to their initial state.
     */
    public void resetAll() {
        for (AnimationRule rule : rules) {
            rule.reset();
        }
    }
    
    /**
     * Get rules that affect a specific bone.
     */
    public List<AnimationRule> getRulesForBone(String boneName) {
        List<AnimationRule> result = new ArrayList<>();
        for (AnimationRule rule : rules) {
            if (boneName.equals(rule.getTargetBone()) || rule.getTargetBone() == null) {
                result.add(rule);
            }
        }
        return result;
    }
}
