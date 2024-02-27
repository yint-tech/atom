import React, {useContext} from 'react';
import {makeStyles} from '@material-ui/styles';
import {AppContext} from 'adapter';
import Alert from '@material-ui/lab/Alert';
import PropTypes from 'prop-types';
import {Avatar, Typography} from "@material-ui/core";
import {Link as RouterLink} from 'react-router-dom';

const useStyles = makeStyles(theme => ({
    avatar: {
        width: 60,
        height: 60
    },
    user: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: theme.spacing(1),
    },
    line: {
        height: theme.spacing(2),
        marginLeft: theme.spacing(1),
        marginRight: theme.spacing(1)
    },
    name: {
        marginLeft: theme.spacing(1),
    },
    setting: {
        fontSize: 14,
        display: 'flex',
        alignItems: 'center',
    }
}));

const Profile = () => {
    const {user, notice} = useContext(AppContext);
    const classes = useStyles();

    return (
        <div>
            <div className={classes.user}>
                <Avatar
                    className={classes.purple}
                    component={RouterLink}
                    to="/"
                >
                    {user.userName ? user.userName[0] : ''}
                </Avatar>
                <Typography
                    className={classes.name}
                    variant="h3"
                >
                    {user.userName}
                </Typography>
            </div>
            {notice ? (
                <Alert severity="info">{notice}</Alert>
            ) : null}
        </div>
    );
};

Profile.propTypes = {
    className: PropTypes.string
};

export default Profile;
