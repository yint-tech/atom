import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import { AppContext } from 'adapter';
import { Card, CardContent, Divider, Typography } from '@mui/material';

const BuildInfo = () => {
  const { t } = useTranslation();
  const { systemInfo } = useContext(AppContext);
  const buildInfo = systemInfo.buildInfo;
  return (
    <Card>
      <CardContent>
        <Typography gutterBottom variant='h4'>
          {t('system.buildTime')}
        </Typography>
        <Typography gutterBottom variant='subtitle2'>
          {buildInfo.buildTime}
        </Typography>
        <Divider />
        <Typography gutterBottom variant='h4'>
          {t('system.buildHost')}
        </Typography>
        <Typography gutterBottom variant='subtitle2'>
          {buildInfo.buildUser}
        </Typography>
        <Divider />
        <Typography gutterBottom variant='h4'>
          gitId
        </Typography>
        <Typography gutterBottom variant='subtitle2'>
          {buildInfo.gitId}
        </Typography>

        <Divider />
        <Typography gutterBottom variant='h4'>
          {t('system.version')}
        </Typography>
        <Typography gutterBottom variant='subtitle2'>
          {buildInfo.versionName}
        </Typography>

        <Divider />
        <Typography gutterBottom variant='h4'>
          {t('system.versionNumber')}
        </Typography>
        <Typography gutterBottom variant='subtitle2'>
          {buildInfo.versionCode}
        </Typography>
      </CardContent>
    </Card>
  );
};

export default BuildInfo;
