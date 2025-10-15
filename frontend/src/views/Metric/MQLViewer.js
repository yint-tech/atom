import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  Divider,
  MenuItem,
  Select,
} from '@mui/material';
import MetricCharsV2 from 'components/MetricCharts';
import CodeMirror from '@uiw/react-codemirror';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';

const useStyles = createUseStyles({
  root: {
    padding: ({ theme }) => theme.spacing(3),
  },
  content: {
    marginTop: ({ theme }) => theme.spacing(2),
  },
  item: {
    marginTop: ({ theme }) => theme.spacing(5),
  },
  tableButton: {
    marginRight: ({ theme }) => theme.spacing(1),
  },
});

const MQLViewer = () => {
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { t } = useTranslation();
  
  const demoMQL = `
# ${t('metrics.load')}
${t('metrics.serverMinuteLoad')} = metric(system.load.average.1m);
show(${t('metrics.serverMinuteLoad')});

${t('metrics.systemCpuUsage')} = metric(system.cpu.usage);
${t('metrics.processCpuUsage')} = metric(process.cpu.usage);
show(${t('metrics.systemCpuUsage')},${t('metrics.processCpuUsage')});
`;

  const [accuracy, setAccuracy] = useState('minutes');

  const [errMsg, setErrMsg] = useState(t('systemMetrics.pleaseEnterMQLScript'));
  const [mql, setMql] = useState('');

  let initMQL = localStorage.getItem('ViewMQLScript') || demoMQL;
  const [editMql, setEditMql] = useState(initMQL);
  useEffect(() => {
    localStorage.setItem('ViewMQLScript', editMql);
  }, [editMql]);

  return (
    <div className={classes.root}>
      <Card>
        <CardHeader
          action={
            <>
              <Select
                className={classes.tableButton}
                style={{ width: '200px', height: '40px', overflow: 'hidden' }}
                variant='outlined'
                value={accuracy}
                onChange={e => {
                  setAccuracy(e.target.value);
                }}
              >
                {['minutes', 'hours', 'days'].map(d => (
                  <MenuItem key={d} value={d}>
                    {d}
                  </MenuItem>
                ))}
              </Select>
              <Button
                className={classes.tableButton}
                variant='contained'
                color='primary'
                onClick={() => {
                  setMql(editMql);
                  setErrMsg('');
                }}
              >
                {t('common.confirm')}
              </Button>
              <Button
                className={classes.tableButton}
                variant='contained'
                color='primary'
                onClick={() => {
                  setEditMql(demoMQL);
                }}
              >
                {t('systemMetrics.loadDefaultMQL')}
              </Button>
            </>
          }
        />
        <CardContent className={classes.content}>
          <CodeMirror
            height='200px'
            value={editMql}
            onChange={value => {
              setEditMql(value);
            }}
          />

          <Divider />
          {!!errMsg ? (
            <CodeMirror height='200px' value={errMsg} />
          ) : (
            <MetricCharsV2
              onLoadMsg={setErrMsg}
              className={classes.marginTop}
              title={t('systemMetrics.mqlDebugMetrics')}
              accuracy={accuracy}
              mql={mql}
            />
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default MQLViewer;
