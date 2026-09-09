
import { tokens } from '../../theme/tokens';
import { Tab, Tabs } from '@mui/material';
import {
  TabPanel,
  usePersistedTab,
} from '../../components';
import Config from './Config';
import Log from './Log';
import BuildInfo from './BuildInfo';
import SeverNodeList from './SeverNodeList';
import configs from '../../config';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';
import { useTranslation } from 'react-i18next';
import { Page, PageCard } from '../../components';

const useStyles = createUseStyles({
  tabs: {
    borderBottom: `1px solid ${tokens.border}`,
    '& .MuiTab-root': {
      fontSize: '14px',
      fontWeight: 500,
      textTransform: 'none',
      minHeight: '48px',
      color: tokens.textMuted,
      '&.Mui-selected': {
        color: tokens.primary,
        fontWeight: 600,
      },
    },
    '& .MuiTabs-indicator': {
      backgroundColor: tokens.primary,
      height: '2px',
    },
  },
  content: {
    padding: ({ theme }) => theme.spacing(3),
  },
});

const systemDashboardConfigTabKey = configs.app + '-system-dashboard-tab';

function System() {
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { t } = useTranslation();

  const [value, handleChange] = usePersistedTab(systemDashboardConfigTabKey);

  return (
    <Page>
      <PageCard>
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
      </PageCard>
    </Page>
  );
}

export default System;
