import React, { createContext, useEffect, useState } from 'react';
import moment from 'moment';
import apis, { ApiMethods, AppUser, Query } from '../apis';
import { useSnackbar } from 'notistack';
import { Loading } from '../components';
import { CommonRes } from '../types/api';
import { SystemInfo } from '../types/api';

export interface Api extends ApiMethods {
  getStore(): AppUser;
  setStore(user: AppUser, key?: string): void;
  errorToast(msg: string): void;
  successToast(msg: string): void;
}

export interface AppContextValue {
  /** 全局用户信息 */
  user: AppUser;
  /** 挂载到全局的 API 集合，失败时自动弹出错误提示 */
  api: Api;
  setUser: (user: AppUser) => void;
  /** 给用户推送的系统通告 */
  notice: string;
  /** 后端系统信息（构建信息、环境开关等） */
  systemInfo: SystemInfo;
}

const DEFAULT_SYSTEM_INFO: SystemInfo = {
  buildInfo: {
    versionCode: 1,
    versionName: '1.0',
    buildTime: '2024-01-00_00:00:00',
    buildUser: 'yint',
    gitId: '458baa545ae941239cb62fec359a911c44cbfa3a',
  },
  env: {
    demoSite: true,
    debug: true,
  },
};

export const AppContext = createContext<AppContextValue>({
  user: {},
  api: {} as Api,
  setUser: () => {},
  notice: '',
  systemInfo: DEFAULT_SYSTEM_INFO,
});

/** 给原始 api 包一层失败时的 toast 提示，包装后的签名不变 */
function buildApi(
  enqueueSnackbar: ReturnType<typeof useSnackbar>['enqueueSnackbar']
): Api {
  const toast = (msg: string, variant: 'error' | 'success') =>
    enqueueSnackbar(msg, {
      variant,
      anchorOrigin: { vertical: 'top', horizontal: 'center' },
    });

  const impl: Record<string, unknown> = {
    getStore: () => apis.getStore(),
    setStore: (user: AppUser, key?: string) => apis.setStore(user, key),
    errorToast: (msg: string) => toast(msg, 'error'),
    successToast: (msg: string) => toast(msg, 'success'),
  };

  const origin = apis as unknown as Record<
    string,
    (...args: Query[]) => Promise<CommonRes<unknown>>
  >;
  for (const key of Object.keys(origin)) {
    const fn = origin[key];
    if (typeof fn !== 'function') {
      continue;
    }
    impl[key] = (...args: Query[]) =>
      fn(...args).then(res => {
        if (res.status !== 0) {
          console.log('call api ' + key + ' error :' + res.message);
          toast(
            (res.message || 'unknown error').substring(0, 50),
            'error'
          );
        }
        return res;
      });
  }
  return impl as unknown as Api;
}

const Adapter = (props: { children?: React.ReactNode }) => {
  const { enqueueSnackbar } = useSnackbar();
  const [user, setUser] = useState<AppUser>({});
  const [api, setApi] = useState<Api>({} as Api);
  const [notice, setNotice] = useState('');
  const [systemInfo, setSystemInfo] = useState<SystemInfo>(DEFAULT_SYSTEM_INFO);
  // 在调用任何业务代码之前，确保完成第一次的登录token刷新，避免到业务模块时，token刷新还未完成产生鉴权失败问题
  const [firstLogin, setFirstLogin] = useState(false);

  useEffect(() => {
    setApi(buildApi(enqueueSnackbar));
  }, [enqueueSnackbar]);

  useEffect(() => {
    let u = apis.getStore();
    setUser({
      ...u,
      time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss'),
    });
    const refreshUserInfo = () => {
      apis
        .notice()
        .then(res => {
          if (res.status === 0) {
            setNotice(res.data || '');
          }
        })
        .catch(() => {});
      return apis.getUser().then(res => {
        if (res.status === 0) {
          u = {
            ...apis.getStore(),
            ...res.data,
            time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss'),
          };
          apis.setStore(u);
          setUser(u);
        }
      });
    };
    refreshUserInfo().then(() => {
      setFirstLogin(true);
    });
    apis.systemInfo().then(res => {
      if (res.status === 0) {
        setSystemInfo(res.data ?? DEFAULT_SYSTEM_INFO);
      }
    });
    const timer = setInterval(() => {
      refreshUserInfo();
    }, 60 * 1000);
    return () => {
      clearInterval(timer);
    };
  }, []);

  return (
    <AppContext.Provider
      value={{
        user, // 你可以在全局访问到用户信息
        api, // 挂载到全局的，支持react特性的一些API
        setUser, // 登录，注销，穿越等功能需要修改用户内容
        notice, // 给用户推送的消息
        systemInfo, // 后端的服务器配置信息，用于前端进行功能性质的开关选型
      }}
    >
      {firstLogin ? props.children : <Loading />}
    </AppContext.Provider>
  );
};

export default Adapter;
