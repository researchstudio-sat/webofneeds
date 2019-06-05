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
        elmApp.ports.outPort.subscribe(message => {
          switch (message.type) {
            case "action":
              if (actionCreators[message.name]) {
                $ngRedux.dispatch(
                  actionCreators[message.name](...message.arguments)
                );
              } else {
                console.error(`Could not find action "${message.name}"`);
              }
              break;
            case "event": {
              const eventAttrName = message.name
                .replace(/([A-Z])/g, "-$1")
                .toLowerCase();
              if (element[0].hasAttribute(eventAttrName)) {
                scope.$parent.$eval(
                  element[0].getAttribute(eventAttrName),
                  message.payload
                );
              } else {
                console.error(
                  `Could not find attribute ${eventAttrName}`,
                  element[0]
                );
              }
              break;
            }
            default:
              console.error(`Could not read message "${message}"`);
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
