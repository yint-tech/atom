import React, { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { AppContext } from 'adapter';
import { BackendPagedTable, SearchInput } from 'components';
import { Switch } from '@mui/material';
import { createUseStyles, useTheme } from 'react-jss';

const useStyles = createUseStyles({
  root: {},
  row: {
    height: '42px',
    display: 'flex',
    alignItems: 'center',
    marginTop: ({ theme }) => theme.spacing(1),
  },
  spacer: {
    flexGrow: 1,
  },
  searchInput: {
    marginBottom: ({ theme }) => theme.spacing(2),
  },
});

const SeverNodeList = () => {
  const { t } = useTranslation();
  const theme = useTheme();
  const classes = useStyles({ theme });

  const { api } = useContext(AppContext);
  const [searchParam, setSearchParam] = useState({
    key: '',
  });
  const [refresh, setRefresh] = useState(+new Date());

  const handleChange = item => {
    api.setServerStatus({ id: item.id, enable: !item.enable }).then(res => {
      if (res.status === 0) {
        api.successToast(t('common.operationSuccess'));
        setRefresh(+new Date());
      }
    });
  };

  return (
    <BackendPagedTable
      toolbar={
        <>
          <SearchInput
            className={classes.searchInput}
            onChange={v => {
              setSearchParam({
                ...searchParam,
                key: v,
              });
            }}
            placeholder={t('common.searchPlaceholder')}
          />
          <span className={classes.spacer} />
        </>
      }
      loadDataFun={api.listServer}
      searchParam={searchParam}
      setSearchParam={setSearchParam}
      refresh={refresh}
      columns={[
        {
          label: 'ID',
          key: 'id',
        },
        {
          label: t('system.nodeId'),
          key: 'serverId',
        },
        {
          label: t('system.exitIp'),
          key: 'outIp',
        },
        {
          label: t('system.webPort'),
          render: item => item.port || '-',
        },
        {
          label: t('system.heartbeatTime'),
          key: 'lastActiveTime',
        },
        {
          label: '',
          render: item => (
            <>
              <Switch
                checked={item.enable}
                onChange={() => handleChange(item)}
                color='primary'
                inputProps={{ 'aria-label': 'primary checkbox' }}
              />
            </>
          ),
        },
      ]}
    />
  );
};

export default SeverNodeList;
