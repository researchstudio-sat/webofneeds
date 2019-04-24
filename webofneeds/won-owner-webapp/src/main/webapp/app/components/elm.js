import angular from "angular";
import { actionCreators } from "../actions/actions";

import "./svg-icon.js";

import "../../style/_elm.scss";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      module: "<",
      props: "<",
      onAction: "&",
    },
    link: (scope, element) => {
      const childElement = document.createElement("div");
      element[0].appendChild(childElement);
      const elmApp = scope.module.init({
        node: childElement,
        flags: scope.props,
      });

      scope.$watch("props", props => {
        elmApp.ports.inPort.send({
          newProps: props,
        });
      });

      if (elmApp.ports.outPort) {
        elmApp.ports.outPort.subscribe(({ action, payload }) => {
          if (actionCreators[action]) {
            $ngRedux.dispatch(actionCreators[action](...payload));
          } else {
            scope.onAction({ action, payload });
          }
        });
      }

      elmApp.ports.errorPort.subscribe(error => {
        console.error(error);
      });

      scope.$on("$destroy", () => {
        elmApp.ports.inPort.send({
          unmount: true,
        });
        if (elmApp.ports.outPort) {
          elmApp.ports.outPort.unsubscribe();
        }
        elmApp.ports.errorPort.unsubscribe();
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];

export default angular
  .module("won.owner.components.elm", [])
  .directive("wonElm", genComponentConf).name;
