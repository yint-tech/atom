import React from 'react';
import PropTypes from 'prop-types';
import clsx from 'clsx';
import { Typography } from '@mui/material';
import config from 'config';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';

const useStyles = createUseStyles({
  root: {
    padding: ({ theme }) => theme.spacing(4),
  },
});

const Footer = props => {
  const { className, ...rest } = props;

  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <div {...rest} className={clsx(classes.root, className)}>
    </div>
  );
};

Footer.propTypes = {
  className: PropTypes.string,
};

export default Footer;
