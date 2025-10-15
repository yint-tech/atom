import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Paper, Tab, Tabs } from '@mui/material';

import GlobalMetric from './GlobalMetric';
import SystemMetrics from './SystemMetric';
import MQLViewer from './MQLViewer';
import MetricList from './MetricList';
import configs from 'config';
import { createUseStyles, useTheme } from 'react-jss';

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

const metricConfigTabKey = configs.app + '-metric-tab';

function Metrics() {
  const { t } = useTranslation();
  const theme = useTheme();
  const classes = useStyles({ theme });

  let initValue = Number(localStorage.getItem(metricConfigTabKey)) || 0;
  const [value, setValue] = useState(initValue);
  useEffect(() => {
    localStorage.setItem(metricConfigTabKey, value + '');
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
          <Tab label={t('metrics.businessDashboard')} />
            <Tab label={t('metrics.systemMonitoring')} />
            <Tab label={t('metrics.mqlEditor')} />
            <Tab label={t('metrics.metricList')} />
        </Tabs>
        <div className={classes.content}>
          <TabPanel value={value} index={0}>
            <GlobalMetric />
          </TabPanel>
          <TabPanel value={value} index={1}>
            <SystemMetrics />
          </TabPanel>
          <TabPanel value={value} index={2}>
            <MQLViewer />
          </TabPanel>
          <TabPanel value={value} index={3}>
            <MetricList />
          </TabPanel>
        </div>
      </Paper>
    </div>
  );
}

export default Metrics;
