import React, {useContext} from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import {Card, CardActions, CardContent, Pagination, Switch} from '@mui/material';
import {Table} from "components";
import {AppContext} from "adapter";
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {},
    content: {
        padding: 0
    },
    nameContainer: {
        display: 'flex',
        alignItems: 'center'
    },
    avatar: {
        marginRight: ({theme}) => theme.spacing(2)
    },
    actions: {
        paddingTop: ({theme}) => theme.spacing(2),
        paddingBottom: ({theme}) => theme.spacing(2),
        justifyContent: 'center'
    },
    tableButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
});

const DataTable = props => {
    const {api} = useContext(AppContext);
    const {className, data, total, rowsPerPage, pageState, setRefresh, ...rest} = props;
    const [page, setPage] = pageState;

    const theme = useTheme();
    const classes = useStyles({theme});

    const handlePageChange = (event, page) => {
        setPage(page);
    };

    const handleChange = (item) => {
        api.setServerStatus({id: item.id, enable: !item.enable})
            .then(res => {
                if (res.status === 0) {
                    api.successToast("操作成功");
                    setRefresh(+new Date());
                }
            })
    }

    return (
        <Card
            {...rest}
            className={clsx(classes.root, className)}
        >
            <CardContent className={classes.content}>
                <Table
                    data={data}
                    columns={[
                        {
                            label: 'ID',
                            key: 'id'
                        }, {
                            label: '节点ID',
                            key: 'serverId'
                        }, {
                            label: '出口',
                            key: 'outIp'
                        }, {
                            label: 'web端口',
                            render: (item) => item.port || '-'
                        }, {
                            label: '心跳时间',
                            key: 'lastActiveTime'
                        }, {
                            label: '',
                            render: (item) => (
                                <>
                                    <Switch
                                        checked={item.enable}
                                        onChange={() => handleChange(item)}
                                        color="primary"
                                        inputProps={{'aria-label': 'primary checkbox'}}
                                    />
                                </>
                            )
                        }
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
