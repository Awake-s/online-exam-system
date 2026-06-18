# NotificationController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java`  
> **路径前缀**: `/api/notification`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 8

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/notification/list` | 公开 / 默认认证 | `list` | `Result<PageResult<Map<String, Object>>>` | L37 |
| 2 | GET | `/api/notification/unread-count` | 公开 / 默认认证 | `unreadCount` | `Result<Map<String, Object>>` | L62 |
| 3 | PUT | `/api/notification/read/{id}` | 公开 / 默认认证 | `markRead` | `Result<Void>` | L68 |
| 4 | PUT | `/api/notification/read-all` | 公开 / 默认认证 | `markAllRead` | `Result<Void>` | L75 |
| 5 | DELETE | `/api/notification/{id}` | 公开 / 默认认证 | `delete` | `Result<Integer>` | L83 |
| 6 | DELETE | `/api/notification/batch` | 公开 / 默认认证 | `batchDelete` | `Result<Integer>` | L93 |
| 7 | PUT | `/api/notification/batch-read` | 公开 / 默认认证 | `batchMarkRead` | `Result<Integer>` | L110 |
| 8 | GET | `/api/notification/pending` | 公开 / 默认认证 | `pending` | `Result<List<Map<String, Object>>>` | L124 |

---

## 接口详细签名

### 1. GET `/api/notification/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java:37`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. GET `/api/notification/unread-count`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java:62`
- **Java 方法**: `Result<Map<String, Object>> unreadCount(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

### 3. PUT `/api/notification/read/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java:68`
- **Java 方法**: `Result<Void> markRead(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 4. PUT `/api/notification/read-all`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java:75`
- **Java 方法**: `Result<Void> markAllRead(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

### 5. DELETE `/api/notification/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java:83`
- **Java 方法**: `Result<Integer> delete(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 6. DELETE `/api/notification/batch`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java:93`
- **Java 方法**: `Result<Integer> batchDelete(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestBody Map<String`
  - `List<Long>> body`

### 7. PUT `/api/notification/batch-read`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java:110`
- **Java 方法**: `Result<Integer> batchMarkRead(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestBody Map<String`
  - `List<Long>> body`

### 8. GET `/api/notification/pending`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\NotificationController.java:124`
- **Java 方法**: `Result<List<Map<String, Object>>> pending(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
