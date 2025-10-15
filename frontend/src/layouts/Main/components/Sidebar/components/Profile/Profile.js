import React, { useContext } from 'react';
import { AppContext } from 'adapter';
import PropTypes from 'prop-types';
import { Alert, Avatar, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  avatar: {
    width: 60,
    height: 60,
  },
  user: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: ({ theme }) => theme.spacing(1),
  },
  line: {
    height: ({ theme }) => theme.spacing(2),
    marginLeft: ({ theme }) => theme.spacing(1),
    marginRight: ({ theme }) => theme.spacing(1),
  },
  name: {
    marginLeft: ({ theme }) => theme.spacing(1),
  },
  setting: {
    fontSize: 14,
    display: 'flex',
    alignItems: 'center',
  },
});

const Profile = () => {
  const { user, notice } = useContext(AppContext);
  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <div>
      <div className={classes.user}>
        <Avatar className={classes.purple} component={RouterLink} to='/'>
          {user.userName ? user.userName[0] : ''}
        </Avatar>
        <Typography className={classes.name} variant='h3'>
          {user.userName}
        </Typography>
      </div>
      {notice ? <Alert severity='info'>{notice}</Alert> : null}
    </div>
  );
};

Profile.propTypes = {
  className: PropTypes.string,
};

export default Profile;
