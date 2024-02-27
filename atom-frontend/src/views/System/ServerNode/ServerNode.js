import React, {useContext, useEffect, useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {Grid} from '@material-ui/core';
import {Table, Toolbar} from './components';
import {AppContext} from "adapter";

const useStyles = makeStyles(theme => ({
    root: {
        padding: theme.spacing(2)
    },
    content: {
        marginTop: theme.spacing(2)
    }
}));

const ServerNode = () => {
    const classes = useStyles();
    const {api} = useContext(AppContext);
    const [keyword, setKeyword] = useState('');
    const [limit] = useState(10);
    const [page1, setPage1] = useState(1);
    const [refresh1, setRefresh1] = useState(+new Date());

    const [table1, setTable1] = useState([]);

    useEffect(() => {
        const getData = () => {
            api.listServer({page: page1, pageSize: 1000}).then(res => {
                if (res.status === 0) {
                    setTable1(res.data.records || []);
                }
            })
        }
        getData();
    }, [refresh1, page1, api]);

    const showTable1 = table1.filter(item => {
        return JSON.stringify(item).includes(keyword);
    });

    return (
        <div>
            <Toolbar onInputChange={(k) => {
                setKeyword(k);
                setPage1(1);
            }} setRefresh={(val) => {
                setRefresh1(val);
            }}/>
            <div className={classes.content}>
                <Grid
                    container
                    spacing={4}
                >
                    <Grid
                        item
                        sm={12}
                        xs={12}
                    >
                        <Table
                            data={showTable1.slice((page1 - 1) * limit, page1 * limit)}
                            total={showTable1.length}
                            rowsPerPage={limit}
                            pageState={[page1, setPage1]}
                            changeStatus="serverStatus"
                            setRefresh={setRefresh1}/>
                    </Grid>
                </Grid>
            </div>
        </div>
    );
};

export default ServerNode;
