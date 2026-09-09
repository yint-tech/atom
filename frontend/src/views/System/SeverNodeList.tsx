import { useContext, useState, ComponentType, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import { AppContext } from '../../adapter';
import {
  BackendPagedTable as BackendPagedTableComponent,
  SearchInput as SearchInputComponent,
} from '../../components';
import { Switch } from '@mui/material';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';
import { CommonRes, IPage, ServerNode } from '../../types/api';

/**
 * SearchInput / BackendPagedTable 的 propTypes 未覆盖 placeholder、setSearchParam
 * 等实际使用的属性，这里以类型断言补全视图层用到的 props（纯类型层面，不影响运行时行为）。
 */
interface SearchInputProps {
  className?: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

interface BackendPagedTableProps {
  toolbar?: ReactNode;
  loadDataFun: (
    params: { page: number; pageSize: number; key?: string }
  ) => Promise<CommonRes<IPage<ServerNode>>>;
  searchParam: { key: string };
  setSearchParam: (param: { key: string }) => void;
  refresh?: number;
  columns: {
    label: string;
    key?: string;
    render?: (item: ServerNode) => ReactNode;
  }[];
}

const SearchInput = SearchInputComponent as unknown as ComponentType<SearchInputProps>;
const BackendPagedTable = BackendPagedTableComponent as unknown as ComponentType<BackendPagedTableProps>;

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
    '& .MuiTextField-root': {
      '& .MuiInputBase-input': {
        fontSize: '14px',
      },
      '& .MuiInputLabel-root': {
        fontSize: '14px',
      },
    },
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

  const handleChange = (item: ServerNode) => {
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
          render: (item: ServerNode) => item.port || '-',
        },
        {
          label: t('system.heartbeatTime'),
          key: 'lastActiveTime',
        },
        {
          label: '',
          render: (item: ServerNode) => (
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
