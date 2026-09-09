import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// 本地开发时后端服务地址，默认为本机 8081 端口（gradle 启动的本地 server），
// 可通过环境变量覆盖：VITE_API_TARGET=http://xxx yarn dev
const apiTarget = process.env.VITE_API_TARGET || 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  // ENABLE_AMS_NOTICE 由 server 模块的 gradle yarnBuild 任务注入，供因体加密环境使用
  envPrefix: ['VITE_', 'ENABLE_AMS_NOTICE'],
  build: {
    // server 模块的发布包将 frontend/build 打入 conf/static，目录名不能变更
    outDir: 'build',
  },
  server: {
    port: 3000,
    proxy: {
      '/atom-api': { target: apiTarget, changeOrigin: true },
      '/atom-doc': { target: apiTarget, changeOrigin: true },
    },
  },
});
