import angular from "angular";
import { h, render } from "preact";

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
      render(h(scope.component, scope.props), element[0]);

      scope.$watch("props", props => {
        render(h(scope.component, props), element[0]);
      });
      scope.$watch("component", component => {
        render(h(component, scope.props), element[0]);
      });
      scope.$on("$destroy", () => {
        //TODO: DESTROY PREACT IF NECESSARY
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];

export default angular
  .module("won.owner.components.preact", [])
  .directive("wonPreact", genComponentConf).name;
