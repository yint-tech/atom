import React from 'react';
import { Grid, Typography } from '@mui/material';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';

const useStyles = createUseStyles({
  root: {
    padding: ({ theme }) => theme.spacing(4),
  },
  content: {
    paddingTop: 150,
    textAlign: 'center',
  },
  image: {
    marginTop: 50,
    display: 'inline-block',
    maxWidth: '100%',
    width: 560,
  },
});

const NotFound = () => {
  const theme = useTheme();
  const classes = useStyles({ theme });
  const { t } = useTranslation();

  return (
    <div className={classes.root}>
      <Grid container justifyContent='center' spacing={4}>
        <Grid item lg={6} xs={12}>
          <div className={classes.content}>
            <Typography variant='h1'>{t('errors.pageNotFound')}</Typography>
            <Typography variant='subtitle2'>{t('errors.pleaseOpenCorrectUrl')}</Typography>
            <img
              alt='Under development'
              className={classes.image}
              src='/images/undraw_page_not_found_su7k.svg'
            />
          </div>
        </Grid>
      </Grid>
    </div>
  );
};

export default NotFound;
