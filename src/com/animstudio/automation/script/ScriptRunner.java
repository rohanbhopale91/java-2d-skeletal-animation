package com.animstudio.automation.script;

import com.animstudio.automation.AnimationRule;
import com.animstudio.automation.RuleEngine;
import com.animstudio.core.model.Skeleton;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads and runs automation scripts.
 * Integrates ScriptParser with RuleEngine.
 */
public class ScriptRunner {
    
    private final ScriptParser parser;
    private final RuleEngine engine;
    
    public ScriptRunner() {
        this.parser = new ScriptParser();
        this.engine = new RuleEngine();
    }
    
    public ScriptRunner(RuleEngine engine) {
        this.parser = new ScriptParser();
        this.engine = engine;
    }
    
    /**
     * Load rules from a script file and add to the engine.
     * @return Number of rules loaded
     */
    public int loadScript(Path scriptFile) throws IOException {
        List<AnimationRule> rules = parser.parseFile(scriptFile);
        for (AnimationRule rule : rules) {
            engine.addRule(rule);
        }
        return rules.size();
    }
    
    /**
     * Load rules from a JSON string and add to the engine.
     * @return Number of rules loaded
     */
    public int loadScriptString(String json) {
        List<AnimationRule> rules = parser.parseString(json);
        for (AnimationRule rule : rules) {
            engine.addRule(rule);
        }
        return rules.size();
    }
    
    /**
     * Update all rules.
     */
    public void update(Skeleton skeleton, double time, double deltaTime) {
        engine.update(skeleton, time, deltaTime);
    }
    
    /**
     * Clear all loaded rules.
     */
    public void clearRules() {
        engine.clearRules();
    }
    
    /**
     * Get the underlying rule engine.
     */
    public RuleEngine getEngine() {
        return engine;
    }
    
    /**
     * Get the parser for direct parsing access.
     */
    public ScriptParser getParser() {
        return parser;
    }
    
    /**
     * Enable/disable a rule by name.
     */
    public void setRuleEnabled(String ruleName, boolean enabled) {
        for (AnimationRule rule : engine.getRules()) {
            if (rule.getName().equals(ruleName)) {
                rule.setEnabled(enabled);
                return;
            }
        }
    }
    
    /**
     * Check if a rule is enabled.
     */
    public boolean isRuleEnabled(String ruleName) {
        for (AnimationRule rule : engine.getRules()) {
            if (rule.getName().equals(ruleName)) {
                return rule.isEnabled();
            }
        }
        return false;
    }
    
    /**
     * Get count of loaded rules.
     */
    public int getRuleCount() {
        return engine.getRules().size();
    }
}
