import React from 'react';
import {SearchInput} from 'components';
import clsx from 'clsx';
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {},
    row: {
        height: '42px',
        display: 'flex',
        alignItems: 'center',
        marginTop: ({theme}) => theme.spacing(1)
    },
    spacer: {
        flexGrow: 1
    },
    importButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
    exportButton: {
        marginRight: ({theme}) => theme.spacing(1)
    },
    searchInput: {},
    dialog: {
        width: ({theme}) => theme.spacing(60)
    },
    dialogInput: {
        width: '100%'
    },
    ml: {
        marginLeft: ({theme}) => theme.spacing(2)
    },
});

const Toolbar = props => {
    const {className, onInputChange, setRefresh, ...rest} = props;

    const theme = useTheme();
    const classes = useStyles({theme});

    return (
        <div
            {...rest}
            className={clsx(classes.root, className)}
        >
            <div className={classes.row}>
                <SearchInput
                    className={classes.searchInput}
                    onChange={(v) => onInputChange(v)}
                    placeholder="请输入关键词进行查询"
                />
                <span className={classes.spacer}/>
            </div>
        </div>
    );
};

export default Toolbar;
