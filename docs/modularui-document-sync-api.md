# ModularUI 动态文档同步 API

本文描述 ModularUI 当前已经实现的动态文档同步协议。它服务于 Java/XML/可选 JS 创建的客户端 UI，
不负责同步 DOM，也不改变传统 MUI 的固定 handler 和真实容器槽位拓扑。

## 1. 所有权与数据流

~~~text
server endpoint（唯一业务真相源）
  -> snapshot / PATCH / RESET
  -> 固定 DocumentSyncHandler
  -> client RemoteStore + MuiStore
  -> Java/XML binding
  -> DOM/Widget projection

DOM event / Java controller / JS controller
  -> 显式 RemoteStore.command(...)
  -> COMMAND
  -> server endpoint 再做权限、revision 和业务校验
  -> RESULT 或 ERROR
~~~

- DOM 和 Widget tree 只存在于客户端，不进入同步包。
- 每个 synced panel 在 build 阶段注册一个 `DocumentSyncHandler`；运行时只改变其内部 logical channel。
- 客户端不能注册 endpoint、分配 channel id 或直接写服务端状态。
- 服务端不加载 XML DOM 或 JavaScript runtime，因此仍可运行在 Java 8 dedicated server。
- DOM 节点移动不改变 store/channel；删除 binding 只减少 subscription refcount。

## 2. 注册固定 handler

client 和 server 的 panel builder 必须使用相同 key/id。`documentChannel` 固定使用 numeric id `0`：

~~~java
DocumentSyncLimits limits = DocumentSyncLimits.DEFAULT;
DocumentSyncHandler documentSync = syncManager.documentChannel(
        "mui_document", serverEndpointCatalog, limits);
~~~

客户端允许 `serverEndpointCatalog` 为 `null`；服务端必须提供 catalog。helper 最终仍调用普通
`getOrCreateSyncHandler`，所以只能在 panel 注册尚未锁定时调用，也没有动态注册特权。

## 3. Endpoint catalog

~~~java
DocumentEndpointCatalog catalog = new DocumentEndpointCatalog();
catalog.register(DocumentEndpointDescriptor
        .builder("rtsbuilding:storage", 1, storageEndpoint)
        .permission(context -> canOpen(context.getPlayer()))
        .parameters(params -> validateStorageParams(params))
        .build());
catalog.freeze();
~~~

catalog 的 key 全局稳定，schema version 是 endpoint wire contract 的一部分。重复 key 失败；冻结后禁止继续
注册。收到 `SUBSCRIBE` 时，MUI 按以下顺序执行：

1. 检查 session、request id 和 channel 配额；
2. 查找 endpoint 并检查 schema；
3. 执行 permission gate；
4. 执行 parameter validator；
5. 调用 endpoint `open()` 并读取初始 revision/snapshot。

endpoint 返回一个 `DocumentEndpointSubscription`：

~~~java
DocumentEndpoint endpoint = (context, params, observer) ->
        new DocumentEndpointSubscription() {
            public long getRevision() { return revision; }
            public Map<String, ?> snapshot() { return state; }

            public Object executeCommand(String key, Object payload)
                    throws DocumentSyncException {
                if (!"set-filter".equals(key)) {
                    throw new DocumentSyncException(
                            DocumentSyncErrorCode.COMMAND_NOT_FOUND,
                            "Unknown storage command");
                }
                // 再检查距离、权限、expected revision 和业务参数。
                applyFilter(payload);
                observer.patch(baseRevision, revision,
                        DocumentPatch.of(
                                DocumentPatchOperation.set("filter", payload)));
                return null;
            }

            public void close() { releaseBackendListener(); }
        };
~~~

endpoint 的 observer 和 subscription 都属于创建它们的 server thread。业务更新必须使用连续 revision：

~~~java
observer.patch(oldRevision, newRevision, DocumentPatch.of(
        DocumentPatchOperation.set("items", page),
        DocumentPatchOperation.remove("loading")));

observer.reset(newRevision, completeSnapshot);
~~~

当前 patch 是 top-level store key 的原子 `set/remove` 批次。嵌套对象需要整体替换该 top-level key。
同一个 patch 内不能重复修改相同 key。

## 4. Client RemoteStore

handler 初始化前后都可以申请订阅；handshake 完成前的订阅会排队：

~~~java
RemoteStoreSubscription scope = documentSync.subscribe(
        "rtsbuilding:storage", 1, params);
RemoteStore remote = scope.getStore();
MuiStore state = remote.getState();

StoreSubscription titleBinding = MuiStoreBinding.text(
        state, "title", titleElement);
~~~

相同 endpoint、schema 和结构相同的 params 共享一个 `RemoteStore`。params map 的 key 顺序不影响共享，
list 顺序仍有意义。每次 `subscribe` 返回独立 handle；最后一个 handle `close()` 时发送 `UNSUBSCRIBE`。

状态为：

| 状态 | 含义 |
|---|---|
| `PENDING` | 等待 READY 或 SUBSCRIBED |
| `ACTIVE` | snapshot 已安装，可发 command |
| `ERROR` | endpoint/schema/permission/revision 等错误 |
| `CLOSED` | 最后一个引用或整个 document 已关闭 |

`MuiStore` 的本地 revision 表示 binding mutation 次数；`RemoteStore.getRemoteRevision()` 才是服务端协议
revision，两者不能混用。

显式命令返回 future：

~~~java
CompletableFuture<DocumentCommandResult> result =
        remote.command("set-filter", remote.getRemoteRevision(), "ores");
~~~

DOM event 本身不会自动产生网络包。Java 或可选 JS controller 必须调用 manifest/Java 允许的 command。

## 5. Wire protocol

当前协议版本为 `1`，frame opcode 为：

| 方向 | Frame |
|---|---|
| C2S | `HELLO(protocolVersion, features)` |
| S2C | `READY(protocolVersion, limits)` |
| C2S | `SUBSCRIBE(requestId, endpointKey, schemaVersion, params)` |
| S2C | `SUBSCRIBED(requestId, channelId, revision, snapshot)` |
| S2C | `PATCH(channelId, baseRevision, revision, patch)` |
| C2S | `COMMAND(channelId, commandKey, requestId, expectedRevision, payload)` |
| S2C | `RESULT(requestId, revision, payload)` |
| S2C | `ERROR(requestId, code, safeMessage)` |
| 双向 | `UNSUBSCRIBE(channelId, reason)` |
| S2C | `RESET(channelId, revision, snapshot)` |

服务端分配 channel id。client request id 必须严格递增；重复或倒序会关闭 session，防止 command replay。
未知 opcode、错误字段集合、错误数字类型、尾随字节和非法长度会被拒绝。

`MuiValueCodec` 只允许：`null`、boolean、int、long、double、UTF-8 string、`byte[]`、list 和
string-key map。禁止 Java serialization、任意 NBT/Gson reflection、循环对象和任意 Java instance。

默认限额：

| 项目 | 默认值 |
|---|---:|
| 单 frame | 256 KiB |
| UTF-8 string | 32767 bytes |
| collection entries | 4096 |
| nesting depth | 32 |
| active channels | 128 |
| pending client commands | 256 |
| patch operations/frame | 4096 |

client/server READY 后使用双方上限的逐项较小值。revision gap/rollback 只关闭受影响 store/channel；严重
framing、方向或 request replay 错误关闭整个 document session。

## 6. 线程与生命周期

- `DocumentClientSession`、`RemoteStore` 和内部 `MuiStore` 属于 client/UI owner thread。
- `DocumentServerSession` 和 endpoint subscription 属于 server tick thread。
- endpoint observer 不能从 worker thread 直接调用；worker 结果必须先排回 server thread。
- handler `dispose()` 会关闭 channel、endpoint subscription 和未完成 command future。
- late subscription response 会立即 `UNSUBSCRIBE`；未知/late command result 被视为协议错误。
- screen reload 应销毁旧 handler/session，不能把旧 generation 的 result 投影到新 document。

## 7. 真实物品槽位

`RemoteStore` 适合虚拟物品单元格、仓储页、任务列表和其他动态集合。真实 `ModularSlot` 仍属于 vanilla
container protocol：

- 数量和 ordinal 在 panel build 时固定；
- DOM 可移动已有 slot 的视觉节点；
- 分页可使用预分配 slot pool 和 parking parent；
- 大型动态仓储使用 virtual item cell + command；
- 真正改变真实 slot 数量时关闭并重新打开 container。

因此后台重建物品格子时，应重建虚拟 DOM row，或计算 `SlotViewUpdate` 来移动固定 slot pool；不能根据
后台结果动态增删 `ItemSlotSH`。

## 8. 当前边界

- 已实现动态 endpoint/channel 协议、Java store 投影与固定 handler 适配。
- 固定容器另有已实现的 `MuiProtocolPlan`、protocol XML、显式 factory registry、OpenGui fingerprint
  handshake 和真实 slot topology 校验；它不是动态 channel 的一部分。详见
  [`modularui-fixed-protocol-template.md`](modularui-fixed-protocol-template.md)。
- GraalJS 不在 MUI 主 jar 中；未来 addon 只能通过同一 `RemoteStore`/DOM/event API 接入，不能实现第二套网络。
- RTS、AE2/RS/JEI 权限和业务 schema 属于独立 RTS addon，不进入 MUI core。
