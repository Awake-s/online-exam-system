# 接口画像 总览索引

> 自动生成于 2026-04-30 17:14:36，源自 exam-system/src/main/java/com/exam/controller/

## 统计

- Controller 总数: 20 个
- HTTP 接口总数: 108 个

## Controller 清单

| # | Controller | 路径前缀 | 类级权限 | 接口数 | 画像文档 |
|---|---|---|---|---|---|
| 1 | `AuthController` | `/api/auth` | _(公开/方法级)_ | 4 | [AuthController.md](AuthController.md) |
| 2 | `ChatController` | `/api/chat` | _(公开/方法级)_ | 19 | [ChatController.md](ChatController.md) |
| 3 | `ChatTypingController` | `` | _(公开/方法级)_ | 0 | _(无)_ |
| 4 | `ClassController` | `/api/class` | `hasRole('ADMIN')` | 8 | [ClassController.md](ClassController.md) |
| 5 | `DashboardController` | `/api/dashboard` | _(公开/方法级)_ | 4 | [DashboardController.md](DashboardController.md) |
| 6 | `ExamController` | `/api/exam` | `hasRole('TEACHER')` | 5 | [ExamController.md](ExamController.md) |
| 7 | `FileController` | `/api/upload` | _(公开/方法级)_ | 2 | [FileController.md](FileController.md) |
| 8 | `MajorController` | `/api/major` | _(公开/方法级)_ | 5 | [MajorController.md](MajorController.md) |
| 9 | `MarkingController` | `/api/marking` | `hasRole('TEACHER')` | 4 | [MarkingController.md](MarkingController.md) |
| 10 | `NotificationController` | `/api/notification` | _(公开/方法级)_ | 8 | [NotificationController.md](NotificationController.md) |
| 11 | `PaperController` | `/api/paper` | `hasRole('TEACHER')` | 7 | [PaperController.md](PaperController.md) |
| 12 | `ProfileController` | `/api/profile` | `isAuthenticated()` | 4 | [ProfileController.md](ProfileController.md) |
| 13 | `QuestionController` | `/api/question` | `hasRole('TEACHER')` | 7 | [QuestionController.md](QuestionController.md) |
| 14 | `ScoreController` | `/api/score` | _(公开/方法级)_ | 4 | [ScoreController.md](ScoreController.md) |
| 15 | `SpeechController` | `/api/speech` | _(公开/方法级)_ | 1 | [SpeechController.md](SpeechController.md) |
| 16 | `StudentExamController` | `/api/student/exam` | `hasRole('STUDENT')` | 6 | [StudentExamController.md](StudentExamController.md) |
| 17 | `SubjectController` | `/api/subject` | _(公开/方法级)_ | 5 | [SubjectController.md](SubjectController.md) |
| 18 | `TemplateController` | `/api/template` | `hasRole('TEACHER')` | 5 | [TemplateController.md](TemplateController.md) |
| 19 | `UserController` | `/api/user` | `hasRole('ADMIN')` | 6 | [UserController.md](UserController.md) |
| 20 | `WrongController` | `/api/wrong` | `hasRole('STUDENT')` | 4 | [WrongController.md](WrongController.md) |

---
生成器: `doc/接口测试/03_PowerShell脚本/generate-api-portraits.ps1`
