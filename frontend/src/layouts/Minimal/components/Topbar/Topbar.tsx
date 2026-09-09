import { Link as RouterLink } from 'react-router-dom';
import { AppBar, Toolbar } from '@mui/material';
import clsx from 'clsx';
import { LanguageToggle } from '../../../../components';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../../../common/theme';
import config, { assetUrl } from '../../../../config';

interface TopbarProps {
  className?: string;
}

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

const Topbar = (props: TopbarProps) => {
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
              src={assetUrl(config.logo_path)}
            />
          </RouterLink>
        </div>
        <div className={classes.flexGrow} />
        <LanguageToggle variant="light" />
      </Toolbar>
    </AppBar>
  );
};

export default Topbar;
