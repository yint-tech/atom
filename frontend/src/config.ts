const config = {
  app: 'atom',
  api_prefix: '/atom-api',
  login_token_key: 'Atom-Token',
  login_user_key: 'Atom-USER',
  doc_path: '/atom-doc/index.html',
  logo_path: '/images/logos/logo.svg',
  main_site: 'https://github.com/yint-tech/atom',
  footer: 'atom-系统框架',
  copyRight: `${new Date().getFullYear()} 因体信息公司`,
};

/** 以站点 base 为前缀解析 public 资源路径（等价于 CRA 的 process.env.PUBLIC_URL） */
export function assetUrl(path: string): string {
  return import.meta.env.BASE_URL.replace(/\/+$/, '') + path;
}

export default config;
