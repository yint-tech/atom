import { tokens } from '../../theme/tokens';
import { useTranslation } from 'react-i18next';
import { Tab, Tabs } from '@mui/material';
import { Page, PageCard } from '../../components';
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
  tabs: {
    '& .MuiTab-root': {
      fontSize: '14px',
      fontWeight: 500,
      textTransform: 'none',
      color: tokens.textMuted,
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
    <Page>
      <PageCard>
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
      </PageCard>
    </Page>
  );
}

export default Metrics;
