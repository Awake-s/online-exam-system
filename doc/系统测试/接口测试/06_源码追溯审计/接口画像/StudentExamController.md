# StudentExamController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\StudentExamController.java`  
> **路径前缀**: `/api/student/exam`  
> **类级权限**: `hasRole('STUDENT')`  
> **接口数量**: 6

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/student/exam/my-exams` | hasRole('STUDENT') | `myExams` | `Result<List<Map<String, Object>>>` | L27 |
| 2 | GET | `/api/student/exam/start/{examId}` | hasRole('STUDENT') | `start` | `Result<Map<String, Object>>` | L34 |
| 3 | POST | `/api/student/exam/submit` | hasRole('STUDENT') | `submit` | `Result<Map<String, Object>>` | L39 |
| 4 | GET | `/api/student/exam/result/{recordId}` | hasRole('STUDENT') | `result` | `Result<Map<String, Object>>` | L44 |
| 5 | POST | `/api/student/exam/auto-save` | hasRole('STUDENT') | `autoSave` | `Result<Void>` | L49 |
| 6 | POST | `/api/student/exam/switch-screen/{recordId}` | hasRole('STUDENT') | `switchScreen` | `Result<Map<String, Object>>` | L55 |

---

## 接口详细签名

### 1. GET `/api/student/exam/my-exams`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\StudentExamController.java:27`
- **Java 方法**: `Result<List<Map<String, Object>>> myExams(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**: _(无参数)_

### 2. GET `/api/student/exam/start/{examId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\StudentExamController.java:34`
- **Java 方法**: `Result<Map<String, Object>> start(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@PathVariable Long examId`

### 3. POST `/api/student/exam/submit`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\StudentExamController.java:39`
- **Java 方法**: `Result<Map<String, Object>> submit(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@Valid @RequestBody ExamSubmitRequest request`

### 4. GET `/api/student/exam/result/{recordId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\StudentExamController.java:44`
- **Java 方法**: `Result<Map<String, Object>> result(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@PathVariable Long recordId`

### 5. POST `/api/student/exam/auto-save`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\StudentExamController.java:49`
- **Java 方法**: `Result<Void> autoSave(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@RequestBody ExamSubmitRequest request`

### 6. POST `/api/student/exam/switch-screen/{recordId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\StudentExamController.java:55`
- **Java 方法**: `Result<Map<String, Object>> switchScreen(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@PathVariable Long recordId`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
