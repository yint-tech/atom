import React from 'react';
import { useTranslation } from 'react-i18next';

const GlobalMetrics = () => {
  const { t } = useTranslation();
  return <p>{t('metrics.todoBusinessDesign')}</p>;
};

export default GlobalMetrics;
