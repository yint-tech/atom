import React from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import Pagination from '@material-ui/lab/Pagination';
import moment from 'moment';
import {makeStyles} from '@material-ui/styles';
import {Table} from "components";
import {Card, CardActions, CardContent,} from '@material-ui/core';

const useStyles = makeStyles(theme => ({
    root: {},
    content: {
        padding: 0
    },
    nameContainer: {
        display: 'flex',
        alignItems: 'center'
    },
    avatar: {
        marginRight: theme.spacing(2)
    },
    actions: {
        paddingTop: theme.spacing(2),
        paddingBottom: theme.spacing(2),
        justifyContent: 'center'
    },
    tableButton: {
        marginRight: theme.spacing(1)
    },
}));

const DataTable = props => {
    const {className, data, total, rowsPerPage, pageState, setRefresh, ...rest} = props;
    const [page, setPage] = pageState;

    const classes = useStyles();

    const handlePageChange = (event, page) => {
        setPage(page);
    };

    return (
        <Card
            {...rest}
            className={clsx(classes.root, className)}
        >
            <CardContent className={classes.content}>
                <Table
                    data={data}
                    columns={[{
                        label: '操作人',
                        key: 'username'
                    }, {
                        label: '操作',
                        key: 'operation'
                    }, {
                        label: '参数',
                        key: 'params'
                    }, {
                        label: '时间',
                        render: (item) => moment(new Date(item.createTime)).format('YYYY-MM-DD HH:mm:ss')
                    },
                    ]}
                />
            </CardContent>
            <CardActions className={classes.actions}>
                <Pagination
                    count={Math.ceil(total / rowsPerPage) || 1}
                    page={page}
                    onChange={handlePageChange}
                    shape="rounded"/>
            </CardActions>
        </Card>
    );
};

DataTable.propTypes = {
    className: PropTypes.string,
    data: PropTypes.array.isRequired
};

export default DataTable;
