package com.cotogoto.mcpclient;

import java.net.URI;
import java.util.Locale;

public record AppConfig(URI endpoint) {
    public static AppConfig fromArgs(String[] args) {
        String endpoint = System.getenv().getOrDefault("COTOGOTO_MCP_ENDPOINT", "http://localhost:8080/mcp");
        for (String arg : args) {
            String normalized = arg.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("--endpoint=")) {
                endpoint = arg.substring("--endpoint=".length());
            }
        }
        return new AppConfig(URI.create(endpoint));
    }
}
