# cotogoto-mcp-server

## MCP 会話 API（SSE中継）

このサーバーは、`POST /api/mcp/conversations` で受け取った会話リクエストを
上流の cotogoto SSE API に転送し、受信した SSE イベントをそのままクライアントへ中継します。

- ローカル受け口: `POST /api/mcp/conversations`
- 上流転送先（既定値）: `https://app.cotogoto.ai/webapi/api/mcp/conversations`

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
