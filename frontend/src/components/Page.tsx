import React from 'react';
import clsx from 'clsx';
import { Card, Container } from '@mui/material';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../common/theme';

const useStyles = createUseStyles({
  root: {
    minHeight: '100vh',
    backgroundColor: '#f8f9fa',
    paddingTop: ({ theme }) => theme.spacing(3),
    paddingBottom: ({ theme }) => theme.spacing(3),
  },
  card: {
    borderRadius: '12px',
    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)',
    border: '1px solid rgba(0, 0, 0, 0.06)',
    overflow: 'hidden',
  },
  padded: {
    padding: ({ theme }) => theme.spacing(3),
  },
});

/**
 * 后台页面统一骨架：浅灰背景 + 全宽容器。
 * 内容放 PageCard（或任意自定义卡片）中，保证各页面边距与卡片观感一致。
 */
const Page = (props: { children?: React.ReactNode }) => {
  const theme = useTheme();
  const classes = useStyles({ theme });
  return (
    <div className={classes.root}>
      <Container maxWidth={false}>{props.children}</Container>
    </div>
  );
};

/** 与 Page 配套的内容卡片，padded 控制是否带统一内边距 */
export function PageCard(props: {
  children?: React.ReactNode;
  className?: string;
  padded?: boolean;
}) {
  const { children, className, padded = false } = props;
  const theme = useTheme();
  const classes = useStyles({ theme });
  return (
    <Card className={clsx(classes.card, className)}>
      {padded ? <div className={classes.padded}>{children}</div> : children}
    </Card>
  );
}

export default Page;
