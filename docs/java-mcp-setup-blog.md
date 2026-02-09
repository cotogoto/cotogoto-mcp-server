# JavaでMCPサーバーを作る手順（cotogoto-mcp-server をベースにした実践メモ）

このリポジトリは Spring Boot + Spring AI の MCP サーバーとして動作し、
MCP JSON-RPC でツールを公開して cotogoto の SSE API に同期的にリクエストを送る構成になっています。
本記事では、このプロジェクトをベースに **Java で MCP サーバーを立ち上げるまでの手順** を整理します。

> 対象: Java 17 以上 / Spring Boot 4 / Spring AI MCP

---

## 1. ざっくり構成を把握する

このプロジェクトは、以下の点が特徴です。

- **MCP JSON-RPC (`/mcp`) を公開**し、`listTools` / `callTool` を受け付けます。`/mcp` は Spring AI の MCP サーバーが提供します。【F:README.md†L137-L172】
- **上流 API に対して SSE で会話リクエストを送る**構成で、上流 URL は `application.yaml` で指定します。【F:README.md†L73-L87】【F:src/main/resources/application.yaml†L9-L13】
- **Spring AI MCP Server WebMVC 依存**を利用しています。`spring-ai-starter-mcp-server-webmvc` が入っていることを確認します。【F:pom.xml†L25-L35】

---

## 2. 前提条件をそろえる

README に沿って、最低限以下が必要です。

- **Java 17 以上**【F:README.md†L105-L108】【F:pom.xml†L20-L21】
- **cotogoto の API トークン**（MCP クライアントの環境変数で渡します）【F:README.md†L105-L109】

---

## 3. 設定ファイルを確認する

`src/main/resources/application.yaml` を確認し、上流 URL とトークンの受け渡しを把握します。

```yaml
cotogoto:
  upstream:
    conversations-url: https://app.cotogoto.ai/webapi/api/mcp/conversations
    api-token: ${COTOGOTO_API_KEY}
```

- **上流 URL**は `cotogoto.upstream.conversations-url` で管理されています。【F:src/main/resources/application.yaml†L9-L13】
- **API トークン**は環境変数 `COTOGOTO_API_KEY` から読み込む設計です。【F:src/main/resources/application.yaml†L9-L13】
- `spring.ai.mcp.server.type: SYNC` かつ `stdio: true` なので、**MCP 標準 I/O で同期動作**する前提です。【F:src/main/resources/application.yaml†L1-L8】

---

## 4. ビルドして起動する

README どおり、Maven wrapper でビルドします。

```bash
./mvnw package
java -jar target/cotogoto-mcp-server.jar
```

`spring-boot-maven-plugin` が `executable` モードで設定されているため、
ビルド後は以下のように直接実行できます。【F:README.md†L91-L103】

```bash
./target/cotogoto-mcp-server.jar
```

---

## 5. 使うポート・上流 URL を切り替える

ポートや上流 URL は **引数** か **環境変数** で上書きできます。

### 引数で渡す

```bash
java -jar target/cotogoto-mcp-server.jar \
  --server.port=8081 \
  --cotogoto.upstream.conversations-url=https://app.cotogoto.ai/webapi/api/mcp/conversations
```

### 環境変数で渡す

```bash
export SERVER_PORT=8081
export COTOGOTO_UPSTREAM_CONVERSATIONS_URL=https://app.cotogoto.ai/webapi/api/mcp/conversations
export COTOGOTO_API_KEY=your-api-token
java -jar target/cotogoto-mcp-server.jar
```

いずれも README の手順どおりです。【F:README.md†L105-L129】

---

## 6. MCP エンドポイントを叩いて疎通確認

`/mcp` で `listTools` / `callTool` を投げることができます。

### listTools

```bash
curl -s http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "listTools"
  }'
```

### callTool

```bash
curl -s http://localhost:8081/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "callTool",
    "params": {
      "name": "cotogoto_conversation",
      "arguments": {
        "message": "おはようございます"
      }
    }
  }'
```

上記は README に掲載されているサンプルです。【F:README.md†L137-L172】

---

## 7. MCP クライアント側の設定（mcp.json の考え方）

このリポジトリは **`mcp.json` のフォーマットを定義しません**。
クライアントの仕様に従って MCP サーバーへの接続を設定します。

ポイントは以下です。

- `mcp.json` は「どの MCP サーバーに接続するか」を定義するものです。【F:README.md†L11-L25】
- `ConversationRequest` の内容は実行時に送るので、**`mcp.json` に直接書かない**設計です。【F:README.md†L20-L25】
- 上流 API 用の `apiToken` は **環境変数**で渡します。【F:README.md†L27-L28】

### stdio / HTTP の違い（はまりポイント）

このプロジェクトは `application.yaml` で **stdio 接続**を有効にしています。【F:src/main/resources/application.yaml†L1-L8】
そのため **MCP クライアントが stdio で起動する構成**の場合、HTTP の `curl` 例は使いません。

- **stdio 接続**: MCP クライアントが `java -jar ...` を起動し、標準入出力で `listTools` / `callTool` をやり取りする
- **HTTP 接続**: MCP クライアント（またはブリッジ）が `http://localhost:8081/mcp` に JSON-RPC を投げる

stdio で起動しているのに HTTP の `curl` を試すと詰まりやすいので、
**どちらで接続しているか**を先に確認してください。

---

## 8. LM Studio で使う場合の注意点

README の補足によると、LM Studio の MCP 設定は LM Studio 側の仕様です。
HTTP MCP に接続できない場合は、以下を確認します。【F:README.md†L184-L212】

- サーバーが起動しているか
- ポートが一致しているか
- 8081 を他のプロセスが使っていないか
- ファイアウォール等がローカルホスト接続をブロックしていないか

---

## まとめ

このプロジェクトは **Spring AI MCP Server WebMVC** を使って MCP JSON-RPC を公開し、
cotogoto の SSE API へ会話リクエストを中継する最小構成です。

- **設定は `application.yaml`** で完結【F:src/main/resources/application.yaml†L1-L13】
- **ビルド・起動手順は README に沿うだけ**【F:README.md†L91-L135】
- **MCP エンドポイントは `/mcp`** で `listTools` / `callTool` が使える【F:README.md†L137-L172】

この流れを押さえれば、Java で MCP サーバーを立ち上げる最短ルートになります。

---

必要であれば、次のステップとして「独自ツールの追加」や「認証フローの拡張」などもまとめられます。
