import { useTranslation } from 'react-i18next';
import { Tab, Tabs, Container, Card } from '@mui/material';
import {
  TabPanel,
  usePersistedTab,
} from '../../components';

import GlobalMetric from './GlobalMetric';
import SystemMetrics from './SystemMetric';
import MQLViewer from './MQLViewer';
import MetricList from './MetricList';
import configs from '../../config';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';

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
  tabs: {
    '& .MuiTab-root': {
      fontSize: '14px',
      fontWeight: 500,
      textTransform: 'none',
      color: '#546e7a',
      '&.Mui-selected': {
        color: ({ theme }) => theme.palette.primary.main,
      },
    },
    '& .MuiTabs-indicator': {
      backgroundColor: ({ theme }) => theme.palette.primary.main,
    },
  },
  content: {
    padding: ({ theme }) => theme.spacing(3),
  },
});

const metricConfigTabKey = configs.app + '-metric-tab';

function Metrics() {
  const { t } = useTranslation();
  const theme = useTheme();
  const classes = useStyles({ theme });

  const [value, handleChange] = usePersistedTab(metricConfigTabKey);

  return (
    <div className={classes.root}>
      <Container >
        <Card className={classes.card}>
          <Tabs
            value={value}
            indicatorColor='primary'
            textColor='primary'
            onChange={handleChange}
            className={classes.tabs}
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
        </Card>
      </Container>
    </div>
  );
}

export default Metrics;
