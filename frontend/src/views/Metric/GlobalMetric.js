import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { createUseStyles, useTheme } from 'react-jss';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';

const useStyles = createUseStyles({
  card: {
    borderRadius: '12px',
    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)',
    border: '1px solid rgba(0, 0, 0, 0.06)',
    textAlign: 'center',
    padding: ({ theme }) => theme.spacing(4),
  },
  icon: {
    fontSize: '48px',
    color: '#90a4ae',
    marginBottom: ({ theme }) => theme.spacing(2),
  },
  title: {
    fontSize: '18px',
    fontWeight: 600,
    color: '#1a1a1a',
    marginBottom: ({ theme }) => theme.spacing(1),
  },
  description: {
    fontSize: '14px',
    color: '#546e7a',
    lineHeight: 1.6,
  },
});

const GlobalMetrics = () => {
  const { t } = useTranslation();
  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <Card className={classes.card}>
      <CardContent>
        <Box display="flex" flexDirection="column" alignItems="center">
          <TrendingUpIcon className={classes.icon} />
          <Typography className={classes.title}>
            {t('metrics.businessDashboard')}
          </Typography>
          <Typography className={classes.description}>
            {t('metrics.todoBusinessDesign')}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default GlobalMetrics;
