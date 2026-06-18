# DashboardController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\DashboardController.java`  
> **路径前缀**: `/api/dashboard`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 4

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/dashboard/admin` | 公开 / 默认认证 | `admin` | `Result<Map<String, Object>>` | L22 |
| 2 | GET | `/api/dashboard/teacher` | hasRole('ADMIN') | `teacher` | `Result<Map<String, Object>>` | L28 |
| 3 | GET | `/api/dashboard/student` | hasRole('TEACHER') | `student` | `Result<Map<String, Object>>` | L34 |
| 4 | GET | `/api/dashboard/student-trend` | hasRole('STUDENT') | `studentTrend` | `Result<java.util.List<Map<String, Object>>>` | L40 |

---

## 接口详细签名

### 1. GET `/api/dashboard/admin`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\DashboardController.java:22`
- **Java 方法**: `Result<Map<String, Object>> admin(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

### 2. GET `/api/dashboard/teacher`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\DashboardController.java:28`
- **Java 方法**: `Result<Map<String, Object>> teacher(...)`
- **鉴权要求**: hasRole('ADMIN')
- **参数清单**: _(无参数)_

### 3. GET `/api/dashboard/student`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\DashboardController.java:34`
- **Java 方法**: `Result<Map<String, Object>> student(...)`
- **鉴权要求**: hasRole('TEACHER')
- **参数清单**: _(无参数)_

### 4. GET `/api/dashboard/student-trend`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\DashboardController.java:40`
- **Java 方法**: `Result<java.util.List<Map<String, Object>>> studentTrend(...)`
- **鉴权要求**: hasRole('STUDENT')
- **参数清单**:
  - `@RequestParam(required = false`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
