import { useTheme as useJssTheme } from 'react-jss';
import type { Theme as MuiTheme } from '@mui/material/styles';

/**
 * react-jss 的 ThemeProvider 里挂载的是 MUI theme，
 * 但 react-jss 转发的 useTheme 类型被弱化为 object | null，
 * 这里收敛为准确的 MUI Theme 类型。
 */
export function useTheme(): MuiTheme {
  return useJssTheme() as MuiTheme;
}
