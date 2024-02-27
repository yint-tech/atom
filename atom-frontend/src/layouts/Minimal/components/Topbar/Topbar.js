import React from 'react';
import {Link as RouterLink} from 'react-router-dom';
import {makeStyles} from '@material-ui/styles';
import {AppBar, Toolbar} from '@material-ui/core';
import clsx from 'clsx';
import PropTypes from 'prop-types';

const useStyles = makeStyles(() => ({
    root: {
        boxShadow: 'none'
    },
    title: {
        fontWeight: 'bold',
        fontSize: 24,
        color: '#fff'
    },
    flexGrow: {
        flexGrow: 1
    }
}));

const Topbar = props => {
    const {className, ...rest} = props;

    const classes = useStyles();

    return (
        <AppBar
            {...rest}
            className={clsx(classes.root, className)}
            color="primary"
            position="fixed"
        >
            <Toolbar>
                <RouterLink to="/">
                    <img
                        alt="Logo"
                        style={{height: 60}}
                        src="/images/logos/logo.svg"
                    />
                </RouterLink>
            </Toolbar>
        </AppBar>
    );
};

Topbar.propTypes = {
    className: PropTypes.string
};

export default Topbar;
