# cotogoto-mcp-server

## MCP 会話 API（SSE中継）

このサーバーは、`POST /api/mcp/conversations` で受け取った会話リクエストを
上流の cotogoto SSE API に転送し、受信した SSE イベントをそのままクライアントへ中継します。

- ローカル受け口: `POST /api/mcp/conversations`
- 上流転送先（既定値）: `https://app.cotogoto.ai/webapi/api/mcp/conversations`

---

## `mcp.json` について

このリポジトリは MCP クライアント設定（`mcp.json`）のフォーマットを定義していません。
`mcp.json` の書式は利用する MCP クライアントごとに異なるため、
クライアントの公式ドキュメントに従って設定してください。

本サーバーが提供するインターフェースは HTTP(S) の
`POST /api/mcp/conversations` で、SSE をそのまま中継する構成です。
`mcp.json` 側では、この HTTP エンドポイントを呼び出せるクライアントや
プロキシ/ブリッジの設定が必要になります。

### LM Studio について

LM Studio の `mcp.json` 書式は LM Studio 側で定義されます。
LM Studio の MCP 設定手順は公式ドキュメントを参照してください。
https://lmstudio.ai/docs/app/mcp

LM Studio が HTTP への直接接続に対応している場合は、
このサーバーの `POST /api/mcp/conversations` を呼び出す設定を行ってください。
もし LM Studio が MCP の stdio 方式のみ対応している場合は、
stdio を HTTP へ中継するブリッジを用意する必要があります。

---

## 設定

`src/main/resources/application.yaml` で上流URLを設定します。

```yaml
cotogoto:
  upstream:
    conversations-url: https://app.cotogoto.ai/webapi/api/mcp/conversations
```

必要に応じて環境ごとに `cotogoto.upstream.conversations-url` を上書きしてください。

---

## ビルドと起動

```bash
./mvnw package
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar
```

`spring-boot-maven-plugin` を `executable` モードで設定しているため、
ビルド後は以下のように直接実行も可能です。

```bash
./target/mcp-server-0.0.1-SNAPSHOT.jar
```

### 事前に必要なもの

- Java 17 以上
- cotogoto の API トークン（`apiToken`）

### パラメータの渡し方

Spring Boot の標準的な引数や環境変数で設定値を渡せます。

#### 引数で渡す（`java -jar` / 直接実行どちらも同じ）

```bash
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar \
  --server.port=8081 \
  --cotogoto.upstream.conversations-url=https://app.cotogoto.ai/webapi/api/mcp/conversations
```

#### 環境変数で渡す

```bash
export SERVER_PORT=8081
export COTOGOTO_UPSTREAM_CONVERSATIONS_URL=https://app.cotogoto.ai/webapi/api/mcp/conversations
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar
```

#### JVM オプションを渡す（メモリなど）

```bash
java -Xms256m -Xmx512m -jar target/mcp-server-0.0.1-SNAPSHOT.jar
```

### 動作確認（ローカルでの一連の手順）

1. ビルド

```bash
./mvnw package
```

2. 起動（必要ならポートや上流URLを変更）

```bash
java -jar target/mcp-server-0.0.1-SNAPSHOT.jar \
  --server.port=8081 \
  --cotogoto.upstream.conversations-url=https://app.cotogoto.ai/webapi/api/mcp/conversations
```

3. 会話リクエストを送信（`apiToken` は実際の値に置き換えてください）

```bash
curl -N -X POST http://localhost:8081/api/mcp/conversations \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{
    "sessionId": "SESSION_ID",
    "apiToken": "API_TOKEN",
    "entry": {
      "turnId": "turn-1",
      "role": "user",
      "content": "おはようございます"
    }
  }'
```

---

## エンドポイント

### `POST /api/mcp/conversations`

#### リクエスト例
```json
{
  "sessionId": "SESSION_ID",
  "apiToken": "API_TOKEN",
  "entry": {
    "turnId": "turn-1",
    "role": "user",
    "content": "おはようございます"
  }
}
```

#### 入力項目
- `sessionId`（必須）
- `apiToken`（必須・空文字不可）
- `entry`（必須）
  - `turnId`（必須）
  - `role`（必須）
  - `content`（必須）

#### レスポンス（SSE）
- 成功時: 上流から受け取った `event:` / `data:` を中継
- 上流がHTTPエラー時: `conversation.error` イベントでエラーボディを返却

例:
```text
event: conversation.accepted
data: {"accepted":true,"storedTurnIds":["turn-1"],"commandResponse":"..."}
```

#### エラー
- `apiToken` 未指定または空: `400 Bad Request`
- `entry` 未指定: `400 Bad Request`

---

## 実装概要

- `ConversationController#acceptConversation`
  - 入力検証後に `SseEmitter` を生成
  - `ConversationRelayService` に中継処理を委譲
- `ConversationRelayService`
  - 上流URLへ `POST`（`Accept: text/event-stream`）
  - SSE行を読み取り、イベント名とデータをローカル emitter に転送
