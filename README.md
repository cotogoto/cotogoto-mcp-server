# cotogoto-mcp-client

## MCP利用ガイド（API利用手順）

### 1. エンドポイント一覧
- `POST /api/mcp/conversations`

OpenAPI 定義:
- `docs/openapi/domain-api.yaml`

---

### 2. 会話ログ（`/api/mcp/conversations`）

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

#### 主な入力項目
- `sessionId`（必須）: 会話セッションID
- `apiToken`（必須）: 会話に紐づくユーザーのAPIトークン
- `entry`（必須）: 会話ログ（1件のみ）
  - `turnId`（必須）: ターン識別子
  - `role`（必須）: `user / assistant / system / tool`
  - `content`（必須）: 発話内容

#### レスポンス例（Server-Sent Events）
```text
event: conversation.accepted
data: {"accepted":true,"storedTurnIds":["turn-1"],"commandResponse":"コマンド応答（該当時のみ）"}
```

---

### 3. 実装上の挙動メモ
- `entries` は `role=user` の発話だけを集約して処理します。
- `jp.livlog.cotogoto.helper.noby.AI` を使って解析し、コマンドが検出された場合は `CommandService` に完全委譲します。

---

### 4. 注意点
- `apiToken` が存在しない場合はエラーになります。
- `entry` が存在しない場合はエラーになります。

---

### 5. Apache⇔Tomcat 構成での注意点（SSE）
SSE は接続を長時間維持するため、Apache のプロキシ設定でバッファリング無効化とタイムアウト調整が必要です。
以下は代表的な例です（環境に合わせて調整してください）。

```apache
# mod_proxy / mod_proxy_http を前提
ProxyPass "/api/" "http://127.0.0.1:8080/api/" flushpackets=on
ProxyPassReverse "/api/" "http://127.0.0.1:8080/api/"

# SSE は長時間接続を維持するためタイムアウトを長めに
ProxyTimeout 300

# 必要に応じて KeepAlive を有効化
KeepAlive On
```

Tomcat 側は Spring MVC の `SseEmitter` で非同期処理として動くため、サーブレットコンテナの非同期が有効であることを前提にします。
