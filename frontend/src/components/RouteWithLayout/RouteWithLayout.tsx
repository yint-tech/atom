import * as React from 'react';
import { Route, RouteProps } from 'react-router-dom';

interface RouteWithLayoutProps extends RouteProps {
  component: React.ComponentType<any>;
  layout: React.ComponentType<any>;
}

const RouteWithLayout = ({
  layout: Layout,
  component: Component,
  ...rest
}: RouteWithLayoutProps) => {
  return (
    <Route
      {...rest}
      render={matchProps => (
        <Layout>
          <Component {...matchProps} />
        </Layout>
      )}
    />
  );
};

export default RouteWithLayout;
