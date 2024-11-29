import React, { useContext, useEffect, useState } from 'react';
import { AppContext } from "adapter";
import {
    Card,
    CardActions,
    CardContent,
    Pagination,
} from "@mui/material";
import { createUseStyles, useTheme } from "react-jss";
import Table from "../../components/Table/Table";
import SearchPanel from "../../components/Table/SearchPanel";
import { UserTypes } from "../../common/UserType";

let defaultPageSize = 50;
const useStyles = createUseStyles({
    root: {
        padding: ({ theme }) => theme.spacing(3)
    },
    content: {
        marginTop: ({ theme }) => theme.spacing(2)
    },
    nameContainer: {
        display: "flex",
        alignItems: "center"
    },
    avatar: {
        marginRight: ({ theme }) => theme.spacing(2)
    },
    actions: {
        paddingTop: ({ theme }) => theme.spacing(2),
        paddingBottom: ({ theme }) => theme.spacing(2),
        justifyContent: "center"
    },
    miniMargin: {
        margin: ({ theme }) => `${theme.spacing(2)} 0`
    },
    tableButton: {
        marginRight: ({ theme }) => theme.spacing(1)
    },
    dialogInput: {
        width: "100%"
    }
});

const ExampleList = () => {
    const { api } = useContext(AppContext);
    const theme = useTheme();
    const classes = useStyles({ theme });
    const [refresh, setRefresh] = useState(+new Date());
    const [loading, setLoading] = useState(false);
    const [data, setData] = useState([]);
    const [total, setTotal] = useState(0);

    let initListParam = JSON.parse(localStorage.getItem("ExampleListParam") || "{}");

    const [searchParam, setSearchParam] = useState({
        "userName": "",
        "apiToken": "",
        "lastActive": "",
        ...initListParam
    });
    useEffect(() => {
        localStorage.setItem("ExampleListParam", JSON.stringify(searchParam));
    }, [searchParam])

    const prepareSearchFrom = () => {
        let param = { ...searchParam };
        if ("ALL" === param["userType"]) {
            delete param.userType;
        }
        return param
    }

    const searchFields = [
        [
            {
                label: '用户名',
                fieldName: 'userName',
                placeholder: '用户名称',
                componentType: 'text', // 输入框类型
            },
            {
                label: 'API Token',
                fieldName: 'apiToken',
                placeholder: 'API Token',
                componentType: 'text', // 输入框类型
            }
        ],
        [
            {
                label: '导出',
                fieldName: 'export',
                componentType: 'button',
                onClick: () => {
                    alert('暂未实现');
                }
            },
        ]
    ];

    useEffect(() => {
        setLoading(true);
        api.userList(prepareSearchFrom())
            .then((res) => {
                setData(res.data.records || []);
                setTotal(res.data.total);
            }).finally(() => {
                setLoading(false);
            });
    }, [refresh, searchParam]);

    return (
        <div className={classes.root}>
            <SearchPanel
                searchParam={searchParam}
                setSearchParam={setSearchParam}
                setRefresh={setRefresh}
                title={"列表样例"}
                fields={searchFields}
            />
            <div className={classes.content}>
                <Card>
                    <CardContent className={classes.tableContent}>
                        <Table
                            loading={loading}
                            data={data}
                            columns={[
                                {
                                    label: "用户名",
                                    key: "userName"
                                },
                                {
                                    label: "活跃时间",
                                    key: "lastActive"
                                },
                                {
                                    label: "API Token",
                                    key: "apiToken"
                                }
                            ]}
                        />
                    </CardContent>
                    <CardActions className={classes.actions}>
                        <Pagination
                            count={Math.ceil(total / defaultPageSize) || 1}
                            page={searchParam.page}
                            onChange={(event, page) => {
                                setSearchParam({
                                    ...searchParam,
                                    "page": page
                                })
                                setRefresh(+new Date())
                            }}
                            shape="rounded" />
                    </CardActions>
                </Card>
            </div>
        </div>
    )
}

export default ExampleList;