package com.cotogoto.mcpclient.tool;

import java.util.HashMap;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, ToolHandler> handlers = new HashMap<>();

    public static ToolRegistry defaultRegistry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register("ping", params -> Map.of("message", "pong"));
        return registry;
    }

    public void register(String name, ToolHandler handler) {
        handlers.put(name, handler);
    }

    public ToolHandler get(String name) {
        return handlers.get(name);
    }
}
