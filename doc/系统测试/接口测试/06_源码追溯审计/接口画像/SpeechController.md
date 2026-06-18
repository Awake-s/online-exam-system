# SpeechController 接口画像

> **源文件**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\SpeechController.java`  
> **路径前缀**: `/api/speech`  
> **类级权限**: _(未声明类级，按方法级或默认认证)_  
> **接口数量**: 1

---

## 接口清单

| # | Method | URL | 鉴权 | 方法名 | 返回类型 | 行号 |
|---|---|---|---|---|---|---|
| 1 | POST | `/api/speech/recognize` | 公开 / 默认认证 | `recognize` | `Result<Map<String, Object>>` | L22 |

---

## 接口详细签名

### 1. POST `/api/speech/recognize`

- **源码定位**: `D:\Java Projects\在线考试系统\exam-system\src\main\java\com\exam\controller\SpeechController.java:22`
- **Java 方法**: `Result<Map<String, Object>> recognize(...)`
- **鉴权要求**: 公开 / 默认认证
- **参数清单**:
  - `@RequestParam("audio"`

---

**生成时间**: 2026-04-30 17:14:36  
**生成方式**: `generate-api-portraits.ps1` 自动扫描源码
