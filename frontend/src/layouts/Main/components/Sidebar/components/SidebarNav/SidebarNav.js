/* eslint-disable react/no-multi-comp */
/* eslint-disable react/display-name */
import React, { forwardRef } from 'react';
import { NavLink as RouterLink } from 'react-router-dom';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { Button, colors, List, ListItem, Divider } from '@mui/material';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  root: {
    padding: 0,
  },
  item: {
    display: 'flex',
    paddingTop: 0,
    paddingBottom: 0,
    marginBottom: ({ theme }) => theme.spacing(0.8),
    '&:last-child': {
      marginBottom: 0,
    },
  },
  button: {
    color: '#5a6c7d',
    padding: ({ theme }) => theme.spacing(1.2, 1.5),
    justifyContent: 'flex-start',
    textTransform: 'none',
    letterSpacing: 0,
    width: '100%',
    fontWeight: 500,
    fontSize: '14px',
    borderRadius: '8px',
    margin: ({ theme }) => theme.spacing(0, 0.5),
    transition: 'all 0.2s ease',
    position: 'relative',
    background: 'transparent',
    border: 'none',
    boxShadow: 'none',
    '&:hover': {
      backgroundColor: 'rgba(79, 172, 254, 0.08)',
      color: '#4facfe',
      '& $icon': {
        color: '#4facfe',
      },
    },
    '&::before': {
      content: '""',
      position: 'absolute',
      left: 0,
      top: 0,
      bottom: 0,
      width: '3px',
      backgroundColor: 'transparent',
      borderRadius: '5px 0 0 5px',
      transition: 'background-color 0.2s ease',
    },
  },
  icon: {
    color: '#5a6c7d',
    width: 20,
    height: 20,
    display: 'flex',
    alignItems: 'center',
    marginRight: ({ theme }) => theme.spacing(1.5),
    transition: 'color 0.2s ease',
  },
  active: {
    color: '#4facfe',
    fontWeight: 600,
    backgroundColor: 'rgba(79, 172, 254, 0.1)',
    '&::before': {
      backgroundColor: '#4facfe',
    },
    '& $icon': {
      color: '#4facfe',
    },
    '&:hover': {
      backgroundColor: 'rgba(79, 172, 254, 0.12)',
    },
  },
  divider: {
    margin: ({ theme }) => theme.spacing(1, 2),
    backgroundColor: '#e9ecef',
    height: '1px',
  },
});

const CustomRouterLink = forwardRef((props, ref) => (
  <div ref={ref} style={{ flexGrow: 1 }}>
    <RouterLink {...props} />
  </div>
));

const SidebarNav = props => {
  const { pages, className, ...rest } = props;

  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <List {...rest} className={clsx(classes.root, className)}>
      {pages.map((page, index) => (
        <React.Fragment key={page.title}>
          <ListItem
            className={classes.item}
            disableGutters
          >
            <Button
              activeClassName={classes.active}
              className={classes.button}
              component={CustomRouterLink}
              to={page.href}
            >
              <div className={classes.icon}>{page.icon}</div>
              {page.title}
            </Button>
          </ListItem>
          {page.divider && index < pages.length - 1 && (
            <Divider className={classes.divider} />
          )}
        </React.Fragment>
      ))}
    </List>
  );
};

SidebarNav.propTypes = {
  className: PropTypes.string,
  pages: PropTypes.array.isRequired,
};

export default SidebarNav;
