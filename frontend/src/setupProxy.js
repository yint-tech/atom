const { createProxyMiddleware } = require('http-proxy-middleware');

// 本地开发时后端服务地址，默认为本机 8081 端口（gradle 启动的本地 server），
// 可通过环境变量覆盖：REACT_APP_API_TARGET=http://xxx yarn start
const target = process.env.REACT_APP_API_TARGET || 'http://localhost:8081';

module.exports = function (app) {
  app.use(
    '/atom-api',
    createProxyMiddleware({
      target: target,
      changeOrigin: true,
    })
  );
  app.use(
    '/atom-doc',
    createProxyMiddleware({
      target: target,
      changeOrigin: true,
    })
  );
};
