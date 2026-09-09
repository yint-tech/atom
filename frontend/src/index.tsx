import { createRoot } from 'react-dom/client';

import App from './App';
import './i18n';

// 清理历史版本可能注册过的 service worker，避免缓存干扰
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then(registrations => {
    registrations.forEach(registration => registration.unregister());
  });
}

const container = document.getElementById('root');
if (!container) {
  throw new Error('root element not found');
}
const root = createRoot(container);
root.render(<App />);
