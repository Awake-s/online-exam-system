// @ts-check
import { defineConfig } from 'astro/config'
import tailwindcss from '@tailwindcss/vite'

// ====================================================================
// 在线考试系统 · 营销落地页（Astro 5 + Tailwind 4）
// --------------------------------------------------------------------
// 部署说明：
//   - Vercel / Netlify 根路径部署：site 改为你的域名，base 删除或设为 '/'
//   - GitHub Pages 项目站点（子路径）：base 设为 '/你的仓库名/'
// ====================================================================
export default defineConfig({
  site: 'https://your-domain.example', // TODO: 替换为真实域名
  // base: '/online-exam-system/',     // 仅 GitHub Pages 项目站点需要，按需开启
  vite: {
    plugins: [tailwindcss()]
  }
})
