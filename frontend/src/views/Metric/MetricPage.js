import React, { useState } from 'react';
import { Card, CardContent, CardHeader, MenuItem, Select } from '@mui/material';
import MetricCharsV2 from 'components/MetricCharts';
import PropTypes from 'prop-types';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  card: {
    borderRadius: '12px',
    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)',
    border: '1px solid rgba(0, 0, 0, 0.06)',
  },
  cardHeader: {
    '& .MuiCardHeader-title': {
      fontSize: '16px',
      fontWeight: 600,
      color: '#1a1a1a',
    },
  },
  select: {
    width: '200px',
    height: '40px',
    '& .MuiSelect-select': {
      fontSize: '14px',
      padding: ({ theme }) => theme.spacing(1, 1.5),
    },
    '& .MuiOutlinedInput-notchedOutline': {
      borderRadius: '8px',
    },
  },
  item: {
    marginTop: ({ theme }) => theme.spacing(3),
  },
});

const MetricPage = props => {
  const { configs, bottomLegend, ...rest } = props;
  const theme = useTheme();
  const classes = useStyles({ theme });
  const [accuracy, setAccuracy] = useState('hours');
  return (
    <Card {...rest} className={classes.card}>
      <CardHeader
        className={classes.cardHeader}
        action={
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
        }
      />
      <CardContent>
        {configs.map(config => (
          <MetricCharsV2
            className={classes.item}
            key={config.title}
            title={config.title}
            accuracy={accuracy}
            bottomLegend={bottomLegend || config['bottomLegend']}
            mql={config.mql}
          />
        ))}
      </CardContent>
    </Card>
  );
};

MetricPage.propTypes = {
  configs: PropTypes.array.isRequired,
  bottomLegend: PropTypes.bool,
};

export default MetricPage;
