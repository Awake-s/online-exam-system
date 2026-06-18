# UserController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\UserController.java`  
> **路径前缀**: `/api/user`  
> **类级权限**: `hasRole('ADMIN')`  
> **接口数量**: 6

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/user/list` | hasRole('ADMIN') | `list` | `Result<PageResult<Map<String, Object>>>` | L24 |
| 2 | POST | `/api/user/add` | hasRole('ADMIN') | `add` | `Result<Void>` | L36 |
| 3 | PUT | `/api/user/update` | hasRole('ADMIN') | `update` | `Result<Void>` | L42 |
| 4 | DELETE | `/api/user/{id}` | hasRole('ADMIN') | `delete` | `Result<Void>` | L48 |
| 5 | PUT | `/api/user/status/{id}` | hasRole('ADMIN') | `updateStatus` | `Result<Void>` | L54 |
| 6 | PUT | `/api/user/reset-password/{id}` | hasRole('ADMIN') | `resetPassword` | `Result<Void>` | L60 |

---

## 接口详细签名

### 1. GET `/api/user/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\UserController.java:24`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. POST `/api/user/add`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\UserController.java:36`
- **Java 方法**: `Result<Void> add(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@Valid @RequestBody UserAddRequest request`

### 3. PUT `/api/user/update`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\UserController.java:42`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@Valid @RequestBody UserUpdateRequest request`

### 4. DELETE `/api/user/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\UserController.java:48`
- **Java 方法**: `Result<Void> delete(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@PathVariable Long id`

### 5. PUT `/api/user/status/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\UserController.java:54`
- **Java 方法**: `Result<Void> updateStatus(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@PathVariable Long id`
  - `@RequestBody Map<String`
  - `Integer> body`

### 6. PUT `/api/user/reset-password/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\UserController.java:60`
- **Java 方法**: `Result<Void> resetPassword(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@PathVariable Long id`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
