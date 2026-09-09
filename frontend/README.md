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

## 样式规范（不写 CSS 也能做页面）

后台页面的样式已全部收敛，新增一个业务页面的标准姿势：

```tsx
import { Page, PageCard, SimpleTable } from 'components';

const MyPage = () => (
  <Page>
    <PageCard>
      <SimpleTable loadDataFun={() => api.myList({ page: 1, pageSize: 10 })} columns={[...]} />
    </PageCard>
  </Page>
);
```

- 颜色一律从 `src/theme/tokens.ts` 取（`tokens.primary`、`tokens.border`、`tokens.textMuted`...），
  **禁止手写十六进制**；换品牌色只需改这一个文件，MUI 组件和业务样式会同步变化
- 页面骨架用 `<Page>` + `<PageCard>`；表格用 `<SimpleTable>`（客户端分页/搜索）或
  `<BackendPagedTable>`（服务端分页）；弹窗用 `<OpeDialog>`；搜索框用 `<SearchInput>`
- 确实需要自定义样式时用 `createUseStyles`，值引用 tokens；少量一次性装饰色（如插画配色）可留在组件内

## 目录约定

- `src/apis/` 接口注册表与类型化的请求层，接口签名与后端 controller 对应
- `src/types/` 与后端实体对齐的数据契约、jss 主题类型增强
- `src/components/DropZone/` 保留的 JS 版上传组件（骨架预留能力，未迁移 TS）
