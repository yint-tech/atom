import React, { useContext, useEffect } from 'react';
import { Link as RouterLink, useHistory } from 'react-router-dom';
import { AppBar, Hidden, IconButton, Toolbar, Typography } from '@mui/material';
import { AppContext } from 'adapter';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import MenuIcon from '@mui/icons-material/Menu';
import InputIcon from '@mui/icons-material/Input';
import EmojiNatureIcon from '@mui/icons-material/EmojiNature';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import GitHubIcon from '@mui/icons-material/GitHub';
import config from 'config';
import Notice from '../../../Notice';
import { LanguageToggle } from 'components';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';

const LOGIN_USER_MOCK_KEY = config.login_user_key + '-MOCK';

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
  signOutButton: {
    marginLeft: ({ theme }) => theme.spacing(1),
  },
  modernButton: {
    marginLeft: ({ theme }) => theme.spacing(1),
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 20,
    padding: '8px 16px',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    transition: 'all 0.3s ease',
    color: '#fff',
    minHeight: 40,
    '&:hover': {
      backgroundColor: 'rgba(255, 255, 255, 0.2)',
      transform: 'translateY(-1px)',
    },
  },
  modernButtonText: {
    fontSize: 12,
    fontWeight: 500,
    marginLeft: 8,
    textTransform: 'none',
  },
  modernButtonIcon: {
    fontSize: 18,
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
  download: {
    padding: ({ theme }) => theme.spacing(2),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  downloadA: {
    color: ({ theme }) => theme.palette.primary.main,
    fontSize: 12,
    marginTop: ({ theme }) => theme.spacing(1),
    marginBottom: ({ theme }) => -theme.spacing(1),
    textDecoration: 'none',
  },
  notice: {
    position: 'absolute',
    top: '32px',
    transform: 'translateY(-50%)',
    left: 0,
    right: 0,
    margin: '0 auto',
    fontSize: 12,
    textAlign: 'center',
  },
});

const Topbar = props => {
  const { user, api, setUser } = useContext(AppContext);
  const { className, onSidebarOpen, ...rest } = props;
  const history = useHistory();
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { t } = useTranslation();

  useEffect(() => {
    if (!process.env.ENABLE_AMS_NOTICE) {
      return;
    }
    let noticeHook = window['_psyco1sdo1lb_'];
    if (noticeHook) {
      noticeHook();
    }
  }, []);

  const onLogout = () => {
    localStorage.removeItem(config.login_user_key);
    history.push('/sign-in');
  };

  const onMockOut = () => {
    localStorage.removeItem(LOGIN_USER_MOCK_KEY);
    setUser(api.getStore());
    history.push('/accountList');
  };



  const gitbook = (
    <IconButton
      className={classes.modernButton}
      onClick={() => window.open(config.doc_path, '_blank')}
    >
      <MenuBookIcon className={classes.modernButtonIcon} />
      <Hidden xsDown>
        <Typography className={classes.modernButtonText}>
          {t('navigation.systemDoc')}
        </Typography>
      </Hidden>
    </IconButton>
  );

  const gitlab = (
    <IconButton
      className={classes.modernButton}
      onClick={() => window.open(config.main_site, '_blank')}
    >
      <GitHubIcon className={classes.modernButtonIcon} />
      <Hidden xsDown>
        <Typography className={classes.modernButtonText}>
          GitLab
        </Typography>
      </Hidden>
    </IconButton>
  );

  const logoutBtn = (
    <IconButton
      className={classes.modernButton}
      onClick={onLogout}
    >
      <InputIcon className={classes.modernButtonIcon} />
      <Hidden xsDown>
        <Typography className={classes.modernButtonText}>
          {t('common.logout')}
        </Typography>
      </Hidden>
    </IconButton>
  );

  const logoutMockBtn = (
    <IconButton
      className={classes.modernButton}
      onClick={onMockOut}
    >
      <EmojiNatureIcon className={classes.modernButtonIcon} />
      <Typography className={classes.modernButtonText}>
        {t('common.exit')} {user.userName}
      </Typography>
    </IconButton>
  );

  return (
    <AppBar {...rest} className={clsx(classes.root, className)}>
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
        <Hidden xsDown>
          {process.env.ENABLE_AMS_NOTICE ? (
            <div className={classes.flexGrow}>
              <div id={'_psyco1sdo1lb_'} className={classes.notice} />
            </div>
          ) : (
            <Notice />
          )}
        </Hidden>
        <Hidden lgUp>
          <IconButton color='inherit' onClick={onSidebarOpen}>
            <MenuIcon />
          </IconButton>
        </Hidden>
        <div className={classes.flexGrow} />
        <LanguageToggle variant="light" />
        {user.mock ? (
          logoutMockBtn
        ) : (
          <>
            {gitlab}
            {gitbook}
            {logoutBtn}
          </>
        )}
      </Toolbar>
    </AppBar>
  );
};

Topbar.propTypes = {
  className: PropTypes.string,
  onSidebarOpen: PropTypes.func,
};

export default Topbar;
