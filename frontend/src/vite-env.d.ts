/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 因体加密环境的授权组件开关，由 server 模块 gradle 构建注入（"true"/"false"） */
  readonly ENABLE_AMS_NOTICE?: string;
}
