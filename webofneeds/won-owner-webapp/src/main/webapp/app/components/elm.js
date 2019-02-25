import angular from "angular";
import { currentSkin } from "../selectors/general-selectors";
import { actionCreators } from "../actions/actions";

import "../../style/_elm-ui-shim.scss";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      module: "<",
      attributes: "<",
      onAction: "&",
    },
    link: (scope, element) => {
      const childElement = document.createElement("div");
      element[0].appendChild(childElement);
      const elmApp = scope.module.init({
        node: childElement,
        flags: {
          state: $ngRedux.getState().toJS(),
          attributes: scope.attributes,
          style: currentSkin(),
        },
      });

      const disconnectState = $ngRedux.connect(state => ({ state: state }))(
        ({ state }) =>
          window.requestAnimationFrame(() => {
            elmApp.ports.inPort.send({
              newState: state.toJS(),
              newStyle: currentSkin(),
            });
          })
      );

      scope.$watch("attributes", attributes => {
        elmApp.ports.inPort.send({
          newAttributes: attributes,
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

      scope.$on("destroy", () => {
        disconnectState();
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
