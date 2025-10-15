import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Card, CardContent, CardHeader, Grid } from '@mui/material';
import PropTypes from 'prop-types';
import Typography from '@mui/material/Typography';
import { AppContext } from 'adapter';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  root: {
    padding: ({ theme }) => theme.spacing(2),
  },
  content: {
    marginTop: ({ theme }) => theme.spacing(2),
  },
  groupButton: {
    border: '1px dashed #f0f0f0',
    backgroundColor: '#F3AD21FF',
    marginRight: ({ theme }) => theme.spacing(1),
    marginTop: ({ theme }) => theme.spacing(1),
    textTransform: 'none',
  },
  groupButtonActive: {
    border: '1px dashed #2196f3',
    backgroundColor: '#2196f3',
    marginRight: ({ theme }) => theme.spacing(1),
    marginTop: ({ theme }) => theme.spacing(1),
    textTransform: 'none',
  },
});

const parsePermsExp = exp => {
  let scope;
  let ret = [];
  exp.split('\n').forEach(line => {
    if (!line) {
      return;
    }
    line.split(':').forEach((item, index) => {
      if (index === 0) {
        scope = item;
      } else {
        ret.push(scope + ':' + item);
      }
    });
  });

  ret.sort();
  return ret;
};

function Permission(props) {
  const { t } = useTranslation();
  const { api } = useContext(AppContext);
  const { account, setRefresh } = props;
  const [permScopes, setPermScopes] = useState([]);
  const [selectedPermScope, setSelectedPermScope] = useState('');

  const [allPermItems, setAllPermItems] = useState([]);
  const [selectedPermItems, setSelectedPermItems] = useState([]);
  const [availablePermItems, setAvailablePermItems] = useState([]);

  const [userPermsExp, setUserPermExp] = useState('');
  const [userHoldPerms, setUserHolePerms] = useState([]);
  const theme = useTheme();
  const classes = useStyles({ theme });

  useEffect(() => {
    api.permScopes({}).then(res => {
      if (res.status === 0) {
        setPermScopes(res.data);
      }
    });
    setUserPermExp(account.permission);
  }, [account, api]);

  useEffect(() => {
    if (!selectedPermScope) {
      setAllPermItems([]);
      return;
    }
    api.permItemsOfScope({ scope: selectedPermScope }).then(res => {
      if (res.status === 0) {
        setAllPermItems(res.data);
      }
    });
  }, [api, selectedPermScope]);

  useEffect(() => {
    setSelectedPermItems(
      allPermItems.filter(it => {
        return userHoldPerms.indexOf(selectedPermScope + ':' + it) >= 0;
      })
    );
    setAvailablePermItems(
      allPermItems.filter(it => {
        return userHoldPerms.indexOf(selectedPermScope + ':' + it) < 0;
      })
    );
  }, [allPermItems, userHoldPerms, selectedPermScope]);

  useEffect(() => {
    setUserHolePerms(parsePermsExp(userPermsExp));
  }, [userPermsExp]);

  const editPerm = permItems => {
    api
      .editUserPerm({
        userName: account.userName,
        permsConfig: permItems.join('\n'),
      })
      .then(res => {
        if (res.status === 0) {
          setRefresh(+new Date());
          setUserPermExp(res.data.permission);
        }
      });
  };

  const addPerm = permItem => {
    editPerm([...userHoldPerms, selectedPermScope + ':' + permItem]);
  };

  const removePerm = permItem => {
    editPerm(
      userHoldPerms.filter(it => it !== selectedPermScope + ':' + permItem)
    );
  };

  return (
    <div className={classes.root}>
      <Card>
        <CardHeader title={t('permissionType')} />
        <CardContent>
          {permScopes.map(item => (
            <Button
              key={item}
              size='small'
              onClick={() => {
                setSelectedPermScope(item);
              }}
              className={
                item === selectedPermScope
                  ? classes.groupButtonActive
                  : classes.groupButton
              }
            >
              <Typography
                variant='subtitle2'
                style={{
                  color: item === selectedPermScope ? '#fff' : '#546e7a',
                }}
              >
                {item}
              </Typography>
            </Button>
          ))}
        </CardContent>
      </Card>
      <Grid className={classes.content} container spacing={1} wrap='wrap'>
        <Grid item xs={6}>
          <Card>
            <CardHeader title={t('availablePermissions')} />
            <CardContent>
              {availablePermItems.map(item => (
                <Button
                  key={item}
                  size='small'
                  onClick={() => {
                    addPerm(item);
                  }}
                  className={classes.groupButtonActive}
                >
                  <Typography variant='subtitle2' style={{ color: '#0ff' }}>
                    {item}
                  </Typography>
                </Button>
              ))}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={6}>
          <Card>
            <CardHeader title={t('heldPermissions')} />
            <CardContent>
              {selectedPermItems.map(item => (
                <Button
                  key={item}
                  size='small'
                  onClick={() => {
                    removePerm(item);
                  }}
                  className={classes.groupButton}
                >
                  <Typography variant='subtitle2' style={{ color: '#546e7a' }}>
                    {item}
                  </Typography>
                </Button>
              ))}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </div>
  );
}

Permission.propTypes = {
  account: PropTypes.object.isRequired,
  setRefresh: PropTypes.func.isRequired,
};

export default Permission;
