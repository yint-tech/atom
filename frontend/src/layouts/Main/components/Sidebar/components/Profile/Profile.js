import React, { useContext } from 'react';
import { AppContext } from 'adapter';
import PropTypes from 'prop-types';
import { Alert, Avatar, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  container: {
    padding: ({ theme }) => theme.spacing(1.5),
    borderRadius: '8px',
    background: '#f8f9fa',
    border: '1px solid #e9ecef',
    marginBottom: ({ theme }) => theme.spacing(1.5),
  },
  user: {
    display: 'flex',
    alignItems: 'center',
    gap: ({ theme }) => theme.spacing(1.5),
  },
  avatar: {
    width: 40,
    height: 40,
    backgroundColor: '#4facfe',
    color: '#ffffff',
    fontSize: '16px',
    fontWeight: 500,
  },
  name: {
    color: '#495057',
    fontSize: '14px',
    fontWeight: 500,
    flex: 1,
  },
  notice: {
    marginTop: ({ theme }) => theme.spacing(1),
    '& .MuiAlert-root': {
      borderRadius: '6px',
      fontSize: '12px',
      padding: ({ theme }) => theme.spacing(0.5, 1),
    },
  },
});

const Profile = () => {
  const { user, notice } = useContext(AppContext);
  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <div>
      <div className={classes.container}>
        <div className={classes.user}>
          <Avatar className={classes.avatar} component={RouterLink} to='/'>
            {user.userName ? user.userName[0].toUpperCase() : 'U'}
          </Avatar>
          <Typography className={classes.name} variant='h6'>
            {user.userName || 'User'}
          </Typography>
        </div>
      </div>
      {notice ? (
        <div className={classes.notice}>
          <Alert severity='info'>{notice}</Alert>
        </div>
      ) : null}
    </div>
  );
};

Profile.propTypes = {
  className: PropTypes.string,
};

export default Profile;
