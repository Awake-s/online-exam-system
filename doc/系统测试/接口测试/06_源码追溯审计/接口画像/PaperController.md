# PaperController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\PaperController.java`  
> **路径前缀**: `/api/paper`  
> **类级权限**: `hasRole('TEACHER')`  
> **接口数量**: 7

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/paper/list` | hasRole('TEACHER') | `list` | `Result<PageResult<Map<String, Object>>>` | L24 |
| 2 | GET | `/api/paper/{id}` | hasRole('TEACHER') | `detail` | `Result<Map<String, Object>>` | L34 |
| 3 | POST | `/api/paper/add` | hasRole('TEACHER') | `add` | `Result<Void>` | L39 |
| 4 | POST | `/api/paper/random` | hasRole('TEACHER') | `random` | `Result<Void>` | L45 |
| 5 | PUT | `/api/paper/update/{id}` | hasRole('TEACHER') | `update` | `Result<Void>` | L51 |
| 6 | DELETE | `/api/paper/{id}` | hasRole('TEACHER') | `delete` | `Result<Void>` | L57 |
| 7 | PUT | `/api/paper/togglePublish/{id}` | hasRole('TEACHER') | `togglePublish` | `Result<Void>` | L63 |

---

## 接口详细签名

### 1. GET `/api/paper/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\PaperController.java:24`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. GET `/api/paper/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\PaperController.java:34`
- **Java 方法**: `Result<Map<String, Object>> detail(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`

### 3. POST `/api/paper/add`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\PaperController.java:39`
- **Java 方法**: `Result<Void> add(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@Valid @RequestBody PaperCreateRequest request`

### 4. POST `/api/paper/random`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\PaperController.java:45`
- **Java 方法**: `Result<Void> random(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@Valid @RequestBody PaperRandomRequest request`

### 5. PUT `/api/paper/update/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\PaperController.java:51`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`
  - `@Valid @RequestBody PaperCreateRequest request`

### 6. DELETE `/api/paper/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\PaperController.java:57`
- **Java 方法**: `Result<Void> delete(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`

### 7. PUT `/api/paper/togglePublish/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\PaperController.java:63`
- **Java 方法**: `Result<Void> togglePublish(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
