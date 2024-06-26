import React from 'react';
import {Grid, Typography} from '@mui/material';
import {createUseStyles, useTheme} from "react-jss";

const useStyles = createUseStyles({
    root: {
        padding: ({theme}) => theme.spacing(4)
    },
    content: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center'
    },
    image: {
        margin: ({theme}) => theme.spacing(4, 0, 2),
        display: 'inline-block',
        width: ({theme}) => theme.spacing(20)
    }
});

const Empty = ({text}) => {
    const theme = useTheme();
    const classes = useStyles({theme});

    return (
        <div className={classes.root}>
            <Grid
                container
                justifyContent="center"
                spacing={4}
            >
                <Grid
                    item
                    lg={6}
                    xs={12}
                >
                    <div className={classes.content}>
                        <img
                            alt="Under development"
                            className={classes.image}
                            src="/images/not_found.png"
                        />
                        <Typography variant="caption">
                            {text}
                        </Typography>
                    </div>
                </Grid>
            </Grid>
        </div>
    );
};

export default Empty;
