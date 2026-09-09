import React, { useContext } from 'react';
import { Redirect, Switch } from 'react-router-dom';

import { RouteWithLayout } from './components';
import { Main as MainLayout, Minimal as MinimalLayout } from './layouts';
import loadable from '@loadable/component';
import { AppContext } from 'adapter';

const AccountView = loadable(() => import('./views/Account'));
const MineView = loadable(() => import('./views/Mine'));
const NotFoundView = loadable(() => import('./views/NotFound'));
const SignInView = loadable(() => import('./views/SignIn'));
const SignUpView = loadable(() => import('./views/SignUp'));
const SystemView = loadable(() => import('./views/System'));
const MetricsView = loadable(() => import('./views/Metric'));
// custom

const PrivateRoute = ({ ...rest }) => {
  const { user } = useContext(AppContext);
  // adapter 在首次登录态刷新完成后才会渲染路由，此处 user.id 即真实的登录状态
  return user.id ? (
    <RouteWithLayout {...rest} />
  ) : (
    <Redirect
      to={{
        pathname: '/sign-in',
      }}
    />
  );
};

const Routes = () => {
  return (
    <Switch>
      <Redirect exact from='/' to='/mine' />
      <PrivateRoute
        component={AccountView}
        exact
        layout={MainLayout}
        path='/accountList'
      />
      <PrivateRoute
        component={MineView}
        exact
        layout={MainLayout}
        path='/mine'
      />
      <PrivateRoute
        component={SystemView}
        exact
        layout={MainLayout}
        path='/systemSettings'
      />

      <RouteWithLayout
        component={SignInView}
        exact
        layout={MinimalLayout}
        path='/sign-in'
      />
      <RouteWithLayout
        component={SignUpView}
        exact
        layout={MinimalLayout}
        path='/sign-up'
      />
      <RouteWithLayout
        component={NotFoundView}
        exact
        layout={MinimalLayout}
        path='/not-found'
      />
      <PrivateRoute
        component={MetricsView}
        exact
        layout={MainLayout}
        path='/metrics'
      />
      {/* custom begin */}
      <Redirect to='/not-found' />
    </Switch>
  );
};

export default Routes;
