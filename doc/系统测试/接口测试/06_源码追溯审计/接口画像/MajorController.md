# MajorController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MajorController.java`  
> **路径前缀**: `/api/major`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 5

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | GET | `/api/major/list` | 公开 / 默认认证 | `list` | `Result<PageResult<Map<String, Object>>>` | L22 |
| 2 | POST | `/api/major/add` | 公开 / 默认认证 | `add` | `Result<Void>` | L31 |
| 3 | PUT | `/api/major/update/{id}` | 公开 / 默认认证 | `update` | `Result<Void>` | L38 |
| 4 | DELETE | `/api/major/{id}` | 公开 / 默认认证 | `delete` | `Result<Void>` | L45 |
| 5 | GET | `/api/major/all` | 公开 / 默认认证 | `all` | `Result<List<Map<String, Object>>>` | L52 |

---

## 接口详细签名

### 1. GET `/api/major/list`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MajorController.java:22`
- **Java 方法**: `Result<PageResult<Map<String, Object>>> list(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam(defaultValue = "1"`

### 2. POST `/api/major/add`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MajorController.java:31`
- **Java 方法**: `Result<Void> add(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@Valid @RequestBody MajorAddRequest request`

### 3. PUT `/api/major/update/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MajorController.java:38`
- **Java 方法**: `Result<Void> update(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`
  - `@Valid @RequestBody MajorAddRequest request`

### 4. DELETE `/api/major/{id}`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MajorController.java:45`
- **Java 方法**: `Result<Void> delete(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@PathVariable Long id`

### 5. GET `/api/major/all`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\MajorController.java:52`
- **Java 方法**: `Result<List<Map<String, Object>>> all(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**: _(无参数)_

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
