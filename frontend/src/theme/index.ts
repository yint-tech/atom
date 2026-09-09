import { createTheme } from '@mui/material';
import type { ThemeOptions } from '@mui/material/styles';
import { zhCN } from '@mui/material/locale';
import palette from './palette';

import typography from './typography';
import overrides from './overrides';

// v4 风格的 overrides 键在 MUI v5 的 ThemeOptions 类型中已不存在（运行时 createTheme
// 会忽略未知键），这里通过类型断言保持既有运行时行为不变。
const theme = createTheme(
  {
    palette,
    typography,
    overrides,
    zIndex: {
      appBar: 1200,
      drawer: 1100,
    },
  } as ThemeOptions,
  zhCN
);

export default theme;
