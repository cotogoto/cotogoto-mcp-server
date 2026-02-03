package com.cotogoto.mcpclient.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcRequest(String jsonrpc, String id, String method, Object params) {
    public static JsonRpcRequest request(String id, String method, Object params) {
        return new JsonRpcRequest("2.0", id, method, params);
    }
}
