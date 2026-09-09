import { UserDashboard } from './components';
import { Box } from '@mui/material';
import { Page, PageCard } from '../../components';
import SetLoginPassword from './components/SetLoginPassword';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';

const useStyles = createUseStyles({
  section: {
    marginBottom: ({ theme }) => theme.spacing(3),
  },
});

const Mine = () => {
  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <Page>
      <Box className={classes.section}>
        <PageCard>
          <UserDashboard />
        </PageCard>
      </Box>
      <Box className={classes.section}>
        <PageCard>
          <SetLoginPassword />
        </PageCard>
      </Box>
    </Page>
  );
};

export default Mine;
