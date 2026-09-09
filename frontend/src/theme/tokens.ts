/**
 * 全局设计令牌（Design Tokens）——所有颜色的唯一出处。
 *
 * 使用规则：
 * - 业务样式（jss / sx）里的颜色一律 import tokens，不允许手写十六进制
 * - MUI palette（theme/palette.ts）同样从这里取值
 * - 换品牌 / 换主题 = 只修改这个文件
 */
export const tokens = {
  /** 品牌主色（按钮、链接、选中态、tab 高亮） */
  primary: '#4facfe',
  /** 品牌主色的悬停/加深态 */
  primaryDark: '#1976d2',
  /** 品牌渐变的收尾色 */
  primaryLight: '#00f2fe',
  /** 品牌主色的 rgb 通道，用于拼 alpha 透明度：`rgba(${tokens.primaryRgb}, 0.08)` */
  primaryRgb: '79, 172, 254',
  /** 品牌渐变（登录页标题、主操作按钮） */
  brandGradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  /** 品牌渐变的悬停态 */
  brandGradientHover: 'linear-gradient(135deg, #43a3f5 0%, #00d9fe 100%)',

  /** 页面浅灰背景 */
  pageBg: '#f8f9fa',
  /** 登录/注册页的渐变背景 */
  authPageGradient: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
  /** 分隔线/描边 */
  border: '#e9ecef',
  /** 禁用态背景 */
  disabledBg: '#e9ecef',

  /** 最深的标题文字 */
  textDark: '#1a1a1a',
  /** 卡片标题文字 */
  textTitle: '#2c3e50',
  /** 正文文字 */
  textBody: '#495057',
  /** 次要说明文字（各种灰蓝统一收敛到这一档） */
  textMuted: '#6c757d',
};
