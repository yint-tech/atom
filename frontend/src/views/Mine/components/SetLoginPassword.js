import React, { useContext, useState } from 'react';
import {
  Button,
  CardContent,
  CardHeader,
  Divider,
  Grid,
  TextField,
  Box,
  Alert,
} from '@mui/material';
import { createUseStyles, useTheme } from 'react-jss';
import { AppContext } from 'adapter';
import { useTranslation } from 'react-i18next';

const useStyles = createUseStyles({
  root: {
    border: 'none',
    boxShadow: 'none',
  },
  header: {
    marginBottom: ({ theme }) => theme.spacing(1),
    padding: ({ theme }) => theme.spacing(3),
    paddingBottom: ({ theme }) => theme.spacing(2),
    '& .MuiCardHeader-title': {
      fontSize: '1.25rem',
      fontWeight: 600,
      color: '#2c3e50',
    },
  },
  content: {
    padding: ({ theme }) => theme.spacing(0, 3, 3, 3),
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: ({ theme }) => theme.spacing(2.5),
  },
  textField: {
    '& .MuiOutlinedInput-root': {
      borderRadius: '8px',
      fontSize: '14px',
      '& fieldset': {
        borderColor: '#e9ecef',
      },
      '&:hover fieldset': {
        borderColor: '#4facfe',
      },
      '&.Mui-focused fieldset': {
        borderColor: '#4facfe',
        borderWidth: '2px',
      },
    },
    '& .MuiInputLabel-root': {
      fontSize: '14px',
      color: '#6c757d',
      '&.Mui-focused': {
        color: '#4facfe',
      },
    },
  },
  button: {
    alignSelf: 'flex-start',
    borderRadius: '8px',
    padding: ({ theme }) => theme.spacing(0.7, 3),
    fontSize: '14px',
    fontWeight: 600,
    textTransform: 'none',
    background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    color: '#ffffff',
    border: 'none',
    '&:hover': {
      background: 'linear-gradient(135deg, #3b8bfe 0%, #00d4fe 100%)',
    },
    '&:disabled': {
      background: '#e9ecef',
      color: '#6c757d',
    },
  },
  alert: {
    borderRadius: '8px',
    fontSize: '14px',
  },
});

const SetLoginPassword = () => {
  const { api } = useContext(AppContext);
  const { t } = useTranslation();
  const theme = useTheme();
  const classes = useStyles({ theme });
  const [newPassword, setNewPassword] = useState('');
  const [newSecondPassword, setNewSecondPassword] = useState('');

  const saveNewPassword = () => {
    if (newPassword !== newSecondPassword) {
      api.errorToast(t('userDashboard.passwordMismatch'));
      return;
    }
    api
      .updatePassword({
        newPassword: newPassword,
      })
      .then(res => {
        if (res.status === 0) {
          api.successToast(t('common.operationSuccess'));
        }
      });
  };

  return (
    <>
      <CardHeader 
        title={t('userDashboard.changePassword')} 
        className={classes.header}
      />
      <CardContent className={classes.content}>
        <Box className={classes.form}>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                size='small'
                label={t('userDashboard.newPassword')}
                type='password'
                variant='outlined'
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
                className={classes.textField}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                size='small'
                label={t('userDashboard.confirmPassword')}
                type='password'
                variant='outlined'
                value={newSecondPassword}
                onChange={e => setNewSecondPassword(e.target.value)}
                className={classes.textField}
              />
            </Grid>
            <Grid item xs={12} sm={12} md={4}>
              <Button
                variant='contained'
                size='small'
                onClick={saveNewPassword}
                className={classes.button}
                disabled={!newPassword || !newSecondPassword}
              >
                {t('common.apply')}
              </Button>
            </Grid>
          </Grid>
        </Box>
      </CardContent>
    </>
  );
};

export default SetLoginPassword;
