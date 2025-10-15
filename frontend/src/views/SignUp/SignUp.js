import React, { useContext, useEffect, useState } from 'react';
import { Link as RouterLink, withRouter } from 'react-router-dom';
import {
  Button,
  Checkbox,
  Grid,
  Link,
  TextField,
  Typography,
} from '@mui/material';
import { AppContext } from 'adapter';
import PropTypes from 'prop-types';
import validate from 'validate.js';
import moment from 'moment';
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
  policy: {
    presence: { allowEmpty: false, message: t('errors.isRequired') },
    checked: true,
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
    '0%, 100%': {
      transform: 'translateY(0px)',
    },
    '50%': {
      transform: 'translateY(-20px)',
    },
  },
  signupCard: {
    background: 'rgba(255, 255, 255, 0.95)',
    backdropFilter: 'blur(20px)',
    borderRadius: '20px',
    padding: ({ theme }) => theme.spacing(4),
    boxShadow: '0 25px 45px rgba(0, 0, 0, 0.1)',
    border: '1px solid rgba(255, 255, 255, 0.2)',
    maxWidth: '450px',
    width: '100%',
    position: 'relative',
    zIndex: 1,
    transition: 'all 0.3s ease',
    '&:hover': {
      boxShadow: '0 35px 55px rgba(0, 0, 0, 0.15)',
    },
  },
  logoContainer: {
    display: 'flex',
    justifyContent: 'center',
    marginBottom: ({ theme }) => theme.spacing(3),
  },
  logo: {
    height: '60px',
    width: 'auto',
  },
  title: {
    textAlign: 'center',
    marginBottom: ({ theme }) => theme.spacing(1),
    color: '#333',
    fontWeight: 600,
  },
  subtitle: {
    textAlign: 'center',
    marginBottom: ({ theme }) => theme.spacing(3),
    color: '#666',
  },
  textField: {
    marginBottom: ({ theme }) => theme.spacing(2),
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
  policy: {
    marginBottom: ({ theme }) => theme.spacing(2),
    display: 'flex',
    alignItems: 'flex-start',
  },
  policyCheckbox: {
    marginLeft: '-9px',
    marginTop: '-9px',
    color: '#4facfe',
    '&.Mui-checked': {
      color: '#4facfe',
    },
  },
  policyText: {
    marginTop: '2px',
    marginLeft: ({ theme }) => theme.spacing(1),
    fontSize: '0.875rem',
    lineHeight: 1.4,
  },
  signInButton: {
    marginBottom: ({ theme }) => theme.spacing(2),
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
  loginLink: {
    textAlign: 'center',
    '& a': {
      color: '#4facfe',
      textDecoration: 'none',
      fontWeight: 600,
      '&:hover': {
        textDecoration: 'underline',
      },
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
  }, [formState.values, t]);

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
        .register({
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
      <div className={classes.signupCard}>
        <div className={classes.logoContainer}>
          <img
            className={classes.logo}
            src={process.env.PUBLIC_URL + config.logo_path}
            alt=''
          />
        </div>
        <form onSubmit={handleSignIn}>
          <Typography className={classes.title} variant='h4'>
            {configs.app}
          </Typography>
          <Typography className={classes.subtitle} color='textSecondary'>
            {t('auth.accountRegister')}
          </Typography>
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
          <div className={classes.policy}>
            <Checkbox
              checked={formState.values.policy || false}
              className={classes.policyCheckbox}
              color='primary'
              name='policy'
              onChange={handleChange}
            />
            <Typography
              className={classes.policyText}
              color='textSecondary'
              variant='body2'
            >
              {t('auth.agreeToTerms')}{' '}
              <Link
                color='primary'
                component={RouterLink}
                to='#'
                underline='always'
                variant='body2'
              >
                {t('auth.userAgreement', { appName: configs.app })}
              </Link>
            </Typography>
          </div>
          <Button
            className={classes.signInButton}
            disabled={!formState.isValid}
            fullWidth
            size='large'
            type='submit'
            variant='contained'
          >
            {t('auth.register')}
          </Button>
          <Typography className={classes.loginLink} color='textSecondary' variant='body2'>
            {t('auth.hasAccount')}{' '}
            <Link component={RouterLink} to='/sign-in'>
              {t('auth.goToLogin')}
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
