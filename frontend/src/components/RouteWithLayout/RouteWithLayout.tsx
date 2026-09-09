import * as React from 'react';
import { Route, RouteProps } from 'react-router-dom';
import PropTypes from 'prop-types';

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

RouteWithLayout.propTypes = {
  component: PropTypes.any.isRequired,
  layout: PropTypes.any.isRequired,
  path: PropTypes.string,
};

export default RouteWithLayout;
