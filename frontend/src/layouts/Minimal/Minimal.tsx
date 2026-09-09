import React from 'react';
import { Topbar } from './components';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';

const useStyles = createUseStyles({
  root: {
    paddingTop: 64,
    height: '100%',
  },
  content: {
    height: '100%',
  },
});

const Minimal = (props: React.PropsWithChildren<{ className?: string }>) => {
  const { children } = props;

  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <div className={classes.root}>
      <Topbar />
      <main className={classes.content}>{children}</main>
    </div>
  );
};

export default Minimal;
