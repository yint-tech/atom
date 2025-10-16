import React, { useContext } from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import {
  AccountBox,
  Home,
  SettingsApplications,
  ShowChart,
} from '@mui/icons-material';
import { AppContext } from 'adapter';
import { Divider, Drawer, Typography } from '@mui/material';

import { Profile, SidebarNav } from './components';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';

const useStyles = createUseStyles({
  drawer: ({ theme }) => ({
    width: 260,
    [theme.breakpoints.up('lg')]: {
      marginTop: 65.5,
      height: 'calc(100% - 65.5px)',
    },
    '& .MuiDrawer-paper': {
      background: 'linear-gradient(145deg, #ffffff 0%, #f8f9fa 50%, #f1f3f4 100%)',
      borderRight: 'none',
      boxShadow: '4px 0 20px rgba(0, 0, 0, 0.06), 2px 0 8px rgba(0, 0, 0, 0.04), 0 0 1px rgba(0, 0, 0, 0.04)',
      position: 'relative',
      '&::after': {
        content: '""',
        position: 'absolute',
        top: 0,
        right: 0,
        bottom: 0,
        width: '1px',
        background: 'linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.05) 20%, rgba(0, 0, 0, 0.05) 80%, transparent 100%)',
      },
    },
  }),
  root: {
    backgroundColor: 'transparent',
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    padding: ({ theme }) => theme.spacing(2, 1.5),
  },
  nav: {
    marginBottom: ({ theme }) => theme.spacing(2),
    marginLeft: ({ theme }) => theme.spacing(-0.5),
    flex: 1,
  },
  footer: {
    marginTop: 'auto',
    padding: ({ theme }) => theme.spacing(2, 1),
    textAlign: 'center',
    borderTop: '1px solid #e9ecef',
    '& .MuiTypography-root': {
      fontSize: '12px',
      color: '#6c757d',
      fontWeight: 400,
    },
  },
});

const Sidebar = props => {
  const { user } = useContext(AppContext);
  const { open, variant, onClose, className, ...rest } = props;
  const { t } = useTranslation();

  const theme = useTheme();
  const classes = useStyles({ theme });

  let pages = [
    {
      title: t('navigation.mine'),
      href: '/mine',
      icon: <Home />,
    },
  ];

  if (user.isAdmin) {
    pages[pages.length - 1].divider = true;
    pages = pages.concat([
      {
        title: t('navigation.metrics'),
        href: '/metrics',
        icon: <ShowChart />,
      },
      {
        title: t('navigation.accountList'),
        href: '/accountList',
        icon: <AccountBox />,
      },
      {
        title: t('navigation.systemSettings'),
        href: '/systemSettings',
        icon: <SettingsApplications />,
      },
    ]);
  }

  return (
    <Drawer
      anchor='left'
      classes={{ paper: classes.drawer }}
      onClose={onClose}
      open={open}
      variant={variant}
    >
      <div {...rest} className={clsx(classes.root, className)}>
        <Profile />
        <SidebarNav className={classes.nav} pages={pages} />
        <div className={classes.footer}>
          <Typography variant="caption">
            Atom-系统框架
          </Typography>
          <br />
          <Typography variant="caption">
            © 2025 因体信息公司
          </Typography>
          <br />
          <Typography variant='caption'>
            {' '}
            &nbsp;{t('footer.products')} &nbsp;|{' '}
            <a
              href='http://majora.iinti.cn/majora-doc'
              target='_blank'
              rel='noopener noreferrer'
            >
              Majora
            </a>
            &nbsp;|{' '}
            <a
              href='http://sekiro.iinti.cn/sekiro-doc'
              target='_blank'
              rel='noopener noreferrer'
            >
              Sekiro
            </a>
            &nbsp;|{' '}
            <a
              href='https://malenia.iinti.cn/malenia-doc/index.html'
              target='_blank'
              rel='noopener noreferrer'
            >
              Malenia
            </a>
          </Typography>
        </div>
      </div>
    </Drawer>
  );
};

Sidebar.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func,
  open: PropTypes.bool.isRequired,
  variant: PropTypes.string.isRequired,
};

export default Sidebar;
