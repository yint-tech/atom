import { ReactNode, useCallback, useEffect, useState } from 'react';

import clsx from 'clsx';
import {
  Card,
  CardActions,
  CardContent,
  CardHeader,
  Input,
  Pagination,
} from '@mui/material';
import Table, { Column } from '../Table/Table';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';
import type { CommonRes, IPage } from '../../types/api';
import type { Query } from '../../apis';

/** 分页请求参数，结构与 apis 内部的 PagedQuery 对齐 */
interface PagedParams {
  page: number;
  pageSize: number;
  [key: string]: unknown;
}

interface DataTableProps {
  className?: string;
  data: unknown[];
  total: number;
  page: number;
  pageSize: number;
  loading: boolean;
  pageChangeFunc: (event: unknown, page: number) => void;
  renderCollapse?: (row: any) => ReactNode;
  columns: Column[];
  title?: ReactNode;
  setRefresh?: (value: number) => void;
}

const useStyles = createUseStyles({
  root: {},
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

const DataTable = (props: DataTableProps) => {
  const {
    className,
    data,
    total,
    page,
    pageSize,
    loading,
    pageChangeFunc,
    renderCollapse,
    columns,
    title,
    ...rest
  } = props;

  const theme = useTheme();
  const classes = useStyles({ theme });

  return (
    <Card className={clsx(classes.tableRoot, className)}>
      {title && <CardHeader title={title} />}
      <CardContent className={classes.tableContent}>
        <Table
          {...rest}
          loading={loading}
          collapse={!!renderCollapse}
          renderCollapse={renderCollapse}
          data={data}
          columns={columns}
        />
      </CardContent>
      <CardActions className={classes.actions}>
        <Pagination
          count={Math.ceil(total / pageSize) || 1}
          page={page}
          onChange={pageChangeFunc}
          shape='rounded'
        />
        <Input
          type='number'
          value={page}
          onChange={e => {
            let max = Math.ceil(total / pageSize) || 1;
            if ((e.target.value as unknown as number) > max) {
              pageChangeFunc(null, max);
            } else if ((e.target.value as unknown as number) < 1) {
              pageChangeFunc(null, 1);
            } else {
              pageChangeFunc(null, e.target.value as unknown as number);
            }
          }}
        />
      </CardActions>
    </Card>
  );
};

interface BackendPagedTableProps {
  loadDataFun: (params: PagedParams) => Promise<CommonRes<IPage<unknown>>>;
  searchParam: Query;
  toolbar?: ReactNode;
  columns: Column[];
  refresh?: number;
  renderCollapse?: (row: any) => ReactNode;
  searchParamBuilder?: (param: any) => Query;
  setSearchParam?: (param: any) => void;
}

const BackendPagedTable = (props: BackendPagedTableProps) => {
  const theme = useTheme();
  const classes = useStyles({ theme });

  const [page, setPage] = useState(1);
  const defaultSearchParamBuilder = useCallback((param: Query) => param, []);

  const {
    loadDataFun,
    searchParam,
    toolbar,
    columns,
    refresh,
    renderCollapse,
    searchParamBuilder = defaultSearchParamBuilder,
    ...rest
  } = props;

  const [loading, setLoading] = useState(false);
  const [records, setRecords] = useState<unknown[]>([]);
  const [total, setTotal] = useState(0);

  const [innerRefresh, setInnerRefresh] = useState(refresh || +new Date());

  useEffect(() => {
    setLoading(true);
    const param = searchParamBuilder(searchParam);
    loadDataFun({
      ...param,
      page: page,
      pageSize: 10,
    })
      .then(res => {
        if (res.status === 0) {
          setRecords(res.data!.records);
          setTotal(res.data!.total);
        }
      })
      .finally(() => {
        setLoading(false);
      });
    // 翻页通过 handlePageChange 中的 innerRefresh 触发重新加载，
    // 这里不能直接依赖 page，否则一次翻页会触发两次请求
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadDataFun, innerRefresh, searchParam, searchParamBuilder]);

  useEffect(() => {
    setInnerRefresh(+new Date());
  }, [refresh]);

  const handlePageChange = (_event: unknown, page: number) => {
    setPage(page);
    setInnerRefresh(+new Date());
  };

  return (
    <div className={classes.root}>
      {toolbar ? <div className={classes.row}>{toolbar}</div> : <></>}
      <div className={classes.content}>
        <DataTable
          {...rest}
          renderCollapse={renderCollapse}
          pageChangeFunc={handlePageChange}
          loading={loading}
          data={records}
          columns={columns}
          total={total}
          page={page}
          pageSize={10}
          setRefresh={setInnerRefresh}
        />
      </div>
    </div>
  );
};

export default BackendPagedTable;
