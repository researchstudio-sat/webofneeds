import angular from "angular";
import { createElement } from "react";
import { render, unmountComponentAtNode } from "react-dom";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      component: "<",
      props: "<",
      onAction: "&",
    },
    link: (scope, element) => {
      scope.props.ngRedux = $ngRedux;
      render(createElement(scope.component, scope.props), element[0]);

      scope.$watch("props", props => {
        props.ngRedux = $ngRedux;
        render(createElement(scope.component, props), element[0]);
      });
      scope.$watch("component", component => {
        scope.props.ngRedux = $ngRedux;
        render(createElement(component, scope.props), element[0]);
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
