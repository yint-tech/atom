import React from 'react';
import { UserDashboard } from './components';
import { Card, Container, Box } from '@mui/material';
import SetLoginPassword from './components/SetLoginPassword';
import { createUseStyles, useTheme } from 'react-jss';

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
  section: {
    marginBottom: ({ theme }) => theme.spacing(3),
  },
  card: {
    borderRadius: '12px',
    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)',
    border: '1px solid rgba(0, 0, 0, 0.06)',
    overflow: 'hidden',
  },
});

const Mine = () => {
  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <div className={classes.root}>
      <Container className={classes.container}>
        <Box className={classes.section}>
          <Card className={classes.card}>
            <UserDashboard />
          </Card>
        </Box>
        <Box className={classes.section}>
          <Card className={classes.card}>
            <SetLoginPassword />
          </Card>
        </Box>
      </Container>
    </div>
  );
};

export default Mine;
