import React, {useContext, useEffect, useState} from 'react';
import {Link as RouterLink, withRouter} from 'react-router-dom';
import {makeStyles} from '@material-ui/styles';
import {Button, Grid, Link, TextField, Typography} from '@material-ui/core';
import {AppContext} from 'adapter';
import moment from 'moment';
import PropTypes from 'prop-types';
import validate from 'validate.js';
import configs from 'config'

const schema = {
    oa: {
        presence: {allowEmpty: false, message: '不能为空'},
        length: {
            maximum: 64
        }
    },
    password: {
        presence: {allowEmpty: false, message: '不能为空'},
        length: {
            maximum: 128
        }
    }
};

const useStyles = makeStyles(theme => ({
    root: {
        backgroundColor: theme.palette.background.default,
        height: '100%'
    },
    grid: {
        height: '100%'
    },
    quoteContainer: {
        [theme.breakpoints.down('md')]: {
            display: 'none'
        }
    },
    quote: {
        backgroundColor: theme.palette.neutral,
        height: '100%',
        width: '100%',
        overflow: 'hidden',
        display: 'flex',
        alignItems: 'center'
    },
    quoteImg: {
        margin: '0 auto',
        height: '500px'
    },
    quoteIframe: {
        width: '100%',
        height: '300px'
    },
    quoteInner: {
        textAlign: 'center',
        flexBasis: '600px'
    },
    quoteText: {
        color: theme.palette.white,
        fontWeight: 300
    },
    name: {
        marginTop: theme.spacing(3),
        color: theme.palette.white
    },
    bio: {
        color: theme.palette.white
    },
    content: {
        height: '100%',
        display: 'flex',
        flexDirection: 'column'
    },
    contentHeader: {
        display: 'flex',
        alignItems: 'center',
        paddingTop: theme.spacing(5),
        paddingBototm: theme.spacing(2),
        paddingLeft: theme.spacing(2),
        paddingRight: theme.spacing(2)
    },
    logoImage: {
        marginLeft: theme.spacing(4)
    },
    contentBody: {
        flexGrow: 1,
        display: 'flex',
        alignItems: 'center',
        [theme.breakpoints.down('md')]: {
            justifyContent: 'center'
        }

    },
    form: {
        paddingLeft: 100,
        paddingRight: 100,
        paddingBottom: 125,
        flexBasis: 700,
        [theme.breakpoints.down('sm')]: {
            paddingLeft: theme.spacing(2),
            paddingRight: theme.spacing(2)
        }
    },
    title: {
        marginTop: theme.spacing(3)
    },
    socialButtons: {
        marginTop: theme.spacing(3)
    },
    socialIcon: {
        marginRight: theme.spacing(1)
    },
    sugestion: {
        marginTop: theme.spacing(2)
    },

    textField: {
        marginTop: theme.spacing(2)
    },
    signInButton: {
        margin: theme.spacing(2, 0)
    }
}));

const SignIn = props => {
    const {history} = props;
    const {setUser, api} = useContext(AppContext);

    const classes = useStyles();

    const [formState, setFormState] = useState({
        isValid: false,
        values: {},
        touched: {},
        errors: {}
    });

    useEffect(() => {
        const errors = validate(formState.values, schema);

        setFormState(formState => ({
            ...formState,
            isValid: !errors,
            errors: errors || {}
        }));
    }, [formState.values]);

    const handleChange = event => {
        event.persist();

        setFormState(formState => ({
            ...formState,
            values: {
                ...formState.values,
                [event.target.name]:
                    event.target.type === 'checkbox'
                        ? event.target.checked
                        : event.target.value
            },
            touched: {
                ...formState.touched,
                [event.target.name]: true
            }
        }));
    };

    const handleSignIn = event => {
        event.preventDefault();
        if (formState.isValid) {
            api.login({
                userName: formState.values.oa,
                password: formState.values.password,
            }).then(res => {
                if (res.status === 0) {
                    api.setStore(res.data);
                    setUser({
                        ...res.data,
                        time: moment(new Date()).format('YYYY-MM-DD HH:mm:ss')
                    });
                    history.push('/');
                }
            });
        }
    };

    const hasError = field =>
        !!(formState.touched[field] && formState.errors[field]);

    return (
        <div className={classes.root}>
            <Grid
                className={classes.grid}
                container
            >
                <Grid
                    className={classes.quoteContainer}
                    item
                    lg={5}
                >
                    <div className={classes.quote}>
                        <img className={classes.quoteImg} src="/images/logos/logo.svg" alt=""/>
                    </div>
                </Grid>
                <Grid
                    className={classes.content}
                    item
                    lg={7}
                >
                    <div className={classes.content}>
                        <div className={classes.contentBody}>
                            <form
                                className={classes.form}
                                onSubmit={handleSignIn}
                            >
                                <Typography className={classes.title}
                                            variant="h2"
                                >
                                    {configs.app}
                                </Typography>
                                <Typography

                                    color="textSecondary"
                                    gutterBottom
                                >
                                    账号登录
                                </Typography>
                                <TextField
                                    className={classes.textField}
                                    error={hasError('oa')}
                                    fullWidth
                                    helperText={
                                        hasError('oa') ? formState.errors.oa[0] : null
                                    }
                                    label="账号"
                                    name="oa"
                                    onChange={handleChange}
                                    type="text"
                                    value={formState.values.oa || ''}
                                    variant="outlined"
                                />
                                <TextField
                                    className={classes.textField}
                                    error={hasError('password')}
                                    fullWidth
                                    helperText={
                                        hasError('password') ? formState.errors.password[0] : null
                                    }
                                    label="密码"
                                    name="password"
                                    onChange={handleChange}
                                    type="password"
                                    value={formState.values.password || ''}
                                    variant="outlined"
                                />
                                <Button
                                    className={classes.signInButton}
                                    color="primary"
                                    disabled={!formState.isValid}
                                    fullWidth
                                    size="large"
                                    type="submit"
                                    variant="contained"
                                >
                                    登录
                                </Button>
                                <Typography
                                    color="textSecondary"
                                    variant="body1"
                                >
                                    没有账号?{' '}
                                    <Link
                                        component={RouterLink}
                                        to="/sign-up"
                                        variant="h6"
                                    >
                                        立即注册
                                    </Link>
                                </Typography>
                            </form>
                        </div>
                    </div>
                </Grid>
            </Grid>
        </div>
    );
};

SignIn.propTypes = {
    history: PropTypes.object
};

export default withRouter(SignIn);
