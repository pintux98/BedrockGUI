package org.pintux.life.velocity.utils;

import it.pintux.life.common.utils.FormConfig;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.*;

public class VelocityConfig implements FormConfig {

    private final ConfigurationNode config;

    public VelocityConfig(ConfigurationNode config) {
        this.config = config;
    }

    @Override
    public String getString(String path, String defaultValue) {
        return "";
    }

    @Override
    public String getString(String path) {
        return config.node((Object[]) path.split("\\.")).getString();
    }

    @Override
    public List<String> getStringList(String path) {
        try {
            return config.node((Object[]) path.split("\\.")).getList(String.class, new ArrayList<>());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public Set<String> getKeys(String path) {
        return Set.of();
    }

    @Override
    public Map<String, Object> getValues(String path) {
        ConfigurationNode node = config.node((Object[]) path.split("\\."));
        Map<String, Object> values = new HashMap<>();
        
        if (node.isMap()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                String key = entry.getKey().toString();
                ConfigurationNode valueNode = entry.getValue();
                
                if (valueNode.isMap()) {
                    values.put(key, getValues(path + "." + key));
                } else if (valueNode.isList()) {
                    try {
                        values.put(key, valueNode.getList(String.class, new ArrayList<>()));
                    } catch (Exception e) {
                        values.put(key, valueNode.raw());
                    }
                } else {
                    values.put(key, valueNode.getString());
                }
            }
        }
        
        return values;
    }
}