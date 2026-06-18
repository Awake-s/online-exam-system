#!/bin/bash
# ============================================================
# 在线考试系统 — Phase 2 部署脚本
# 职责：前端/后端部署 + Nginx 配置 + systemd 启动 + 健康检查
# 前置：dist 和 exam-system-1.0.0.jar 已上传到 ~/
# 用法：bash ~/deploy-phase2.sh 2>&1 | tee ~/deploy-phase2.log
# ============================================================

SERVER_IP="124.222.21.219"

die() { echo "❌ 错误: $*" >&2; exit 1; }

echo "========================================="
echo "[1/10] 验证上传文件"
echo "========================================="
[ -f ~/dist/index.html ] || die "~/dist/index.html 不存在"
[ -f ~/exam-system-1.0.0.jar ] || die "~/exam-system-1.0.0.jar 不存在"
ls -la ~/dist/index.html ~/exam-system-1.0.0.jar
echo "✅ dist 和 JAR 都在"

echo ""
echo "========================================="
echo "[2/10] 修正 /opt/exam-system 顶层权限（让 nginx 能访问）"
echo "========================================="
sudo chmod 755 /opt/exam-system
sudo chmod 755 /opt/exam-system/frontend 2>/dev/null || true
sudo ls -la /opt/exam-system/
echo "✅ 顶层权限已改为 755"

echo ""
echo "========================================="
echo "[3/10] 部署后端 JAR（幂等）"
echo "========================================="
sudo cp ~/exam-system-1.0.0.jar /opt/exam-system/exam-system-1.0.0.jar || die "cp JAR 失败"
sudo chown exam:exam /opt/exam-system/exam-system-1.0.0.jar
sudo chmod 640 /opt/exam-system/exam-system-1.0.0.jar
sudo ls -la /opt/exam-system/exam-system-1.0.0.jar

echo ""
echo "========================================="
echo "[4/10] 部署前端 dist"
echo "========================================="
sudo rm -rf /opt/exam-system/frontend/dist
sudo cp -r ~/dist /opt/exam-system/frontend/dist || die "cp dist 失败"
sudo chown -R exam:exam /opt/exam-system/frontend
sudo find /opt/exam-system/frontend -type d -exec chmod 755 {} \;
sudo find /opt/exam-system/frontend -type f -exec chmod 644 {} \;
FILES=$(sudo find /opt/exam-system/frontend/dist -type f | wc -l)
echo "✅ 前端文件数: ${FILES}"

echo ""
echo "========================================="
echo "[5/10] 修正 CORS 白名单（加入 IP 访问）"
echo "========================================="
if ! sudo grep -q "http://${SERVER_IP}" /etc/exam-system/secrets.env; then
    sudo sed -i "s|^APP_CORS_ORIGINS=|APP_CORS_ORIGINS=http://${SERVER_IP},|" /etc/exam-system/secrets.env
    echo "✅ CORS 已追加 http://${SERVER_IP}"
else
    echo "✅ CORS 已含 http://${SERVER_IP}"
fi
sudo grep "^APP_CORS_ORIGINS" /etc/exam-system/secrets.env

echo ""
echo "========================================="
echo "[6/10] 部署 Nginx 限流 + 日志格式"
echo "========================================="
sudo tee /etc/nginx/conf.d/00-exam-ratelimit.conf > /dev/null <<'EOF'
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=20r/s;
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;
EOF

sudo tee /etc/nginx/conf.d/00-exam-logformat.conf > /dev/null <<'EOF'
log_format exam_access '$remote_addr - $remote_user [$time_local] '
                       '"$request" $status $body_bytes_sent '
                       '"$http_referer" "$http_user_agent" '
                       'rt=$request_time uct="$upstream_connect_time" '
                       'uht="$upstream_header_time" urt="$upstream_response_time"';
EOF
echo "✅ conf.d 片段已写入"

echo ""
echo "========================================="
echo "[7/10] 部署 Nginx 站点 + 测试"
echo "========================================="
sudo cp ~/deploy/nginx.conf /etc/nginx/sites-available/exam-system
sudo ln -sf /etc/nginx/sites-available/exam-system /etc/nginx/sites-enabled/exam-system
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t || die "nginx -t 失败"
sudo systemctl reload nginx
echo "✅ Nginx 已 reload"

echo ""
echo "========================================="
echo "[8/10] 部署 systemd service"
echo "========================================="
sudo cp ~/deploy/exam-system.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable exam-system 2>&1 | tail -3
echo "✅ systemd 已载入"

echo ""
echo "========================================="
echo "[9/10] 启动后端（等 45 秒让 Spring Boot 初始化）"
echo "========================================="
sudo systemctl restart exam-system
for i in $(seq 1 45); do
    printf "."
    sleep 1
done
echo ""
sudo systemctl status exam-system --no-pager | head -15

echo ""
echo "========================================="
echo "[10/10] 健康检查 + 端口验证"
echo "========================================="
echo "--- 后端健康 ---"
curl -s http://127.0.0.1:8081/actuator/health || echo "（启动中...）"
echo ""
echo "--- 端口监听 ---"
sudo ss -tlnp | grep -E ':80|:8081|:3306|:6379' | head -10
echo ""
echo "--- Nginx 首页 ---"
curl -s -o /dev/null -w "HTTP %{http_code}  size=%{size_download}B  time=%{time_total}s\n" http://localhost/
echo ""
echo "--- 后端最近日志（20 行）---"
sudo journalctl -u exam-system -n 20 --no-pager | tail -20

echo ""
echo "========================================="
echo "✅ 部署脚本完成！"
echo "浏览器访问：http://${SERVER_IP}"
echo "========================================="
