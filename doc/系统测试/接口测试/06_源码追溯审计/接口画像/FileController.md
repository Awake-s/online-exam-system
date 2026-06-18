# FileController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\FileController.java`  
> **路径前缀**: `/api/upload`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 2

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | POST | `/api/upload/image` | 公开 / 默认认证 | `uploadImage` | `Result<Map<String, Object>>` | L28 |
| 2 | POST | `/api/upload/file` | 公开 / 默认认证 | `uploadFile` | `Result<Map<String, Object>>` | L77 |

---

## 接口详细签名

### 1. POST `/api/upload/image`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\FileController.java:28`
- **Java 方法**: `Result<Map<String, Object>> uploadImage(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam("file"`

### 2. POST `/api/upload/file`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\FileController.java:77`
- **Java 方法**: `Result<Map<String, Object>> uploadFile(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam("file"`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
