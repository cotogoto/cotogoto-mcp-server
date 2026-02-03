package com.cotogoto.mcpclient.server;

import com.cotogoto.mcpclient.protocol.JsonRpcCodec;
import com.cotogoto.mcpclient.protocol.JsonRpcRequest;
import com.cotogoto.mcpclient.protocol.JsonRpcResponse;
import com.cotogoto.mcpclient.tool.ToolHandler;
import com.cotogoto.mcpclient.tool.ToolRegistry;
import com.cotogoto.mcpclient.transport.StreamableHttpTransport;

import java.util.Map;

public class McpServerProcess {
    private final StreamableHttpTransport transport;
    private final ToolRegistry toolRegistry;
    private final JsonRpcCodec codec = new JsonRpcCodec();

    public McpServerProcess(StreamableHttpTransport transport, ToolRegistry toolRegistry) {
        this.transport = transport;
        this.toolRegistry = toolRegistry;
    }

    public void run() {
        String sessionId = transport.startSession();
        transport.streamRequests(sessionId, message -> handleMessage(sessionId, message));
    }

    private void handleMessage(String sessionId, String json) {
        JsonRpcRequest request = codec.readRequest(json);
        if (request.id() == null) {
            return;
        }

        JsonRpcResponse response = dispatch(request);
        transport.sendResponse(sessionId, codec.write(response));
    }

    private JsonRpcResponse dispatch(JsonRpcRequest request) {
        String method = request.method();
        String toolName = null;
        if (method != null && method.startsWith("tool/")) {
            toolName = method.substring("tool/".length());
        } else if ("tools/call".equals(method) && request.params() instanceof Map<?, ?> params) {
            Object name = params.get("name");
            if (name instanceof String) {
                toolName = (String) name;
            }
        }

        if (toolName == null) {
            return JsonRpcResponse.error(request.id(), -32601, "Method not found");
        }

        ToolHandler handler = toolRegistry.get(toolName);
        if (handler == null) {
            return JsonRpcResponse.error(request.id(), -32601, "Tool not found: " + toolName);
        }

        Object result = handler.handle(request.params());
        return JsonRpcResponse.success(request.id(), result);
    }
}
