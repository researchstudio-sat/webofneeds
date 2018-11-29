import angular from "angular";
import { Elm } from "../../elm/PublishButton.elm";
import "./svg-icon.js";
import {
  getOwnedPersonas,
  currentSkin,
} from "../selectors/general-selectors.js";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      isValid: "=",
      onPublish: "&",
    },
    link: (scope, element) => {
      const elmApp = Elm.PublishButton.init({
        node: element[0],
        flags: {
          skin: currentSkin(),
          flags: {
            width: window.innerWidth,
            height: window.innerHeight,
          },
        },
      });

      scope.$watch("isValid", newValue => {
        elmApp.ports.publishIn.send({
          draftValid: newValue ? true : false,
          loggedIn: $ngRedux.getState().getIn(["account", "loggedIn"]),
        });
      });

      elmApp.ports.publishIn.send({
        draftValid: scope.isValid ? true : false,
        loggedIn: $ngRedux.getState().getIn(["account", "loggedIn"]),
      });

      const personas = getOwnedPersonas($ngRedux.getState());
      if (personas) {
        elmApp.ports.personaIn.send(personas.toJS());
      }

      const disconnectOptions = $ngRedux.connect(state => {
        return {
          personas: getOwnedPersonas(state),
          loggedIn: state.getIn(["account", "loggedIn"]),
        };
      })(state => {
        elmApp.ports.publishIn.send({
          draftValid: scope.isValid ? true : false,
          loggedIn: state.loggedIn,
        });
        if (!state.personas) {
          return;
        }
        elmApp.ports.personaIn.send(state.personas.toJS());
      });

      const disconnectSkin = $ngRedux.connect(state => {
        return {
          skin: state.getIn(["config", "theme"]),
        };
      })(() => {
        const skin = currentSkin();
        elmApp.ports.skin.send(skin);
      });

      elmApp.ports.publishOut.subscribe(url => {
        scope.onPublish({ persona: url });
      });

      scope.$on("$destroy", () => {
        disconnectOptions();
        disconnectSkin();
        elmApp.ports.publishOut.unsubscribe();
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];
export default angular
  .module("won.owner.components.publishButton", [])
  .directive("wonPublishButton", genComponentConf).name;
