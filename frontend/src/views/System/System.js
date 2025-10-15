import React, { useEffect, useState } from 'react';

import { Paper, Tab, Tabs, Container, Card } from '@mui/material';
import Config from './Config';
import Log from './Log';
import BuildInfo from './BuildInfo';
import SeverNodeList from './SeverNodeList';
import configs from 'config';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';

const useStyles = createUseStyles({
  root: {
    minHeight: '100vh',
    backgroundColor: '#f8f9fa',
    paddingTop: ({ theme }) => theme.spacing(3),
    paddingBottom: ({ theme }) => theme.spacing(3),
  },
  container: {
    maxWidth: '1200px',
  },
  card: {
    borderRadius: '12px',
    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)',
    border: '1px solid rgba(0, 0, 0, 0.06)',
    overflow: 'hidden',
  },
  tabs: {
    borderBottom: '1px solid #e9ecef',
    '& .MuiTab-root': {
      fontSize: '14px',
      fontWeight: 500,
      textTransform: 'none',
      minHeight: '48px',
      color: '#5a6c7d',
      '&.Mui-selected': {
        color: '#4facfe',
        fontWeight: 600,
      },
    },
    '& .MuiTabs-indicator': {
      backgroundColor: '#4facfe',
      height: '2px',
    },
  },
  content: {
    padding: ({ theme }) => theme.spacing(3),
  },
});

function TabPanel(props) {
  const { children, value, index } = props;
  return value === index ? children : null;
}

const systemDashboardConfigTabKey = configs.app + '-system-dashboard-tab';

function System() {
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { t } = useTranslation();

  let initValue =
    Number(localStorage.getItem(systemDashboardConfigTabKey)) || 0;
  const [value, setValue] = useState(initValue);
  useEffect(() => {
    localStorage.setItem(systemDashboardConfigTabKey, value + '');
  }, [value]);

  const handleChange = (event, val) => {
    setValue(val);
  };

  return (
    <div className={classes.root}>
      <Container className={classes.container}>
        <Card className={classes.card}>
          <Tabs
            value={value}
            onChange={handleChange}
            className={classes.tabs}
          >
            <Tab label={t('tabs.systemSettings')} />
            <Tab label={t('tabs.serverNodes')} />
            <Tab label={t('tabs.userOperationLog')} />
            <Tab label={t('tabs.buildInfo')} />
          </Tabs>
          <div className={classes.content}>
            <TabPanel value={value} index={0}>
              <Config />
            </TabPanel>
            <TabPanel value={value} index={1}>
              <SeverNodeList />
            </TabPanel>
            <TabPanel value={value} index={2}>
              <Log />
            </TabPanel>
            <TabPanel value={value} index={3}>
              <BuildInfo />
            </TabPanel>
          </div>
        </Card>
      </Container>
    </div>
  );
}

export default System;
