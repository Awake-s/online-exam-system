# ChatController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java`  
> **路径前缀**: `/api/chat`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 19

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/chat/contacts` | 公开 / 默认认证 | `getContacts` | `Result<List<Map<String, Object>>>` | L24 |
| 2 | GET | `/api/chat/conversations` | 公开 / 默认认证 | `getConversations` | `Result<List<Map<String, Object>>>` | L38 |
| 3 | GET | `/api/chat/conversations/{id}/messages` | 公开 / 默认认证 | `getMessages` | `Result<PageResult<Map<String, Object>>>` | L45 |
| 4 | POST | `/api/chat/messages` | 公开 / 默认认证 | `sendMessage` | `Result<Map<String, Object>>` | L54 |
| 5 | POST | `/api/chat/messages:batch` | 公开 / 默认认证 | `sendMessagesBatch` | `Result<Map<String, Object>>` | L106 |
| 6 | PUT | `/api/chat/conversations/{id}/read` | 公开 / 默认认证 | `markAsRead` | `Result<Void>` | L156 |
| 7 | GET | `/api/chat/unread-count` | 公开 / 默认认证 | `getUnreadCount` | `Result<Map<String, Object>>` | L163 |
| 8 | GET | `/api/chat/messages/incremental` | 公开 / 默认认证 | `getIncrementalMessages` | `Result<List<Map<String, Object>>>` | L175 |
| 9 | POST | `/api/chat/messages/{id}/recall` | 公开 / 默认认证 | `recallMessage` | `Result<Void>` | L188 |
| 10 | GET | `/api/chat/messages/{id}/recall-draft` | 公开 / 默认认证 | `getRecallDraft` | `Result<Map<String, Object>>` | L204 |
| 11 | POST | `/api/chat/messages/{id}/admin-delete` | 公开 / 默认认证 | `adminDeleteMessage` | `Result<Void>` | L216 |
| 12 | DELETE | `/api/chat/conversations/{id}` | 公开 / 默认认证 | `hideConversation` | `Result<Void>` | L226 |
| 13 | PUT | `/api/chat/conversations/{id}/pin` | 公开 / 默认认证 | `pinConversation` | `Result<Void>` | L236 |
| 14 | PUT | `/api/chat/conversations/{id}/unpin` | 公开 / 默认认证 | `unpinConversation` | `Result<Void>` | L245 |
| 15 | PUT | `/api/chat/conversations/{id}/mute` | 公开 / 默认认证 | `muteConversation` | `Result<Void>` | L255 |
| 16 | PUT | `/api/chat/conversations/{id}/unmute` | 公开 / 默认认证 | `unmuteConversation` | `Result<Void>` | L264 |
| 17 | PUT | `/api/chat/conversations/{id}/unhide` | 公开 / 默认认证 | `unhideConversation` | `Result<Void>` | L274 |
| 18 | GET | `/api/chat/online-status/{userId}` | 公开 / 默认认证 | `getOnlineStatus` | `Result<Map<String, Object>>` | L285 |
| 19 | POST | `/api/chat/online-status/batch` | 公开 / 默认认证 | `batchGetOnlineStatus` | `Result<Map<String, Boolean>>` | L299 |

---

## 接口详细签名

### 1. GET `/api/chat/contacts`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:24`
- **Java 方法**: `Result<List<Map<String, Object>>> getContacts(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

### 2. GET `/api/chat/conversations`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:38`
- **Java 方法**: `Result<List<Map<String, Object>>> getConversations(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam(required = false`
  - `defaultValue = "false"`

### 3. GET `/api/chat/conversations/{id}/messages`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:45`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> getMessages(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`
  - `@RequestParam(defaultValue = "1"`

### 4. POST `/api/chat/messages`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:54`
- **Java 方法**: `Result<Map<String, Object>> sendMessage(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestBody Map<String`
  - `Object> body`

### 5. POST `/api/chat/messages:batch`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:106`
- **Java 方法**: `Result<Map<String, Object>> sendMessagesBatch(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestBody Map<String`
  - `Object> body`

### 6. PUT `/api/chat/conversations/{id}/read`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:156`
- **Java 方法**: `Result<Void> markAsRead(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 7. GET `/api/chat/unread-count`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:163`
- **Java 方法**: `Result<Map<String, Object>> getUnreadCount(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

### 8. GET `/api/chat/messages/incremental`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:175`
- **Java 方法**: `Result<List<Map<String, Object>>> getIncrementalMessages(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam(required = false`

### 9. POST `/api/chat/messages/{id}/recall`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:188`
- **Java 方法**: `Result<Void> recallMessage(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 10. GET `/api/chat/messages/{id}/recall-draft`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:204`
- **Java 方法**: `Result<Map<String, Object>> getRecallDraft(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 11. POST `/api/chat/messages/{id}/admin-delete`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:216`
- **Java 方法**: `Result<Void> adminDeleteMessage(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 12. DELETE `/api/chat/conversations/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:226`
- **Java 方法**: `Result<Void> hideConversation(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 13. PUT `/api/chat/conversations/{id}/pin`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:236`
- **Java 方法**: `Result<Void> pinConversation(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 14. PUT `/api/chat/conversations/{id}/unpin`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:245`
- **Java 方法**: `Result<Void> unpinConversation(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 15. PUT `/api/chat/conversations/{id}/mute`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:255`
- **Java 方法**: `Result<Void> muteConversation(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 16. PUT `/api/chat/conversations/{id}/unmute`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:264`
- **Java 方法**: `Result<Void> unmuteConversation(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 17. PUT `/api/chat/conversations/{id}/unhide`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:274`
- **Java 方法**: `Result<Void> unhideConversation(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 18. GET `/api/chat/online-status/{userId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:285`
- **Java 方法**: `Result<Map<String, Object>> getOnlineStatus(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long userId`

### 19. POST `/api/chat/online-status/batch`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ChatController.java:299`
- **Java 方法**: `Result<Map<String, Boolean>> batchGetOnlineStatus(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestBody List<Long> userIds`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
