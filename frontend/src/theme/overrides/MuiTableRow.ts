import palette from '../palette';

const tableRowStyles = {
  root: {
    '&$selected': {
      backgroundColor: palette.background.default,
    },
    '&$hover': {
      '&:hover': {
        backgroundColor: palette.background.default,
      },
    },
  },
};

export default tableRowStyles;
