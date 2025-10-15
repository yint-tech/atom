import React, { useContext } from 'react';
import {
  Alert,
  AlertTitle,
  Button,
  Card,
  CardContent,
  CardHeader,
  Divider,
  Grid,
  IconButton,
  Popover,
  Typography,
} from '@mui/material';
import { AppContext } from 'adapter';
import { useTranslation } from 'react-i18next';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import FileCopyIcon from '@mui/icons-material/FileCopy';
import CachedIcon from '@mui/icons-material/Cached';
import moment from 'moment';
import configs from 'config';
import clsx from 'clsx';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  root: {
    height: '100%',
    border: 'none',
    boxShadow: 'none',
  },
  header: {
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
  tokenContainer: {
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
    padding: ({ theme }) => theme.spacing(2),
    border: '1px solid #e9ecef',
  },
  url: {
    display: 'flex',
    alignItems: 'center',
    fontSize: '14px',
    lineHeight: '1.4em',
    wordBreak: 'break-all',
    fontFamily: 'Monaco, Consolas, "Courier New", monospace',
    color: '#495057',
    backgroundColor: 'transparent',
    padding: 0,
    margin: 0,
  },
  actionButtons: {
    display: 'flex',
    gap: ({ theme }) => theme.spacing(1),
    marginLeft: ({ theme }) => theme.spacing(2),
  },
  iconButton: {
    padding: ({ theme }) => theme.spacing(1),
    borderRadius: '6px',
    backgroundColor: '#ffffff',
    border: '1px solid #dee2e6',
    '&:hover': {
      backgroundColor: '#f8f9fa',
      borderColor: '#4facfe',
    },
    '& .MuiSvgIcon-root': {
      fontSize: '18px',
      color: '#6c757d',
    },
  },
  pop: {
    padding: ({ theme }) => theme.spacing(3),
    maxWidth: '400px',
  },
  popBtns: {
    marginTop: ({ theme }) => theme.spacing(2),
    display: 'flex',
    gap: ({ theme }) => theme.spacing(1),
    justifyContent: 'flex-end',
  },
  alertTitle: {
    fontSize: '16px',
    fontWeight: 600,
  },
  alertContent: {
    fontSize: '14px',
    lineHeight: '1.5',
  },
});

const UserDashboard = props => {
  const { className, ...rest } = props;
  const { user, setUser } = useContext(AppContext);
  const { api } = useContext(AppContext);
  const apiUrl = user.apiToken;
  const { t } = useTranslation();

  const theme = useTheme();
  const classes = useStyles({ theme });

  const [anchorEl, setAnchorEl] = React.useState(null);
  const handleClick = event => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };

  const doRefreshApiToken = () => {
    api.regenerateAPIToken().then(res => {
      if (res.status === 0) {
        let user = api.getStore();
        user.apiToken = res.data.apiToken;
        api.setStore(user);
        setUser({
          ...user,
          time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss'),
        });
      }
    });
  };

  return (
    <Card className={classes.root}>
      <CardHeader
        className={classes.header}
        title="API Token"
        titleTypographyProps={{
          variant: 'h6',
        }}
      />
      <CardContent className={classes.content}>
        <div className={classes.tokenContainer}>
          <div className={classes.url}>
            {apiUrl}
            <div className={classes.actionButtons}>
              <CopyToClipboard
                text={apiUrl}
                onCopy={() => api.successToast(t('common.copySuccess'))}
              >
                <IconButton
                  className={classes.iconButton}
                  title="复制到剪贴板"
                >
                  <FileCopyIcon />
                </IconButton>
              </CopyToClipboard>
              <IconButton 
                className={classes.iconButton}
                onClick={handleClick}
                title="刷新Token"
              >
                <CachedIcon />
              </IconButton>
            </div>
          </div>
        </div>
        
        <Popover
          open={Boolean(anchorEl)}
          anchorEl={anchorEl}
          onClose={handleClose}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'left',
          }}
        >
          <div className={classes.pop}>
            <Alert severity="warning">
              <AlertTitle className={classes.alertTitle}>
                {t('userDashboard.apiTokenRefreshWarning', { appName: configs.app })}
              </AlertTitle>
              <div className={classes.alertContent}>
                {t('userDashboard.apiTokenAdvice')}
              </div>
            </Alert>
            <div className={classes.popBtns}>
              <Button onClick={handleClose} color="primary" variant="outlined">
                {t('common.cancel')}
              </Button>
              <Button 
                onClick={() => {
                  doRefreshApiToken();
                  handleClose();
                }} 
                color="error" 
                variant="contained"
              >
                {t('common.confirm')}
              </Button>
            </div>
          </div>
        </Popover>
      </CardContent>
    </Card>
  );
};

export default UserDashboard;
