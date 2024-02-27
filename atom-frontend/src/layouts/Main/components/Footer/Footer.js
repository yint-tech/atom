import React from 'react';
import PropTypes from 'prop-types';
import clsx from 'clsx';
import {makeStyles} from '@material-ui/styles';
import {Typography} from '@material-ui/core';
import config from 'config'

const useStyles = makeStyles(theme => ({
    root: {
        padding: theme.spacing(4)
    }
}));

const Footer = props => {
    const {className, ...rest} = props;

    const classes = useStyles();

    return (
        <div
            {...rest}
            className={clsx(classes.root, className)}
        >
            <Typography variant="body1">
                {config.footer}
            </Typography>
            <Typography variant="caption">
                &copy; {config.copyRight}
            </Typography>
            <Typography variant="caption">  &nbsp;因体产品
                &nbsp;| <a href="http://majora.iinti.cn/majora-doc" target="_blank" rel="noopener noreferrer">Majora</a>
                &nbsp;| <a href="http://sekiro.iinti.cn/sekiro-doc" target="_blank" rel="noopener noreferrer">Sekiro</a>
                &nbsp;| <a href="https://malenia.iinti.cn/malenia-doc/index.html" target="_blank"
                           rel="noopener noreferrer">Malenia</a>
            </Typography>
        </div>
    );
};

Footer.propTypes = {
    className: PropTypes.string
};

export default Footer;
