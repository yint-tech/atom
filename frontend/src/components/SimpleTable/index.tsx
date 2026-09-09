import { ReactNode, useEffect, useState } from 'react';

import clsx from 'clsx';
import { Loading, SearchInput } from '../index';
import { Card, CardActions, CardContent, Pagination } from '@mui/material';
import Table, { Column } from '../Table/Table';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';
import type { CommonRes, IPage } from '../../types/api';

const useStyles = createUseStyles({
  root: {
    padding: ({ theme }) => theme.spacing(3),
  },
  content: {
    marginTop: ({ theme }) => theme.spacing(2),
  },
  row: {
    height: '100%',
    display: 'flex',
    alignItems: 'center',
    flexWrap: 'wrap',
    marginTop: ({ theme }) => theme.spacing(1),
  },
  spacer: {
    flexGrow: 1,
  },
  tableRoot: {},
  tableContent: {
    padding: 0,
  },
  searchInput: {
    marginBottom: ({ theme }) => theme.spacing(2),
  },
  actions: {
    paddingTop: ({ theme }) => theme.spacing(2),
    paddingBottom: ({ theme }) => theme.spacing(2),
    justifyContent: 'center',
  },
});

interface ToolbarProps {
  onInputChange: (value: string) => void;
  ActionEl?: ReactNode;
}

const Toolbar = (props: ToolbarProps) => {
  const { onInputChange, ActionEl } = props;

  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <div className={classes.row}>
      <SearchInput
        className={classes.searchInput}
        onChange={v => onInputChange(v)}
        placeholder='请输入关键词进行查询'
      />
      <span className={classes.spacer} />
      {ActionEl ? ActionEl : <></>}
    </div>
  );
};

interface DataTableProps<T> {
  className?: string;
  data: T[];
  total: number;
  rowsPerPage: number;
  pageState: [number, (page: number) => void];
  columns: Column<T>[];
  renderCollapse?: (row: T) => ReactNode;
}

const DataTable = <T,>(props: DataTableProps<T>) => {
  const {
    className,
    data,
    total,
    rowsPerPage,
    pageState,
    columns,
    renderCollapse,
  } = props;
  const [page, setPage] = pageState;

  const theme = useTheme();
  const classes = useStyles({ theme });

  const handlePageChange = (_event: unknown, page: number) => {
    setPage(page);
  };

  return (
    <Card className={clsx(classes.tableRoot, className)}>
      <CardContent className={classes.tableContent}>
        <Table
          collapse={!!renderCollapse}
          renderCollapse={renderCollapse}
          data={data}
          columns={columns}
        />
      </CardContent>
      <CardActions className={classes.actions}>
        <Pagination
          count={Math.ceil(total / rowsPerPage) || 1}
          page={page}
          onChange={handlePageChange}
          shape='rounded'
        />
      </CardActions>
    </Card>
  );
};

interface SimpleTableProps<T = any> {
  /**
   * 加载数据的异步函数。data 兼容两种返回：
   * - T[]：全部数据由组件做客户端分页与关键词过滤
   * - IPage<T>：取 records 后同样做客户端分页
   */
  loadDataFun: () => Promise<CommonRes<T[] | IPage<T>>>;
  actionEl?: ReactNode;
  columns: Column<T>[];
  refresh?: number;
  renderCollapse?: (row: T) => ReactNode;
}

const SimpleTable = <T,>(props: SimpleTableProps<T>) => {
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { loadDataFun, actionEl, columns, refresh, renderCollapse } = props;

  const [data, setData] = useState<T[]>([]);
  const [page, setPage] = useState(1);
  const [limit] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [innerRefresh, setInnerRefresh] = useState(refresh || +new Date());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    loadDataFun()
      .then(res => {
        if (res.status === 0) {
          setData(Array.isArray(res.data) ? res.data : (res.data?.records ?? []));
        }
      })
      .finally(() => {
        setLoading(false);
      });
  }, [loadDataFun, innerRefresh]);

  useEffect(() => {
    setInnerRefresh(+new Date());
  }, [refresh]);

  const showData = data.filter(item => {
    return JSON.stringify(item).includes(keyword);
  });

  return (
    <div className={classes.root}>
      <Toolbar
        onInputChange={k => {
          setKeyword(k);
          setPage(1);
        }}
        ActionEl={actionEl}
      />
      <div className={classes.content}>
        {loading ? (
          <Loading />
        ) : (
          <DataTable<T>
            renderCollapse={renderCollapse}
            data={showData.slice((page - 1) * limit, page * limit)}
            total={showData.length}
            columns={columns}
            rowsPerPage={limit}
            pageState={[page, setPage]}
          />
        )}
      </div>
    </div>
  );
};

export default SimpleTable;
