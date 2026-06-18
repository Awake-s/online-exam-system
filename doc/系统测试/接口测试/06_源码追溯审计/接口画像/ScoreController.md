# ScoreController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ScoreController.java`  
> **路径前缀**: `/api/score`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 4

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/score/my-scores` | 公开 / 默认认证 | `myScores` | `Result<PageResult<Map<String, Object>>>` | L22 |
| 2 | GET | `/api/score/class/{examId}` | 公开 / 默认认证 | `classScores` | `Result<Map<String, Object>>` | L30 |
| 3 | GET | `/api/score/export/{examId}` | hasRole('TEACHER') | `exportScores` | `void` | L36 |
| 4 | GET | `/api/score/analysis/{examId}` | hasRole('TEACHER') | `analysis` | `Result<Map<String, Object>>` | L42 |

---

## 接口详细签名

### 1. GET `/api/score/my-scores`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ScoreController.java:22`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> myScores(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. GET `/api/score/class/{examId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ScoreController.java:30`
- **Java 方法**: `Result<Map<String, Object>> classScores(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long examId`

### 3. GET `/api/score/export/{examId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ScoreController.java:36`
- **Java 方法**: `void exportScores(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long examId`
  - `HttpServletResponse response`

### 4. GET `/api/score/analysis/{examId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\ScoreController.java:42`
- **Java 方法**: `Result<Map<String, Object>> analysis(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**:
  - `@PathVariable Long examId`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
