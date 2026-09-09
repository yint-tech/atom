/**
 * 与后端 cn.iinti.atom 包对齐的数据契约。
 * 除特殊说明外，所有接口响应均以后端 CommonRes 结构包装。
 */

export interface CommonRes<T = unknown> {
  status: number;
  message?: string;
  data?: T;
}

/** 后端 UserInfo 实体（password 等敏感字段后端已不再下发） */
export interface UserInfo {
  id: number;
  userName: string;
  lastActive?: string;
  createTime?: string;
  loginToken?: string;
  apiToken?: string;
  isAdmin?: boolean;
  updateTime?: string;
  permission?: string;
}

/** 后端 ServerNode 实体 */
export interface ServerNode {
  id: number;
  serverId: string;
  ip?: string;
  port: number;
  enable?: boolean;
  lastActiveTime?: string;
  createTime?: string;
  localIp?: string;
  outIp?: string;
}

/** 后端 SysLog 实体，操作审计日志 */
export interface SysLog {
  id: number;
  username: string;
  operation: string;
  params?: string;
  methodName?: string;
  createTime?: string;
}

/** 后端 SysConfig 实体，配置中心单条配置 */
export interface SysConfig {
  id: number;
  configComment?: string;
  configKey: string;
  configValue?: string;
  createTime?: string;
}

/** 设置中心配置模版中的一项（后端 Settings.allSettingsVo） */
export interface SettingItem {
  key: string;
  value: unknown;
  type: string;
  desc: string;
  detailDesc?: string;
}

/** 监控指标的 tag 定义 */
export interface MetricTag {
  id: number;
  name: string;
  tag1Name?: string;
  tag2Name?: string;
  tag3Name?: string;
  tag4Name?: string;
  tag5Name?: string;
}

/** 监控指标数据点 */
export interface MetricVo {
  name: string;
  timeKey?: string;
  type: 'COUNTER' | 'GAUGE' | 'TIMER';
  value: number;
  createTime?: string;
  tags: Record<string, string>;
}

/** MyBatis-Plus 分页返回结构 */
export interface IPage<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages?: number;
}

/** 后端 Environment.buildInfo 返回的系统信息 */
export interface SystemInfo {
  buildInfo: {
    versionCode: number;
    versionName: string;
    buildTime: string;
    buildUser: string;
    gitId: string;
  };
  env: {
    demoSite: boolean;
    debug: boolean;
  };
}

/** 后端 EChart4MQL，MQL 查询结果转 echarts 数据 */
export interface EChart4MQLData {
  legends: string[];
  xaxis: string[];
  series: { name: string; data: number[] }[];
}
