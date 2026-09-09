import React, { useEffect, useState } from 'react';

/**
 * 标签页选中态持久化到 localStorage 的通用逻辑。
 * storageKey 建议使用 `configs.app + '-页面-tab'` 形式，避免多产品同源部署时相互覆盖。
 */
export function usePersistedTab(
  storageKey: string
): [number, (event: React.SyntheticEvent, value: number) => void] {
  const [value, setValue] = useState(() => Number(localStorage.getItem(storageKey)) || 0);
  useEffect(() => {
    localStorage.setItem(storageKey, String(value));
  }, [storageKey, value]);
  const handleChange = (_event: React.SyntheticEvent, val: number) => {
    setValue(val);
  };
  return [value, handleChange];
}

/** MUI Tabs 的面板容器：仅在选中时渲染内容 */
export function TabPanel(props: {
  children?: React.ReactNode;
  value: number;
  index: number;
}) {
  return props.value === props.index ? <>{props.children}</> : null;
}
