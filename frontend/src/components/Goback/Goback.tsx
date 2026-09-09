import { MouseEvent, useState } from 'react';
import type { ReactNode } from 'react';
import { useHistory } from 'react-router-dom';
import { CardHeader, Grid, IconButton, Popover } from '@mui/material';
import { ArrowBackIos, Dehaze } from '@mui/icons-material';

import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';

interface GobackProps {
  title?: string;
  subheader?: ReactNode;
  extra?: ReactNode;
}

const useStyles = createUseStyles({
  header: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    padding: ({ theme }) => theme.spacing(1),
  },
  backIcon: {
    margin: ({ theme }) => theme.spacing(1, 2, 0, 0),
  },
  headerButton: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingRight: ({ theme }) => theme.spacing(2),
  },
});

const Goback = ({ title, subheader, extra }: GobackProps) => {
  const history = useHistory();
  const theme = useTheme();
  const classes = useStyles({ theme });

  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const handleClick = (event: MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const selfHeader = (
    <CardHeader
      className={classes.header}
      action={
        <IconButton
          onClick={() => history.go(-1)}
          color='primary'
          aria-label='back'
          className={classes.backIcon}
        >
          <ArrowBackIos style={{ fontSize: 20 }} />
        </IconButton>
      }
      title={title}
      subheader={subheader}
    />
  );

  if (extra) {
    return (
      <Grid className={classes.headerButton}>
        {selfHeader}
        <IconButton onClick={handleClick}>
          <Dehaze />
        </IconButton>
        <Popover
          open={Boolean(anchorEl)}
          anchorEl={anchorEl}
          onClose={handleClose}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'center',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'center',
          }}
        >
          {extra}
        </Popover>
      </Grid>
    );
  }
  return selfHeader;
};

export default Goback;
