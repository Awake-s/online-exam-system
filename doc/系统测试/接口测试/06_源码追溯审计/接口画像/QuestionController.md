# QuestionController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\QuestionController.java`  
> **路径前缀**: `/api/question`  
> **类级权限**: `hasRole('TEACHER')`  
> **接口数量**: 7

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/question/list` | hasRole('TEACHER') | `list` | `Result<PageResult<Map<String, Object>>>` | L25 |
| 2 | GET | `/api/question/{id}` | hasRole('TEACHER') | `detail` | `Result<Map<String, Object>>` | L36 |
| 3 | POST | `/api/question/add` | hasRole('TEACHER') | `add` | `Result<Void>` | L41 |
| 4 | PUT | `/api/question/update/{id}` | hasRole('TEACHER') | `update` | `Result<Void>` | L47 |
| 5 | DELETE | `/api/question/{id}` | hasRole('TEACHER') | `delete` | `Result<String>` | L53 |
| 6 | DELETE | `/api/question/batch` | hasRole('TEACHER') | `batchDelete` | `Result<Map<String, Object>>` | L59 |
| 7 | POST | `/api/question/import` | hasRole('TEACHER') | `importQuestions` | `Result<Map<String, Object>>` | L67 |

---

## 接口详细签名

### 1. GET `/api/question/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\QuestionController.java:25`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. GET `/api/question/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\QuestionController.java:36`
- **Java 方法**: `Result<Map<String, Object>> detail(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`

### 3. POST `/api/question/add`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\QuestionController.java:41`
- **Java 方法**: `Result<Void> add(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@Valid @RequestBody QuestionAddRequest request`

### 4. PUT `/api/question/update/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\QuestionController.java:47`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`
  - `@Valid @RequestBody QuestionAddRequest request`

### 5. DELETE `/api/question/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\QuestionController.java:53`
- **Java 方法**: `Result<String> delete(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long id`

### 6. DELETE `/api/question/batch`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\QuestionController.java:59`
- **Java 方法**: `Result<Map<String, Object>> batchDelete(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@RequestBody List<Long> ids`

### 7. POST `/api/question/import`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\QuestionController.java:67`
- **Java 方法**: `Result<Map<String, Object>> importQuestions(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@RequestParam("file"`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
