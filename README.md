# atom
java单体应用脚手架

说明文档见:[AtomDoc](https://atom.iinti.cn/atom-doc/)

## 前端部署说明

npm在国内安装依赖实在太慢，我们使用yarn2做构建工具

- nodejs >20.10.0

```bash

nvm use 20.10.0;
nvm alias default 20.10.0
npm install -g yarn # 全局安装yarn 1
corepack enable
yarn set version berry # 升级yarn到yarn2.x

```
