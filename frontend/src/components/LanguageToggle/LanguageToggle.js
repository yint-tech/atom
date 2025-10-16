import React from 'react';
import { Box, IconButton, Typography } from '@mui/material';
import { createUseStyles, useTheme } from 'react-jss';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

const useStyles = createUseStyles({
  container: {
    display: 'flex',
    alignItems: 'center',
    backgroundColor: ({ theme, variant }) => 
      variant === 'light' ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)',
    borderRadius: 20,
    padding: '4px',
    border: ({ theme, variant }) => 
      variant === 'light' ? '1px solid rgba(255, 255, 255, 0.2)' : '1px solid rgba(0, 0, 0, 0.1)',
  },
  languageButton: {
    minWidth: 40,
    height: 32,
    borderRadius: 16,
    padding: '4px 12px',
    margin: '0 2px',
    transition: 'all 0.3s ease',
    color: ({ theme, variant }) => 
      variant === 'light' ? '#fff' : theme.palette.text.primary,
    '&:hover': {
      backgroundColor: ({ theme, variant }) => 
        variant === 'light' ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)',
    },
  },
  activeButton: {
    backgroundColor: ({ theme, variant }) => 
      variant === 'light' ? 'rgba(255, 255, 255, 0.2)' : theme.palette.primary.main,
    color: ({ theme, variant }) => 
      variant === 'light' ? '#fff' : '#fff',
    '&:hover': {
      backgroundColor: ({ theme, variant }) => 
        variant === 'light' ? 'rgba(255, 255, 255, 0.25)' : theme.palette.primary.dark,
    },
  },
  languageText: {
    fontSize: 12,
    fontWeight: 500,
    textTransform: 'none',
  },
});

const LanguageToggle = ({ variant = 'light' }) => {
  const theme = useTheme();
  const classes = useStyles({ theme, variant });
  const { i18n } = useTranslation();

  const currentLanguage = i18n.language || 'zh-CN';

  const handleLanguageChange = (language) => {
    i18n.changeLanguage(language);
    localStorage.setItem('i18nextLng', language);
  };

  const languages = [
    { code: 'zh-CN', label: '中' },
    { code: 'en-US', label: 'EN' },
  ];

  return (
    <Box className={classes.container}>
      {languages.map((lang) => (
        <IconButton
          key={lang.code}
          className={`${classes.languageButton} ${
            currentLanguage === lang.code ? classes.activeButton : ''
          }`}
          onClick={() => handleLanguageChange(lang.code)}
          size="small"
        >
          <Typography className={classes.languageText}>
            {lang.label}
          </Typography>
        </IconButton>
      ))}
    </Box>
  );
};

LanguageToggle.propTypes = {
  variant: PropTypes.oneOf(['light', 'dark']),
};

export default LanguageToggle;