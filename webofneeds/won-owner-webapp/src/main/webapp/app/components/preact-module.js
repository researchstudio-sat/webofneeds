import angular from "angular";
import { createElement } from "react";
import { render, unmountComponentAtNode } from "react-dom";
import { Provider } from "react-redux";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      component: "<",
      props: "<",
      onAction: "&",
    },
    link: (scope, element) => {
      render(
        createElement(Provider, { store: $ngRedux }, [
          createElement(scope.component, scope.props),
        ]),
        element[0]
      );

      scope.$watch("props", props => {
        render(
          createElement(Provider, { store: $ngRedux }, [
            createElement(scope.component, props),
          ]),
          element[0]
        );
      });
      scope.$watch("component", component => {
        render(
          createElement(Provider, { store: $ngRedux }, [
            createElement(component, scope.props),
          ]),
          element[0]
        );
      });
      scope.$on("$destroy", () => {
        unmountComponentAtNode(element[0]);
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];

export default angular
  .module("won.owner.components.preact", [])
  .directive("wonPreact", genComponentConf).name;
