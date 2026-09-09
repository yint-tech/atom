import axios, { AxiosRequestConfig } from 'axios';
import config from '../config';
import {
  CommonRes,
  EChart4MQLData,
  IPage,
  MetricTag,
  MetricVo,
  ServerNode,
  SettingItem,
  SysConfig,
  SysLog,
  SystemInfo,
  UserInfo,
} from '../types/api';

import uri from './uri';

/** 存储在 localStorage 的登录用户信息（管理员模拟登录时会有带 mock 标记的副本） */
export type AppUser = Partial<UserInfo> & { mock?: boolean; time?: string };

const LOGIN_USER_MOCK_KEY = config.login_user_key + '-MOCK';

export type Query = Record<string, unknown>;

interface PagedQuery {
  page: number;
  pageSize: number;
  [key: string]: unknown;
}

/** 因体证书 stub 接口的返回结构（仅因体加密环境存在，开源部署下始终 404） */
export interface CertificateInfo {
  expire?: string;
  user?: string;
}

/**
 * api 方法集合，方法签名与后端 controller 一一对应。
 * 方法体由文件底部的 uri 注册表循环生成，调用方拿到的全部是这里的类型。
 */
export interface ApiMethods {
  // system
  systemInfo(): Promise<CommonRes<SystemInfo>>;
  notice(): Promise<CommonRes<string>>;
  getIntPushMsg(): Promise<string>;
  getNowCertificate(): Promise<CertificateInfo>;

  // admin
  travelToUser(params: { id: number }): Promise<CommonRes<UserInfo>>;
  userAdd(params: { userName: string; password: string }): Promise<CommonRes<UserInfo>>;
  userList(params: PagedQuery): Promise<CommonRes<IPage<UserInfo>>>;
  setConfig(data: { key: string; value: string }): Promise<CommonRes<SysConfig>>;
  setConfigs(data: Record<string, string>): Promise<CommonRes<string>>;
  allConfig(): Promise<CommonRes<SysConfig[]>>;
  settingTemplate(): Promise<CommonRes<{ normal: SettingItem[] }>>;
  listServer(params: PagedQuery): Promise<CommonRes<IPage<ServerNode>>>;
  setServerStatus(params: { id: number; enable: boolean }): Promise<CommonRes<ServerNode>>;
  grantAdmin(params: { userName: string; isAdmin: boolean }): Promise<CommonRes<string>>;
  logList(params: PagedQuery): Promise<CommonRes<IPage<SysLog>>>;

  // user
  login(params: { userName: string; password: string }): Promise<CommonRes<UserInfo>>;
  register(params: { userName: string; password: string }): Promise<CommonRes<UserInfo>>;
  getUser(): Promise<CommonRes<UserInfo>>;
  updatePassword(params: { newPassword: string }): Promise<CommonRes<UserInfo>>;
  refreshToken(): Promise<CommonRes<string>>;
  regenerateAPIToken(): Promise<CommonRes<UserInfo>>;
  permScopes(params?: Query): Promise<CommonRes<string[]>>;
  permItemsOfScope(params: { scope: string }): Promise<CommonRes<string[]>>;
  editUserPerm(params: { userName: string; permsConfig: string }): Promise<CommonRes<UserInfo>>;

  // metric
  queryMetric(params: Query): Promise<CommonRes<MetricVo[]>>;
  metricNames(): Promise<CommonRes<string[]>>;
  metricTag(params: { metricName: string }): Promise<CommonRes<MetricTag>>;
  deleteMetric(params: { metricName: string }): Promise<CommonRes<string>>;
  mqlQuery(params: { mqlScript: string; accuracy: string }): Promise<CommonRes<EChart4MQLData>>;
  allMetricConfig(): Promise<CommonRes<MetricTag[]>>;
}

export interface BaseApi extends ApiMethods {
  getStore(): AppUser;
  setStore(user: AppUser, key?: string): void;
}

let timer: ReturnType<typeof setTimeout> | null = null;

const getStore = (): AppUser => {
  const user: AppUser = JSON.parse(
    localStorage.getItem(config.login_user_key) || '{}'
  );
  const userMock: AppUser = JSON.parse(
    localStorage.getItem(LOGIN_USER_MOCK_KEY) || '{}'
  );
  return userMock.mock ? userMock : user;
};

const setStore = (user: AppUser, key?: string) => {
  const userMock: AppUser = JSON.parse(
    localStorage.getItem(LOGIN_USER_MOCK_KEY) || '{}'
  );
  const storeKey =
    key || (userMock.mock ? LOGIN_USER_MOCK_KEY : config.login_user_key);
  localStorage.setItem(storeKey, JSON.stringify(user));
};

// 与后端 CommonRes 约定的登录态异常码：statusNeedLogin / statusLoginExpire
const NEED_LOGIN_STATUS = [-4, -5];

function doRequest(request: AxiosRequestConfig): Promise<CommonRes<never>> {
  const user = getStore();
  const headers = { ...request.headers } as AxiosRequestConfig['headers'];
  if (user && headers) {
    headers[config.login_token_key] = user['loginToken'];
  }
  return new Promise((resolve, reject) => {
    axios({
      ...request,
      headers,
    })
      .then(response => {
        const res = response.data as CommonRes<never>;
        if (NEED_LOGIN_STATUS.indexOf(res.status) >= 0) {
          localStorage.removeItem(config.login_user_key);
          if (timer) {
            clearTimeout(timer);
          }
          timer = setTimeout(() => {
            window.location.href = '/#/sign-in';
          }, 100);
        }
        resolve(res);
      })
      .catch(error => {
        reject(error.response ?? error);
      });
  });
}

function doGet(
  url: string,
  params: Query = {},
  route = false
): Promise<CommonRes<never>> {
  if (route) {
    const pathPart = Object.values(params)[0];
    url += pathPart != null ? '/' + String(pathPart) : '';
  } else {
    // 组装参数
    const pairs: string[] = [];
    for (const key of Object.keys(params)) {
      const value = params[key];
      if (value == null) {
        continue;
      }
      pairs.push(
        `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`
      );
    }
    if (pairs.length > 0) {
      url += '?' + pairs.join('&');
    }
  }
  return doRequest({ method: 'get', url });
}

function doPost(
  url: string,
  data?: Query,
  asForm = false
): Promise<CommonRes<never>> {
  const buildForm = () => {
    const pairs: string[] = [];
    for (const key of Object.keys(data || {})) {
      const value = data?.[key];
      if (value != null) {
        pairs.push(
          `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`
        );
      }
    }
    return pairs.join('&');
  };

  return doRequest({
    method: 'post',
    url,
    data: asForm ? buildForm() : data,
  });
}

function doForm(url: string, data?: Query): Promise<CommonRes<never>> {
  const form = new FormData();
  for (const key of Object.keys(data || {})) {
    const value = data?.[key];
    if (value != null) {
      form.append(key, value as string | Blob);
    }
  }
  return doRequest({ method: 'post', url, data: form });
}

// 依据 uri 注册表生成 api 实现，方法签名由 ApiMethods 保证
const apiImpl: Record<string, (...args: any[]) => any> = {
  getStore,
  setStore,
};

for (const key of Object.keys(uri)) {
  const [url, method, query] = uri[key].split(' ');
  if (method === 'post') {
    apiImpl[key] = body => doPost(url, body, query !== undefined);
  } else if (method === 'form') {
    apiImpl[key] = body => doForm(url, body);
  } else {
    apiImpl[key] = params => doGet(url, params, query !== undefined);
  }
}

export const apis = apiImpl as unknown as BaseApi;

export default apis;
