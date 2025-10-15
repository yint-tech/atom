import React, { useEffect, useState } from 'react';

import { Paper, Tab, Tabs } from '@mui/material';
import Config from './Config';
import Log from './Log';
import BuildInfo from './BuildInfo';
import SeverNodeList from './SeverNodeList';
import configs from 'config';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';

const useStyles = createUseStyles({
  root: {
    flexGrow: 1,
    padding: ({ theme }) => theme.spacing(3),
  },
  content: {
    marginTop: ({ theme }) => theme.spacing(2),
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
      <Paper>
        <Tabs
          value={value}
          indicatorColor='primary'
          textColor='primary'
          onChange={handleChange}
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
      </Paper>
    </div>
  );
}

export default System;
