# MarkingController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MarkingController.java`  
> **路径前缀**: `/api/marking`  
> **类级权限**: `hasRole('TEACHER')`  
> **接口数量**: 4

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/marking/list/{examId}` | hasRole('TEACHER') | `pendingList` | `Result<List<Map<String, Object>>>` | L23 |
| 2 | GET | `/api/marking/detail/{recordId}` | hasRole('TEACHER') | `detail` | `Result<Map<String, Object>>` | L28 |
| 3 | POST | `/api/marking/score` | hasRole('TEACHER') | `score` | `Result<Void>` | L33 |
| 4 | POST | `/api/marking/publish/{examId}` | hasRole('TEACHER') | `publish` | `Result<Void>` | L39 |

---

## 接口详细签名

### 1. GET `/api/marking/list/{examId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MarkingController.java:23`
- **Java 方法**: `Result<List<Map<String, Object>>> pendingList(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long examId`

### 2. GET `/api/marking/detail/{recordId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MarkingController.java:28`
- **Java 方法**: `Result<Map<String, Object>> detail(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long recordId`

### 3. POST `/api/marking/score`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MarkingController.java:33`
- **Java 方法**: `Result<Void> score(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@Valid @RequestBody MarkingScoreRequest request`

### 4. POST `/api/marking/publish/{examId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MarkingController.java:39`
- **Java 方法**: `Result<Void> publish(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long examId`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
