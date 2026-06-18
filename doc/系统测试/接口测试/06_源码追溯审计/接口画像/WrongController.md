# WrongController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\WrongController.java`  
> **路径前缀**: `/api/wrong`  
> **类级权限**: `hasRole('STUDENT')`  
> **接口数量**: 4

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/wrong/subjects` | hasRole('STUDENT') | `subjects` | `Result<List<Map<String, Object>>>` | L22 |
| 2 | GET | `/api/wrong/list` | hasRole('STUDENT') | `list` | `Result<PageResult<Map<String, Object>>>` | L27 |
| 3 | GET | `/api/wrong/detail/{answerId}` | hasRole('STUDENT') | `detail` | `Result<Map<String, Object>>` | L35 |
| 4 | DELETE | `/api/wrong/{answerId}` | hasRole('STUDENT') | `remove` | `Result<Void>` | L40 |

---

## 接口详细签名

### 1. GET `/api/wrong/subjects`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\WrongController.java:22`
- **Java 方法**: `Result<List<Map<String, Object>>> subjects(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**: _(无参数)_

### 2. GET `/api/wrong/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\WrongController.java:27`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 3. GET `/api/wrong/detail/{answerId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\WrongController.java:35`
- **Java 方法**: `Result<Map<String, Object>> detail(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@PathVariable Long answerId`

### 4. DELETE `/api/wrong/{answerId}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\WrongController.java:40`
- **Java 方法**: `Result<Void> remove(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@PathVariable Long answerId`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
