import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { AppBar, Toolbar } from '@mui/material';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { LanguageToggle } from 'components';
import { createUseStyles, useTheme } from 'react-jss';
import config from '../../../../config';

const useStyles = createUseStyles({
  root: {
    boxShadow: 'none',
  },
  title: {
    fontWeight: 'bold',
    fontSize: 24,
    color: '#fff',
  },
  flexGrow: {
    flexGrow: 1,
  },
  logoContainer: {
    display: 'flex',
    alignItems: 'center',
    marginRight: ({ theme }) => theme.spacing(2),
  },
  logo: {
    height: 45,
    width: 'auto',
    transition: 'all 0.3s ease',
    '&:hover': {
      transform: 'scale(1.05)',
    },
  },
});

const Topbar = props => {
  const { className, ...rest } = props;

  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <AppBar
      {...rest}
      className={clsx(classes.root, className)}
      color='primary'
      position='fixed'
    >
      <Toolbar>
        <div className={classes.logoContainer}>
          <RouterLink to='/'>
            <img
              alt='Logo'
              className={classes.logo}
              src={process.env.PUBLIC_URL + config.logo_path}
            />
          </RouterLink>
        </div>
        <div className={classes.flexGrow} />
        <LanguageToggle variant="light" />
      </Toolbar>
    </AppBar>
  );
};

Topbar.propTypes = {
  className: PropTypes.string,
};

export default Topbar;
