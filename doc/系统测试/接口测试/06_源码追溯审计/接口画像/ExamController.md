# ExamController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ExamController.java`  
> **路径前缀**: `/api/exam`  
> **类级权限**: `hasRole('TEACHER')`  
> **接口数量**: 5

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/exam/list` | hasRole('TEACHER') | `list` | `Result<PageResult<Map<String, Object>>>` | L24 |
| 2 | POST | `/api/exam/add` | hasRole('TEACHER') | `add` | `Result<Void>` | L33 |
| 3 | PUT | `/api/exam/update/{id}` | hasRole('TEACHER') | `update` | `Result<Void>` | L39 |
| 4 | DELETE | `/api/exam/{id}` | hasRole('TEACHER') | `delete` | `Result<Void>` | L45 |
| 5 | GET | `/api/exam/records/{examId}` | hasRole('TEACHER') | `records` | `Result<List<Map<String, Object>>>` | L51 |

---

## 接口详细签名

### 1. GET `/api/exam/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ExamController.java:24`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. POST `/api/exam/add`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ExamController.java:33`
- **Java 方法**: `Result<Void> add(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@Valid @RequestBody ExamPublishRequest request`

### 3. PUT `/api/exam/update/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ExamController.java:39`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`
  - `@Valid @RequestBody ExamPublishRequest request`

### 4. DELETE `/api/exam/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ExamController.java:45`
- **Java 方法**: `Result<Void> delete(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`

### 5. GET `/api/exam/records/{examId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ExamController.java:51`
- **Java 方法**: `Result<List<Map<String, Object>>> records(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long examId`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
