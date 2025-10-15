import React, { useCallback, useContext, useState } from 'react';
import { OpeDialog, SimpleTable } from 'components';
import { AppContext } from 'adapter';
import { Button, Grid, TextField, Typography } from '@mui/material';
import {
  DirectionsRailway,
  PermIdentity,
  SupervisorAccount,
} from '@mui/icons-material';
import config from '../../config';
import moment from 'moment/moment';
import { useHistory } from 'react-router-dom';
import EmojiPeopleIcon from '@mui/icons-material/EmojiPeople';
import Permission from './Permission';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';

const LOGIN_USER_MOCK_KEY = config.login_user_key + '-MOCK';

const useStyles = createUseStyles({
  root: {},
  content: {
    padding: 0,
  },
  nameContainer: {
    display: 'flex',
    alignItems: 'center',
  },
  avatar: {
    marginRight: ({ theme }) => theme.spacing(2),
  },
  actions: {
    paddingTop: ({ theme }) => theme.spacing(2),
    paddingBottom: ({ theme }) => theme.spacing(2),
    justifyContent: 'center',
  },
  tableButton: {
    marginRight: ({ theme }) => theme.spacing(1),
  },
  dialogInput: {
    width: '100%',
  },
});

const CreateUserDialog = props => {
  const { openCreateUserDialog, setOpenCreateUserDialog, setRefresh } = props;
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { api } = useContext(AppContext);
  const { t } = useTranslation();
  const [account, setAccount] = useState('');
  const [password, setPassword] = useState('');

  return (
    <OpeDialog
      title={t('userManagement.addUser')}
      opeContent={
        <>
          <Grid container spacing={6} wrap='wrap'>
            <Grid item xs={6}>
              <Typography gutterBottom variant='h6'>
                {t('userManagement.account')}
              </Typography>
              <TextField
                className={classes.dialogInput}
                size='small'
                variant='outlined'
                value={account}
                onChange={e => setAccount(e.target.value)}
              />
            </Grid>
            <Grid item xs={6}>
              <Typography gutterBottom variant='h6'>
                {t('userManagement.password')}
              </Typography>
              <TextField
                className={classes.dialogInput}
                size='small'
                variant='outlined'
                type='password'
                value={password}
                onChange={e => setPassword(e.target.value)}
              />
            </Grid>
          </Grid>
        </>
      }
      openDialog={openCreateUserDialog}
      setOpenDialog={setOpenCreateUserDialog}
      doDialog={() => {
        return api
          .userAdd({
            userName: account,
            password: password,
          })
          .then(res => {
            if (res.status === 0) {
              setRefresh(+new Date());
            }
          });
      }}
      okText={t('common.save')}
      okType='primary'
    />
  );
};

const AccountList = () => {
  const { api, setUser } = useContext(AppContext);
  const history = useHistory();
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { t } = useTranslation();

  const [openCreateUserDialog, setOpenCreateUserDialog] = useState(false);

  const [permOpAccount, setPermOpAccount] = useState({});
  const [showPermOpDialog, setShowPermOpDialog] = useState(false);
  const [refresh, setRefresh] = useState(+new Date());

  const travelToUser = item => {
    api
      .travelToUser({
        id: item.id,
      })
      .then(res => {
        if (res.status === 0) {
          api.setStore({ ...res.data, mock: true }, LOGIN_USER_MOCK_KEY);
          setUser({
            ...res.data,
            mock: true,
            time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss'),
          });
          history.push('/');
        }
      });
  };

  const grantAdmin = item => {
    api
      .grantAdmin({
        userName: item.userName,
        isAdmin: !item.isAdmin,
      })
      .then(res => {
        if (res.status === 0) {
          setRefresh(+new Date());
        }
      });
  };

  const loadApi = useCallback(() => {
    return new Promise((resolve, reject) => {
      api
        .userList({ page: 1, pageSize: 1000 })
        .then(res => {
          if (res.status === 0) {
            resolve({
              data: res.data.records,
              status: 0,
            });
            return;
          }
          reject(res.message);
        })
        .catch(e => {
          reject(e);
        });
    });
  }, [api]);

  return (
    <div>
      <SimpleTable
        refresh={refresh}
        actionEl={
          <Button
            startIcon={<EmojiPeopleIcon />}
            color='primary'
            variant='contained'
            onClick={() => setOpenCreateUserDialog(true)}
          >
            {t('userManagement.addUser')}
          </Button>
        }
        loadDataFun={loadApi}
        columns={[
          {
            label: t('userManagement.account'),
            key: 'userName',
          },
          {
            label: t('userManagement.password'),
            key: 'password',
          },
          {
            label: t('userManagement.admin'),
            render: item => (item.isAdmin ? <p>{t('common.yes')}</p> : <p>{t('common.no')}</p>),
          },
          {
            label: t('common.actions'),
            render: item => (
              <>
                <Button
                  startIcon={<DirectionsRailway style={{ fontSize: 16 }} />}
                  size='small'
                  color='primary'
                  className={classes.tableButton}
                  onClick={() => travelToUser(item)}
                  variant='contained'
                >
                  {t('auth.login')}
                </Button>
                <Button
                  startIcon={<PermIdentity style={{ fontSize: 16 }} />}
                  size='small'
                  color='primary'
                  className={classes.tableButton}
                  onClick={() => {
                    setPermOpAccount(item);
                    setShowPermOpDialog(true);
                  }}
                  variant='contained'
                >
                  {t('userManagement.configurePermissions')}
                </Button>
                <Button
                  startIcon={<SupervisorAccount style={{ fontSize: 16 }} />}
                  size='small'
                  color='primary'
                  className={classes.tableButton}
                  onClick={() => grantAdmin(item)}
                  variant='contained'
                >
                  {item.isAdmin ? t('userManagement.removeAdmin') : t('userManagement.upgradeAdmin')}
                </Button>
              </>
            ),
          },
        ]}
      />

      <CreateUserDialog
        openCreateUserDialog={openCreateUserDialog}
        setOpenCreateUserDialog={setOpenCreateUserDialog}
        setRefresh={setRefresh}
      />

      <OpeDialog
        title={t('userManagement.editPermissions') + ':' + permOpAccount.userName}
        okText={t('common.confirm')}
        openDialog={showPermOpDialog}
        fullScreen
        setOpenDialog={setShowPermOpDialog}
        opeContent={
          <Permission account={permOpAccount} setRefresh={setRefresh} />
        }
      />
    </div>
  );
};

export default AccountList;
