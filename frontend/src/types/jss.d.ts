import type { Theme as MuiTheme } from '@mui/material/styles';

/**
 * react-jss 的默认主题被我们替换为 MUI theme 对象（见 App.tsx 的 ThemeProvider），
 * 这里把 MUI 的 Theme 成员合并进 jss 的全局 Theme 接口，
 * 这样所有 createUseStyles / useTheme 中的 theme 参数都拥有完整的 MUI 主题类型。
 */
declare global {
  namespace Jss {
    // eslint-disable-next-line @typescript-eslint/no-empty-object-type
    export interface Theme extends MuiTheme {}
  }
}

export {};
