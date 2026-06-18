# ClassController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java`  
> **路径前缀**: `/api/class`  
> **类级权限**: `hasRole('ADMIN')`  
> **接口数量**: 8

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/class/list` | hasRole('ADMIN') | `list` | `Result<PageResult<Map<String, Object>>>` | L24 |
| 2 | GET | `/api/class/{id}` | hasRole('ADMIN') | `detail` | `Result<Map<String, Object>>` | L32 |
| 3 | POST | `/api/class/add` | hasRole('ADMIN') | `add` | `Result<Void>` | L37 |
| 4 | PUT | `/api/class/update/{id}` | hasRole('ADMIN') | `update` | `Result<Void>` | L43 |
| 5 | DELETE | `/api/class/{id}` | hasRole('ADMIN') | `delete` | `Result<Void>` | L49 |
| 6 | GET | `/api/class/students/{classId}` | hasRole('ADMIN') | `students` | `Result<List<Map<String, Object>>>` | L55 |
| 7 | GET | `/api/class/all` | hasRole('ADMIN') | `all` | `Result<List<Map<String, Object>>>` | L60 |
| 8 | GET | `/api/class/my` | hasAnyRole('ADMIN', 'TEACHER') | `myClasses` | `Result<List<Map<String, Object>>>` | L66 |

---

## 接口详细签名

### 1. GET `/api/class/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java:24`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. GET `/api/class/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java:32`
- **Java 方法**: `Result<Map<String, Object>> detail(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@PathVariable Long id`

### 3. POST `/api/class/add`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java:37`
- **Java 方法**: `Result<Void> add(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@Valid @RequestBody ClassAddRequest request`

### 4. PUT `/api/class/update/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java:43`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@PathVariable Long id`
  - `@Valid @RequestBody ClassAddRequest request`

### 5. DELETE `/api/class/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java:49`
- **Java 方法**: `Result<Void> delete(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@PathVariable Long id`

### 6. GET `/api/class/students/{classId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java:55`
- **Java 方法**: `Result<List<Map<String, Object>>> students(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**:
  - `@PathVariable Long classId`

### 7. GET `/api/class/all`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java:60`
- **Java 方法**: `Result<List<Map<String, Object>>> all(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**: _(无参数)_

### 8. GET `/api/class/my`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ClassController.java:66`
- **Java 方法**: `Result<List<Map<String, Object>>> myClasses(...)`
- **鉴权要求**: hasAnyRole('ADMIN', 'TEACHER')
- **参数清单**: _(无参数)_

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
