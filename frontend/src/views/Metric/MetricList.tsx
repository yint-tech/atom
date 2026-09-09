import { tokens } from '../../theme/tokens';
import { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AppContext } from '../../adapter';
import { MetricChart, OpeDialog, SimpleTable } from '../../components';
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  MenuItem,
  Select,
  Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import DetailsIcon from '@mui/icons-material/Details';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';
import { MetricTag } from '../../types/api';

/**
 * MetricChart / OpeDialog 的 propTypes 未覆盖 title、className、doDialog 等
 * 实际使用的属性，这里以类型断言补全视图层用到的 props（纯类型层面，不影响运行时行为）。
 */

const useStyles = createUseStyles({
  root: {
    padding: 0,
  },
  card: {
    borderRadius: '12px',
    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)',
    border: '1px solid rgba(0, 0, 0, 0.06)',
  },
  cardHeader: {
    '& .MuiCardHeader-title': {
      fontSize: '16px',
      fontWeight: 600,
      color: tokens.textDark,
    },
  },
  content: {
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
    fontSize: '14px',
    fontWeight: 500,
    textTransform: 'none',
    borderRadius: '8px',
    padding: ({ theme }) => theme.spacing(0.5, 1.5),
  },
  groupButton: {
    border: '1px solid #e0e0e0',
    backgroundColor: '#fff',
    color: tokens.textMuted,
    marginRight: ({ theme }) => theme.spacing(1),
    marginBottom: ({ theme }) => theme.spacing(1),
    fontSize: '14px',
    fontWeight: 500,
    textTransform: 'none',
    borderRadius: '8px',
    padding: ({ theme }) => theme.spacing(0.5, 1.5),
    '&:hover': {
      backgroundColor: '#f5f5f5',
      borderColor: '#d0d0d0',
    },
  },
  groupButtonActive: {
    border: `1px solid ${tokens.primary}`,
    backgroundColor: tokens.primary,
    color: '#fff',
    marginRight: ({ theme }) => theme.spacing(1),
    marginBottom: ({ theme }) => theme.spacing(1),
    fontSize: '14px',
    fontWeight: 500,
    textTransform: 'none',
    borderRadius: '8px',
    padding: ({ theme }) => theme.spacing(0.5, 1.5),
    '&:hover': {
      backgroundColor: tokens.primaryDark,
    },
  },
  select: {
    width: '200px',
    height: '40px',
    marginRight: ({ theme }) => theme.spacing(1),
    '& .MuiSelect-select': {
      fontSize: '14px',
      padding: ({ theme }) => theme.spacing(1, 1.5),
    },
    '& .MuiOutlinedInput-notchedOutline': {
      borderRadius: '8px',
    },
  },
});

const MetricChartPanel = (props: { showMetric: MetricTag; height?: string }) => {
  const { showMetric, height } = props;
  const theme = useTheme();
  const classes = useStyles({ theme });
  const [accuracy, setAccuracy] = useState('minutes');
  const [aggregateTags, setAggregateTags] = useState<string[]>([]);
  const [availableTags, setAvailableTags] = useState<string[]>([]);

  useEffect(() => {
    setAggregateTags([]);
    setAvailableTags(
      (function (...vals: (string | undefined)[]) {
        let av: string[] = [];
        for (let val of vals) {
          if (val) {
            av.push(val);
          }
        }
        return av;
      })(
        showMetric.tag1Name,
        showMetric.tag2Name,
        showMetric.tag3Name,
        showMetric.tag4Name,
        showMetric.tag5Name
      )
    );
  }, [showMetric]);

  return (
    <Card className={classes.card}>
      <CardHeader
        className={classes.cardHeader}
        title={showMetric.name}
        action={
          <>
            {availableTags.map(item => (
              <Button
                key={item}
                size='small'
                onClick={() => {
                  if (aggregateTags.includes(item)) {
                    setAggregateTags(
                      [...aggregateTags].filter(f => f !== item)
                    );
                  } else {
                    setAggregateTags([...aggregateTags, item]);
                  }
                }}
                className={
                  aggregateTags.includes(item)
                    ? classes.groupButtonActive
                    : classes.groupButton
                }
              >
                <Typography
                  variant='subtitle2'
                  style={{
                    color: aggregateTags.includes(item) ? '#fff' : tokens.textMuted,
                  }}
                >
                  {item}
                </Typography>
              </Button>
            ))}
            <Select
              className={classes.select}
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
          </>
        }
      />
      <CardContent className={classes.content}>
        <MetricChart
          height={height}
          title={showMetric.name}
          mql={
            aggregateTags.length > 0
              ? `var = metric('${showMetric.name}'); 
                     var = aggregate(var,${aggregateTags.map(it => "'" + it + "'").join(',')}); 
                     show(var); `
              : `var = metric('${showMetric.name}'); show(var); `
          }
          bottomLegend
          accuracy={accuracy}
        />
      </CardContent>
    </Card>
  );
};

const MetricList = () => {
  const { t } = useTranslation();
  const { api } = useContext(AppContext);
  const theme = useTheme();
  const classes = useStyles({ theme });
  const [openDialog, setOpenDialog] = useState(false);
  const [showMetric, setShowMetric] = useState<MetricTag>({} as MetricTag);
  const [refresh, setRefresh] = useState(+new Date());

  const [confirmDelete, setConfirmDelete] = useState('');
  const [openDeleteConfirmDialog, setOpenDeleteConfirmDialog] = useState(false);

  const deleteMetric = (name: string) => {
    return api
      .deleteMetric({
        metricName: name,
      })
      .then(res => {
        if (res.status === 0) {
          api.successToast(t('common.operationSuccess'));
          setRefresh(+new Date());
        }
      });
  };

  return (
    <SimpleTable
      refresh={refresh}
      actionEl={
        <>
          <OpeDialog
            fullWidth
            maxWidth={'lg'}
            title={t('metrics.viewMetric') + showMetric.name}
            opeContent={
              <MetricChartPanel showMetric={showMetric} height={'500px'} />
            }
            openDialog={openDialog}
            setOpenDialog={setOpenDialog}
            okText={t('common.confirm')}
            okType='primary'
          />
          <OpeDialog
            title={t('metrics.confirmDeleteMetric')}
            opeContent={
              <>
                <Typography gutterBottom variant='h6'>
                  {t('metrics.deleteWarning')}
                </Typography>
                {confirmDelete}
              </>
            }
            doDialog={() => {
              return deleteMetric(confirmDelete);
            }}
            openDialog={openDeleteConfirmDialog}
            setOpenDialog={setOpenDeleteConfirmDialog}
            okText={t('common.confirm')}
            okType='primary'
          />
        </>
      }
      columns={[
        {
          label: t('metrics.metricName'),
          key: 'name',
        },
        {
          label: 'tag1',
          key: 'tag1Name',
        },
        {
          label: 'tag2',
          key: 'tag2Name',
        },
        {
          label: 'tag3',
          key: 'tag3Name',
        },
        {
          label: 'tag4',
          key: 'tag4Name',
        },
        {
          label: 'tag5',
          key: 'tag5Name',
        },
        {
          label: t('common.actions'),
          render: (item: MetricTag) => (
            <>
              <Button
                className={classes.tableButton}
                size='small'
                variant='outlined'
                onClick={() => {
                  setConfirmDelete(item.name);
                  setOpenDeleteConfirmDialog(true);
                }}
              >
                <DeleteIcon />
                {t('common.delete')}
              </Button>
              <Button
                className={classes.tableButton}
                size='small'
                variant='outlined'
                onClick={() => {
                  setShowMetric(item);
                  setOpenDialog(true);
                }}
              >
                <DetailsIcon />
                {t('metrics.viewMetric')}
              </Button>
            </>
          ),
        },
      ]}
      loadDataFun={api.allMetricConfig}
    />
  );
};

export default MetricList;
