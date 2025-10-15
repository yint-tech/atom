import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Card, CardContent, CardHeader, Grid } from '@mui/material';
import PropTypes from 'prop-types';
import Typography from '@mui/material/Typography';
import { AppContext } from 'adapter';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  root: {
  },
  content: {
    marginTop: ({ theme }) => theme.spacing(2),
  },
  card: {
    borderRadius: '8px',
    boxShadow: 'none',
    border: '1px solid #e9ecef',
    marginBottom: ({ theme }) => theme.spacing(2),
  },
  cardHeader: {
    backgroundColor: '#f8f9fa',
    '& .MuiCardHeader-title': {
      fontSize: '14px',
      fontWeight: 600,
      color: '#495057',
    },
  },
  cardContent: {
    padding: ({ theme }) => theme.spacing(2),
  },
  groupButton: {
    border: '1px solid #e9ecef',
    backgroundColor: '#fff3cd',
    color: '#856404',
    marginRight: ({ theme }) => theme.spacing(1),
    marginTop: ({ theme }) => theme.spacing(1),
    textTransform: 'none',
    fontSize: '12px',
    fontWeight: 500,
    borderRadius: '6px',
    padding: ({ theme }) => theme.spacing(0.5, 1),
    '&:hover': {
      backgroundColor: '#ffeaa7',
      borderColor: '#fdcb6e',
    },
  },
  groupButtonActive: {
    border: '1px solid #4facfe',
    backgroundColor: '#4facfe',
    color: '#ffffff',
    marginRight: ({ theme }) => theme.spacing(1),
    marginTop: ({ theme }) => theme.spacing(1),
    textTransform: 'none',
    fontSize: '12px',
    fontWeight: 500,
    borderRadius: '6px',
    padding: ({ theme }) => theme.spacing(0.5, 1),
    '&:hover': {
      backgroundColor: '#74b9ff',
      borderColor: '#0984e3',
    },
  },
  saveButton: {
    fontSize: '14px',
    fontWeight: 500,
    textTransform: 'none',
    borderRadius: '8px',
    padding: ({ theme }) => theme.spacing(1, 2),
    marginTop: ({ theme }) => theme.spacing(2),
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
      <Card className={classes.card}>
        <CardHeader 
          title={t('permissionType')} 
          className={classes.cardHeader}
        />
        <CardContent className={classes.cardContent}>
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
              {item}
            </Button>
          ))}
        </CardContent>
      </Card>
      <Grid className={classes.content} container spacing={2} wrap='wrap'>
        <Grid item xs={6}>
          <Card className={classes.card}>
            <CardHeader 
              title={t('availablePermissions')} 
              className={classes.cardHeader}
            />
            <CardContent className={classes.cardContent}>
              {availablePermItems.map(item => (
                <Button
                  key={item}
                  size='small'
                  onClick={() => {
                    addPerm(item);
                  }}
                  className={classes.groupButton}
                >
                  {item}
                </Button>
              ))}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={6}>
          <Card className={classes.card}>
            <CardHeader 
              title={t('heldPermissions')} 
              className={classes.cardHeader}
            />
            <CardContent className={classes.cardContent}>
              {selectedPermItems.map(item => (
                <Button
                  key={item}
                  size='small'
                  onClick={() => {
                    removePerm(item);
                  }}
                  className={classes.groupButtonActive}
                >
                  {item}
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
