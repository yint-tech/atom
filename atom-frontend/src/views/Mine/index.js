import React from 'react';
import {makeStyles} from '@material-ui/styles';
import {UserDashboard,} from './components';
import {Divider} from "@material-ui/core";
import SetLoginPassword from "./components/SetLoginPassword";

const useStyles = makeStyles(theme => ({
    root: {
        padding: theme.spacing(4)
    }
}));

const Mine = () => {
    const classes = useStyles();


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
