import { HashRouter } from 'react-router-dom';
import { SnackbarProvider } from 'notistack';
import Adapter from './adapter';
import validate from 'validate.js';
import theme from './theme';
import 'react-perfect-scrollbar/dist/css/styles.css';
import './assets/scss/index.scss';
import validators from './common/validators';
import Routes from './Routes';
import { ThemeProvider } from 'react-jss';

validate.validators = {
  ...validate.validators,
  ...validators,
};

function App() {
  return (
    <ThemeProvider theme={theme}>
      <SnackbarProvider maxSnack={3}>
        <Adapter>
          <HashRouter>
            <Routes />
          </HashRouter>
        </Adapter>
      </SnackbarProvider>
    </ThemeProvider>
  );
}

export default App;
