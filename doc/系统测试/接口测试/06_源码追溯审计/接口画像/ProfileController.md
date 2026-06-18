# ProfileController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ProfileController.java`  
> **路径前缀**: `/api/profile`  
> **类级权限**: `isAuthenticated()`  
> **接口数量**: 4

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/profile/info` | isAuthenticated() | `info` | `Result<Map<String, Object>>` | L25 |
| 2 | PUT | `/api/profile/update` | isAuthenticated() | `update` | `Result<Void>` | L30 |
| 3 | PUT | `/api/profile/password` | isAuthenticated() | `changePassword` | `Result<Void>` | L36 |
| 4 | POST | `/api/profile/avatar` | isAuthenticated() | `uploadAvatar` | `Result<Map<String, String>>` | L42 |

---

## 接口详细签名

### 1. GET `/api/profile/info`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ProfileController.java:25`
- **Java 方法**: `Result<Map<String, Object>> info(...)`
- **鉴权要求**: isAuthenticated()
- **参数清单**: _(无参数)_

### 2. PUT `/api/profile/update`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ProfileController.java:30`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: isAuthenticated()
- **参数清单**:
  - `@Valid @RequestBody ProfileUpdateRequest request`

### 3. PUT `/api/profile/password`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ProfileController.java:36`
- **Java 方法**: `Result<Void> changePassword(...)`
- **鉴权要求**: isAuthenticated()
- **参数清单**:
  - `@Valid @RequestBody PasswordChangeRequest request`

### 4. POST `/api/profile/avatar`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ProfileController.java:42`
- **Java 方法**: `Result<Map<String, String>> uploadAvatar(...)`
- **鉴权要求**: isAuthenticated()
- **参数清单**:
  - `@RequestParam("file"`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
