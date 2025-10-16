import React, { useContext, useEffect, useState } from 'react';
import { Link as RouterLink, withRouter } from 'react-router-dom';
import { Button, Grid, Link, TextField, Typography } from '@mui/material';
import { AppContext } from 'adapter';
import moment from 'moment';
import PropTypes from 'prop-types';
import validate from 'validate.js';
import configs from 'config';
import { createUseStyles, useTheme } from 'react-jss';
import config from '../../config';
import { useTranslation } from 'react-i18next';

const getSchema = (t) => ({
  oa: {
    presence: { allowEmpty: false, message: t('errors.cannotBeEmpty') },
    length: {
      maximum: 64,
    },
  },
  password: {
    presence: { allowEmpty: false, message: t('errors.cannotBeEmpty') },
    length: {
      maximum: 128,
    },
  },
});

const useStyles = createUseStyles({
  root: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
    position: 'relative',
    overflow: 'hidden',
    padding: ({ theme }) => theme.spacing(2),
  },
  '@keyframes float': {
    '0%, 100%': { transform: 'translateY(0px)' },
    '50%': { transform: 'translateY(-20px)' },
  },
  loginCard: {
    background: 'rgba(255, 255, 255, 0.95)',
    backdropFilter: 'blur(20px)',
    borderRadius: 20,
    padding: ({ theme }) => theme.spacing(4),
    boxShadow: '0 25px 50px rgba(0, 0, 0, 0.15)',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    width: '100%',
    maxWidth: 450,
    position: 'relative',
    zIndex: 1,
    transition: 'all 0.3s ease',
    '&:hover': {
      boxShadow: '0 35px 60px rgba(0, 0, 0, 0.2)',
    },
  },
  logoContainer: {
    textAlign: 'center',
    marginBottom: ({ theme }) => theme.spacing(3),
  },
  logo: {
    height: 60,
    width: 'auto',
    marginBottom: ({ theme }) => theme.spacing(2),
  },
  form: {
    width: '100%',
  },
  title: {
    marginTop: ({ theme }) => theme.spacing(3),
  },
  socialButtons: {
    marginTop: ({ theme }) => theme.spacing(3),
  },
  socialIcon: {
    marginRight: ({ theme }) => theme.spacing(1),
  },
  sugestion: {
    marginTop: ({ theme }) => theme.spacing(2),
  },

  textField: {
    marginTop: ({ theme }) => theme.spacing(2),
    '& .MuiOutlinedInput-root': {
      borderRadius: '12px',
      '&:hover fieldset': {
        borderColor: '#4facfe',
      },
      '&.Mui-focused fieldset': {
        borderColor: '#4facfe',
      },
    },
  },
  signInButton: {
    margin: ({ theme }) => theme.spacing(2, 0),
    borderRadius: '12px',
    background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    color: 'white',
    fontWeight: 600,
    textTransform: 'none',
    fontSize: '16px',
    padding: ({ theme }) => theme.spacing(1.5),
    '&:hover': {
      background: 'linear-gradient(135deg, #43a3f5 0%, #00d9fe 100%)',
      boxShadow: '0 10px 20px rgba(79, 172, 254, 0.3)',
    },
    '&:disabled': {
      background: '#ccc',
      color: '#666',
    },
  },
});

const SignIn = props => {
  const { history } = props;
  const { setUser, api } = useContext(AppContext);
  const { t } = useTranslation();

  const theme = useTheme();
  const classes = useStyles({ theme });

  const [formState, setFormState] = useState({
    isValid: false,
    values: {},
    touched: {},
    errors: {},
  });

  useEffect(() => {
    const errors = validate(formState.values, getSchema(t));

    setFormState(formState => ({
      ...formState,
      isValid: !errors,
      errors: errors || {},
    }));
  }, [formState.values]);

  const handleChange = event => {
    event.persist();

    setFormState(formState => ({
      ...formState,
      values: {
        ...formState.values,
        [event.target.name]:
          event.target.type === 'checkbox'
            ? event.target.checked
            : event.target.value,
      },
      touched: {
        ...formState.touched,
        [event.target.name]: true,
      },
    }));
  };

  const handleSignIn = event => {
    event.preventDefault();
    if (formState.isValid) {
      api
        .login({
          userName: formState.values.oa,
          password: formState.values.password,
        })
        .then(res => {
          if (res.status === 0) {
            api.setStore(res.data);
            setUser({
              ...res.data,
              time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss'),
            });
            history.push('/');
          }
        });
    }
  };

  const hasError = field =>
    !!(formState.touched[field] && formState.errors[field]);

  return (
    <div className={classes.root}>
      <div className={classes.loginCard}>
        <div className={classes.logoContainer}>
          <img
            className={classes.logo}
            src={process.env.PUBLIC_URL + config.logo_path}
            alt='Logo'
          />
          <Typography variant='h4' component='h1' gutterBottom>
            {configs.app}
          </Typography>
          <Typography color='textSecondary' variant='body1'>
            {t('auth.accountLogin')}
          </Typography>
        </div>
        
        <form className={classes.form} onSubmit={handleSignIn}>
          <TextField
            className={classes.textField}
            error={hasError('oa')}
            fullWidth
            helperText={hasError('oa') ? formState.errors.oa[0] : null}
            label={t('userManagement.account')}
            name='oa'
            onChange={handleChange}
            type='text'
            value={formState.values.oa || ''}
            variant='outlined'
          />
          <TextField
            className={classes.textField}
            error={hasError('password')}
            fullWidth
            helperText={
              hasError('password') ? formState.errors.password[0] : null
            }
            label={t('userManagement.password')}
            name='password'
            onChange={handleChange}
            type='password'
            value={formState.values.password || ''}
            variant='outlined'
          />
          <Button
            className={classes.signInButton}
            color='primary'
            disabled={!formState.isValid}
            fullWidth
            size='large'
            type='submit'
            variant='contained'
          >
            {t('auth.login')}
          </Button>
          <Typography color='textSecondary' variant='body2' align='center'>
            {t('auth.noAccount')}{' '}
            <Link component={RouterLink} to='/sign-up' variant='body2' style={{color: '#4facfe', textDecoration: 'none', fontWeight: 600}}>
              {t('auth.registerNow')}
            </Link>
          </Typography>
        </form>
      </div>
    </div>
  );
};

SignIn.propTypes = {
  history: PropTypes.object,
};

export default withRouter(SignIn);
