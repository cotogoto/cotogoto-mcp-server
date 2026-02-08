# cotogoto-mcp-server

## MCP 会話 API

このサーバーは、MCP JSON-RPC でツールを公開し、
cotogoto の SSE API に対して会話リクエストを同期的に送信します。

---

## `mcp.json` について

このリポジトリは MCP クライアント設定（`mcp.json`）のフォーマットを定義していません。
`mcp.json` の書式は利用する MCP クライアントごとに異なるため、
クライアントの公式ドキュメントに従って設定してください。

本サーバーが提供するインターフェースは MCP JSON-RPC です。
`mcp.json` 側では、この MCP サーバーに接続する設定を行ってください。

### `mcp.json` と ConversationRequest の関係

`mcp.json` は通常「どのサーバーに接続するか」を設定するためのファイルで、
`ConversationRequest` のような会話リクエスト本文はクライアントの実行時に送信します。
そのため、`ConversationRequest` の各フィールド（`sessionId` / `apiToken` / `entry` など）は
`mcp.json` に直接書くのではなく、クライアントがリクエストを送るタイミングで渡します。

一方で、本サーバー自体が上流 API を呼び出すための `apiToken` は、
MCP クライアントの設定（`mcp.json` など）で環境変数として渡してください。

### LM Studio について

LM Studio の `mcp.json` 書式は LM Studio 側で定義されます。
LM Studio の MCP 設定手順は公式ドキュメントを参照してください。
https://lmstudio.ai/docs/app/mcp

LM Studio の MCP 設定画面で本サーバーへ接続する設定を行ってください。
MCP の stdio 方式のみ対応している場合は、stdio を HTTP へ中継するブリッジを用意する必要があります。

---

## 設定

`src/main/resources/application.yaml` で上流URLを設定します。

```yaml
cotogoto:
  upstream:
    conversations-url: https://app.cotogoto.ai/webapi/api/mcp/conversations
```

必要に応じて環境ごとに `cotogoto.upstream.conversations-url` を上書きしてください。
`apiToken` は `mcp.json` から環境変数で渡す想定です。

---

## ビルドと起動

```bash
./mvnw package
java -jar target/cotogoto-mcp-server.jar
```

`spring-boot-maven-plugin` を `executable` モードで設定しているため、
ビルド後は以下のように直接実行も可能です。

```bash
./target/cotogoto-mcp-server.jar
```

### 事前に必要なもの

- Java 17 以上
- cotogoto の API トークン（`apiToken`）

### パラメータの渡し方

Spring Boot の標準的な引数や環境変数で設定値を渡せます。

#### 引数で渡す（`java -jar` / 直接実行どちらも同じ）

```bash
java -jar target/cotogoto-mcp-server.jar \
  --server.port=8081 \
  --cotogoto.upstream.conversations-url=https://app.cotogoto.ai/webapi/api/mcp/conversations
```

#### 環境変数で渡す

```bash
export SERVER_PORT=8081
export COTOGOTO_UPSTREAM_CONVERSATIONS_URL=https://app.cotogoto.ai/webapi/api/mcp/conversations
export COTOGOTO_UPSTREAM_API_TOKEN=your-api-token
java -jar target/cotogoto-mcp-server.jar
```

#### JVM オプションを渡す（メモリなど）

```bash
java -Xms256m -Xmx512m -jar target/cotogoto-mcp-server.jar
```

## エンドポイント

### MCP JSON-RPC (`/mcp`)

Spring AI の MCP サーバーは JSON-RPC 2.0 で `/mcp` を公開します。

#### listTools
```bash
curl -s http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "listTools"
  }'
```

#### callTool
```bash
curl -s http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "callTool",
    "params": {
      "name": "cotogotoConversation",
      "arguments": {
        "message": "おはようございます"
      }
    }
  }'
```

## LM Studio での接続トラブルシュート

### `ECONNREFUSED 127.0.0.1:8081` が出る場合

LM Studio の MCP ブリッジが `localhost:8081` に接続できていない状態です。
次の点を確認してください。

1. **サーバーが起動しているか**
   - `java -jar target/cotogoto-mcp-server.jar --server.port=8081` を実行しているか
2. **ポートが一致しているか**
   - LM Studio の設定が `http://localhost:8081/mcp` を向いているか
3. **別プロセスが 8081 を使用していないか**
   - 既に利用されている場合は `--server.port` を変更してください
4. **ファイアウォール/セキュリティソフト**
   - ローカルホストへの接続をブロックしていないか

### 接続確認（HTTP MCP）

以下の `listTools` が返れば、HTTP MCP は正常に稼働しています。

```bash
curl -s http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "listTools"
  }'
```

## 実装概要

- `ConversationRelayService`
  - 上流URLへ `POST`（`Accept: text/event-stream`）
  - SSE行を読み取り、イベント名とデータを集約して返却
