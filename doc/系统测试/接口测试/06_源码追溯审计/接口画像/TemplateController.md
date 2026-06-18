# TemplateController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\TemplateController.java`  
> **路径前缀**: `/api/template`  
> **类级权限**: `hasRole('TEACHER')`  
> **接口数量**: 5

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/template/list` | hasRole('TEACHER') | `list` | `Result<List<Map<String, Object>>>` | L23 |
| 2 | GET | `/api/template/{id}` | hasRole('TEACHER') | `detail` | `Result<Map<String, Object>>` | L29 |
| 3 | POST | `/api/template/add` | hasRole('TEACHER') | `add` | `Result<Void>` | L34 |
| 4 | PUT | `/api/template/update/{id}` | hasRole('TEACHER') | `update` | `Result<Void>` | L40 |
| 5 | DELETE | `/api/template/{id}` | hasRole('TEACHER') | `delete` | `Result<Void>` | L46 |

---

## 接口详细签名

### 1. GET `/api/template/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\TemplateController.java:23`
- **Java 方法**: `Result<List<Map<String, Object>>> list(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@RequestParam(required = false`

### 2. GET `/api/template/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\TemplateController.java:29`
- **Java 方法**: `Result<Map<String, Object>> detail(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`

### 3. POST `/api/template/add`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\TemplateController.java:34`
- **Java 方法**: `Result<Void> add(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@Valid @RequestBody TemplateCreateRequest request`

### 4. PUT `/api/template/update/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\TemplateController.java:40`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`
  - `@Valid @RequestBody TemplateCreateRequest request`

### 5. DELETE `/api/template/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\TemplateController.java:46`
- **Java 方法**: `Result<Void> delete(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
