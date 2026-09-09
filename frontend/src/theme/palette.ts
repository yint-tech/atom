import { colors } from '@mui/material';
import { tokens } from './tokens';

const white = '#FFFFFF';
const black = '#000000';

// 颜色取值全部来自 tokens.ts（唯一出处），MUI 组件与业务 jss 样式共享同一套品牌色
const palette = {
  black,
  white,
  primary: {
    contrastText: white,
    dark: tokens.primaryDark,
    main: tokens.primary,
    light: tokens.primaryLight,
  },
  secondary: {
    contrastText: white,
    dark: colors.red[900],
    main: colors.red[600],
    light: colors.red[400],
  },
  success: {
    contrastText: white,
    dark: colors.green[900],
    main: colors.green[600],
    light: colors.green[400],
  },
  info: {
    contrastText: white,
    dark: colors.blue[900],
    main: colors.blue[600],
    light: colors.blue[400],
  },
  warning: {
    contrastText: white,
    dark: colors.orange[900],
    main: colors.orange[600],
    light: colors.orange[400],
  },
  error: {
    contrastText: white,
    dark: colors.red[900],
    main: colors.red[600],
    light: colors.red[400],
  },
  text: {
    primary: colors.blueGrey[900],
    secondary: tokens.textMuted,
    link: colors.blue[600],
  },
  background: {
    default: tokens.pageBg,
    paper: white,
  },
  icon: colors.blueGrey[600],
  divider: tokens.border,
};

export default palette;
