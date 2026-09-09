# atom Frontend

React 18 + TypeScript + Vite 的前端工程。

## 本地开发

- node 版本 >= 20.0.0

```shell
# 启用 corepack（内置 yarn 4）
corepack enable

yarn install

# 启动开发服务器（http://localhost:3000）
yarn dev

# 类型检查 + 生产构建（产物输出到 build/，server 模块打包时会放入 conf/static）
yarn build
```

开发服务器会把 `/atom-api` 与 `/atom-doc` 代理到本地后端（默认 `http://localhost:8081`），
如需指向其它后端：

```shell
VITE_API_TARGET=http://your-backend:8081 yarn dev
```

## 代码检查与格式化

```shell
yarn lint
yarn format
```

## 目录约定

- `src/apis/` 接口注册表与类型化的请求层，接口签名与后端 controller 对应
- `src/types/` 与后端实体对齐的数据契约、jss 主题类型增强
- `src/components/DropZone/` 保留的 JS 版上传组件（骨架预留能力，未迁移 TS）
