import React from 'react';
import {UserDashboard,} from './components';
import {Divider} from "@mui/material";
import SetLoginPassword from "./components/SetLoginPassword";
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {
        padding: ({theme}) => theme.spacing(4)
    }
});

const Mine = () => {
    const theme = useTheme();
    const classes = useStyles({theme});

    return (
        <div className={classes.root}>
            <UserDashboard/>
            <Divider/>
            <SetLoginPassword/>
            <Divider/>
        </div>
    );
};

export default Mine;
