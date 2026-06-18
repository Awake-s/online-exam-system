# 部署工具箱 · 目录索引

> 所有与**上线、更新、运维**相关的脚本和配置都在这里。
>
> 按**使用时机**分类，从首次部署到日常维护，从上到下自然推进。

---

## 📂 目录说明

| 子目录 | 使用时机 | 内容 |
|---|---|---|
| 🚀 `01-首次部署/` | **首次部署**（一辈子跑一次） | 环境安装 + 应用部署脚本 |
| 🔄 `02-更新上线/` | **日常代码更新**（每次改代码） | 本地构建 + 服务器更新脚本 |
| 🛠️ `03-日常维护/` | **日常维护** | 备份 + 清理测试数据 |
| ⚙️ `04-运行配置/` | **运行时配置模板** | Nginx + systemd 模板 |
| 📖 `05-文档手册/` | **文档手册** | 部署指南、实战手册、运维手册 |
| 🔐 `凭据/` | **凭据私密** | 生产密码（已 gitignore） |

---

## 🚀 01-首次部署/ — 首次部署（只跑一次）

```
01-首次部署/
├── deploy.sh              # 服务器端：环境安装（MySQL/Redis/Nginx/Java）+ 生成密码
├── deploy-phase2.sh       # 服务器端：应用部署（JAR/dist/Nginx/systemd 启动）
└── secrets.env.example    # 密钥配置模板（生产环境使用前先复制一份）
```

**使用流程**：见 `05-文档手册/部署上线指南.md`。

---

## 🔄 02-更新上线/ — 日常代码更新（最常用）

```
02-更新上线/
├── local-build.ps1        # 本地 PowerShell：一键 mvn/npm build + scp 上传
└── update.sh              # 服务器 Bash：一键应用新版本 + 健康检查 + 自动回滚
```

**标准 2 步流程**（详细见 `05-文档手册/日常运维手册.md`）：

```powershell
# ① 本地（Windows PowerShell，项目根目录）
.\deploy\02-更新上线\local-build.ps1 -Target backend      # 只改后端
.\deploy\02-更新上线\local-build.ps1 -Target frontend     # 只改前端
.\deploy\02-更新上线\local-build.ps1 -Target all          # 前后端一起
```

```bash
# ② 服务器（SSH 后）
sudo bash /opt/exam-system/update.sh backend    # 或 frontend / all
```

**回滚**：

```bash
sudo bash /opt/exam-system/update.sh rollback
```

---

## 🛠️ 03-日常维护/ — 日常维护

```
03-日常维护/
├── backup.sh              # 每日 03:00 自动备份（crontab 已配置）
└── cleanup-test-data.sh   # 清理测试数据（考试/聊天/通知），保留用户/题库
```

**手动立即备份一次**：

```bash
sudo bash /opt/exam-system/backup.sh
```

**清理测试数据**：

```bash
# 预览（不会真删）
sudo bash /opt/exam-system/cleanup-test-data.sh --full --dry-run

# 正式执行
sudo bash /opt/exam-system/cleanup-test-data.sh --full
```

---

## ⚙️ 04-运行配置/ — 运行时配置模板

```
04-运行配置/
├── nginx.conf             # Nginx 站点配置（HTTP 版，当前在用）
├── nginx-https.conf       # Nginx 站点配置（HTTPS 版，备案通过后用）
└── exam-system.service    # systemd 服务定义
```

**典型使用**：首次部署时 `scp` 到服务器 `~/deploy/`，由 `deploy-phase2.sh` 自动复制到系统目录。

**切换到 HTTPS**（备案后）：

```bash
sudo cp ~/deploy/nginx-https.conf /etc/nginx/sites-available/exam-system
sudo nginx -t && sudo systemctl reload nginx
```

---

## 📖 05-文档手册/ — 文档手册

| 文档 | 何时读 |
|---|---|
| `部署上线指南.md` | **首次部署**前完整阅读 |
| `上线实战手册.md` | 执行部署时照着操作 |
| `日常运维手册.md` | **上线后**遇事速查（改代码/清数据/回滚/排错） |

---

## 🔐 凭据/ — 凭据（私密）

```
凭据/
├── exam-system-credentials-20260422-195642.txt    # MySQL/Redis/JWT 凭据备忘
└── online-exam-生产凭据.txt                        # 备用
```

⚠️ **此目录已在 `.gitignore` 中**，**永远不要 push 到 Git**。

---

## 🗺️ 快速上手决策树

```
我是新手,第一次部署       →  读 05-文档手册/部署上线指南.md + 上线实战手册.md
                            →  上传 01-首次部署/ 和 04-运行配置/ 到服务器

我改了代码,要上线         →  本地跑 02-更新上线/local-build.ps1
                            →  服务器跑 update.sh

系统挂了,要紧急恢复       →  服务器跑 update.sh rollback
                            →  看 05-文档手册/日常运维手册.md 的"回滚"章节

备案通过了,要上 HTTPS     →  本地给服务器 scp 04-运行配置/nginx-https.conf
                            →  跑 certbot 后切换配置

要清理测试数据             →  服务器跑 03-日常维护/cleanup-test-data.sh --full

想看数据备份               →  服务器 ls /opt/exam-system/backups/

不知道该做什么             →  先看 05-文档手册/日常运维手册.md
```

---

## 📝 服务器端对应路径（不受本地目录调整影响）

| 服务器路径 | 来源 |
|---|---|
| `/opt/exam-system/exam-system-1.0.0.jar` | 本地 `02-更新上线` 上传 |
| `/opt/exam-system/frontend/dist/` | 本地 `02-更新上线` 上传 |
| `/opt/exam-system/backup.sh` | 本地 `03-日常维护/backup.sh` |
| `/opt/exam-system/update.sh` | 本地 `02-更新上线/update.sh` |
| `/opt/exam-system/cleanup-test-data.sh` | 本地 `03-日常维护/cleanup-test-data.sh` |
| `/etc/nginx/sites-available/exam-system` | 本地 `04-运行配置/nginx.conf` |
| `/etc/systemd/system/exam-system.service` | 本地 `04-运行配置/exam-system.service` |
| `/etc/exam-system/secrets.env` | 基于 `01-首次部署/secrets.env.example` 生成 |
| `/home/ubuntu/staging/` | `02-更新上线/local-build.ps1` 上传目标 |

---

**最后更新**：2026-04-23
