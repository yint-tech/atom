import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import { AppContext } from 'adapter';
import { Card, CardContent, Divider, Typography } from '@mui/material';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  card: {
    borderRadius: '8px',
    boxShadow: 'none',
    border: '1px solid #e9ecef',
  },
  title: {
    fontSize: '14px',
    fontWeight: 600,
    color: '#495057',
    marginBottom: ({ theme }) => theme.spacing(1),
  },
  content: {
    fontSize: '14px',
    color: '#6c757d',
    marginBottom: ({ theme }) => theme.spacing(2),
  },
  divider: {
    margin: ({ theme }) => theme.spacing(2, 0),
    backgroundColor: '#e9ecef',
  },
});

const BuildInfo = () => {
  const { t } = useTranslation();
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { systemInfo } = useContext(AppContext);
  const buildInfo = systemInfo.buildInfo;
  return (
    <Card className={classes.card}>
      <CardContent>
        <Typography className={classes.title}>
          {t('system.buildTime')}
        </Typography>
        <Typography className={classes.content}>
          {buildInfo.buildTime}
        </Typography>
        <Divider className={classes.divider} />
        <Typography className={classes.title}>
          {t('system.buildHost')}
        </Typography>
        <Typography className={classes.content}>
          {buildInfo.buildUser}
        </Typography>
        <Divider className={classes.divider} />
        <Typography className={classes.title}>
          gitId
        </Typography>
        <Typography className={classes.content}>
          {buildInfo.gitId}
        </Typography>

        <Divider className={classes.divider} />
        <Typography className={classes.title}>
          {t('system.version')}
        </Typography>
        <Typography className={classes.content}>
          {buildInfo.versionName}
        </Typography>

        <Divider className={classes.divider} />
        <Typography className={classes.title}>
          {t('system.versionNumber')}
        </Typography>
        <Typography className={classes.content}>
          {buildInfo.versionCode}
        </Typography>
      </CardContent>
    </Card>
  );
};

export default BuildInfo;
