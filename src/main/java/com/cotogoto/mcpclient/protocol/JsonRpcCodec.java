package com.cotogoto.mcpclient.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonRpcCodec {
    private final ObjectMapper mapper = new ObjectMapper();

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize JSON-RPC message", e);
        }
    }

    public JsonRpcRequest readRequest(String json) {
        try {
            return mapper.readValue(json, JsonRpcRequest.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON-RPC request", e);
        }
    }
}
