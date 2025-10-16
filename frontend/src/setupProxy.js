const { createProxyMiddleware } = require('http-proxy-middleware');

const target = 'http://atom.iinti.cn/';

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
