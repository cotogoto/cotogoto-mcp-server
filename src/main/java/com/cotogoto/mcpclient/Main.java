package com.cotogoto.mcpclient;

import com.cotogoto.mcpclient.server.McpServerProcess;
import com.cotogoto.mcpclient.tool.ToolRegistry;
import com.cotogoto.mcpclient.transport.StreamableHttpTransport;

public final class Main {
    public static void main(String[] args) {
        AppConfig config = AppConfig.fromArgs(args);
        StreamableHttpTransport transport = new StreamableHttpTransport(config.endpoint());
        ToolRegistry toolRegistry = ToolRegistry.defaultRegistry();

        McpServerProcess serverProcess = new McpServerProcess(transport, toolRegistry);
        serverProcess.run();
    }
}
