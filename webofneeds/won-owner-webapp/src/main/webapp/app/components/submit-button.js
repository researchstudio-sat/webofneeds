import angular from "angular";
import { Elm } from "../../elm/SubmitButton.elm";
import "./svg-icon.js";
import {
  getOwnedPersonas,
  currentSkin,
} from "../selectors/general-selectors.js";
import { get } from "../utils.js";
import * as accountUtils from "../account-utils.js";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      isValid: "=",
      showPersonas: "=",
      label: "=",
      onSubmit: "&",
    },
    link: (scope, element) => {
      const elmApp = Elm.SubmitButton.init({
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
          isValid: newValue ? true : false,
          loggedIn: accountUtils.isLoggedIn(
            get($ngRedux.getState(), "account")
          ),
          showPersonas: scope.showPersonas ? true : false,
          label: scope.label ? scope.label : "Submit",
        });
      });

      scope.$watch("showPersonas", newValue => {
        elmApp.ports.publishIn.send({
          isValid: scope.isValid ? true : false,
          loggedIn: accountUtils.isLoggedIn(
            get($ngRedux.getState(), "account")
          ),
          showPersonas: newValue ? true : false,
          label: scope.label ? scope.label : "Submit",
        });
      });

      elmApp.ports.publishIn.send({
        isValid: scope.isValid ? true : false,
        loggedIn: accountUtils.isLoggedIn(get($ngRedux.getState(), "account")),
        showPersonas: scope.showPersonas ? true : false,
        label: scope.label ? scope.label : "Submit",
      });

      const personas = getOwnedPersonas($ngRedux.getState());
      if (personas) {
        elmApp.ports.personaIn.send(personas.toJS());
      }

      const disconnectOptions = $ngRedux.connect(state => {
        return {
          personas: getOwnedPersonas(state),
          loggedIn: accountUtils.isLoggedIn(get(state, "account")),
        };
      })(state => {
        elmApp.ports.publishIn.send({
          isValid: scope.isValid ? true : false,
          loggedIn: state.loggedIn,
          showPersonas: scope.showPersonas ? true : false,
          label: scope.label ? scope.label : "Submit",
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
        scope.onSubmit({ persona: url });
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
  .module("won.owner.components.submitButton", [])
  .directive("wonSubmitButton", genComponentConf).name;
