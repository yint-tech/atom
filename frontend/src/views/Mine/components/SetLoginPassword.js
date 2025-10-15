import React, { useContext, useState } from 'react';
import {
  Button,
  CardContent,
  CardHeader,
  Divider,
  Grid,
  TextField,
} from '@mui/material';
import { AppContext } from 'adapter';
import { useTranslation } from 'react-i18next';

const SetLoginPassword = () => {
  const { api } = useContext(AppContext);
  const { t } = useTranslation();
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
      <CardHeader title={t('userDashboard.changePassword')} />
      <Divider />
      <CardContent>
        <Grid container spacing={2}>
          <Grid item xs={4}>
            <TextField
              style={{ width: '100%' }}
              size='small'
              label={t('userDashboard.newPassword')}
              type='password'
              variant='outlined'
              value={newPassword}
              onChange={e => setNewPassword(e.target.value)}
            />
          </Grid>
          <Grid item xs={4}>
            <TextField
              style={{ width: '100%' }}
              size='small'
              label={t('userDashboard.confirmPassword')}
              type='password'
              variant='outlined'
              value={newSecondPassword}
              onChange={e => setNewSecondPassword(e.target.value)}
            />
          </Grid>
          <Grid item xs={2}>
            <Button
              fullWidth
              variant='contained'
              color='primary'
              onClick={saveNewPassword}
            >
              {t('common.apply')}
            </Button>
          </Grid>
        </Grid>
      </CardContent>
    </>
  );
};

export default SetLoginPassword;
