import React from 'react';
import PropTypes from 'prop-types';
import clsx from 'clsx';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../../../common/theme';

const useStyles = createUseStyles({
  root: {
    padding: ({ theme }) => theme.spacing(4),
  },
});

const Footer = (props: React.HTMLAttributes<HTMLDivElement>) => {
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
