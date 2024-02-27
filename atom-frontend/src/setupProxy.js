const {createProxyMiddleware} = require('http-proxy-middleware');

const target = "http://localhost:8081/";

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
