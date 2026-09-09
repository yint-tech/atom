import { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { AppContext } from '../../adapter';
import { BackendPagedTable } from '../../components';
import moment from 'moment/moment';
import { SysLog } from '../../types/api';

const LogPanel = () => {
  const { t } = useTranslation();
  const { api } = useContext(AppContext);
  const [searchParam] = useState({
    key: '',
  });
  return (
    <BackendPagedTable
      loadDataFun={api.logList}
      searchParam={searchParam}
      columns={[
        {
          label: t('system.operator'),
          key: 'username',
        },
        {
          label: t('common.actions'),
          key: 'operation',
        },
        {
          label: t('system.parameters'),
          key: 'params',
        },
        {
          label: t('system.time'),
          render: (item: SysLog) =>
            moment(new Date(item.createTime!)).format('YYYY-MM-DD HH:mm:ss'),
        },
      ]}
    />
  );
};

export default LogPanel;
