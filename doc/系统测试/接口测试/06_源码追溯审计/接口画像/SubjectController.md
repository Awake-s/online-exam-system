# SubjectController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\SubjectController.java`  
> **路径前缀**: `/api/subject`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 5

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/subject/list` | 公开 / 默认认证 | `list` | `Result<PageResult<Map<String, Object>>>` | L25 |
| 2 | POST | `/api/subject/add` | 公开 / 默认认证 | `add` | `Result<Void>` | L34 |
| 3 | PUT | `/api/subject/update/{id}` | 公开 / 默认认证 | `update` | `Result<Void>` | L41 |
| 4 | DELETE | `/api/subject/{id}` | 公开 / 默认认证 | `delete` | `Result<Void>` | L48 |
| 5 | GET | `/api/subject/all` | 公开 / 默认认证 | `all` | `Result<List<Map<String, Object>>>` | L55 |

---

## 接口详细签名

### 1. GET `/api/subject/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\SubjectController.java:25`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. POST `/api/subject/add`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\SubjectController.java:34`
- **Java 方法**: `Result<Void> add(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@Valid @RequestBody SubjectAddRequest request`

### 3. PUT `/api/subject/update/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\SubjectController.java:41`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`
  - `@Valid @RequestBody SubjectAddRequest request`

### 4. DELETE `/api/subject/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\SubjectController.java:48`
- **Java 方法**: `Result<Void> delete(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 5. GET `/api/subject/all`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\SubjectController.java:55`
- **Java 方法**: `Result<List<Map<String, Object>>> all(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
