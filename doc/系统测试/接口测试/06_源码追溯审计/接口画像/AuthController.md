# AuthController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\AuthController.java`  
> **路径前缀**: `/api/auth`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 4

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/auth/captcha` | 公开 / 默认认证 | `getCaptcha` | `Result<Map<String, String>>` | L22 |
| 2 | POST | `/api/auth/login` | 公开 / 默认认证 | `login` | `Result<Map<String, Object>>` | L27 |
| 3 | GET | `/api/auth/info` | 公开 / 默认认证 | `getUserInfo` | `Result<Map<String, Object>>` | L35 |
| 4 | POST | `/api/auth/logout` | 公开 / 默认认证 | `logout` | `Result<Void>` | L41 |

---

## 接口详细签名

### 1. GET `/api/auth/captcha`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\AuthController.java:22`
- **Java 方法**: `Result<Map<String, String>> getCaptcha(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

### 2. POST `/api/auth/login`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\AuthController.java:27`
- **Java 方法**: `Result<Map<String, Object>> login(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@Valid @RequestBody LoginRequest request`

### 3. GET `/api/auth/info`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\AuthController.java:35`
- **Java 方法**: `Result<Map<String, Object>> getUserInfo(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

### 4. POST `/api/auth/logout`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\AuthController.java:41`
- **Java 方法**: `Result<Void> logout(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `HttpServletRequest request`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
