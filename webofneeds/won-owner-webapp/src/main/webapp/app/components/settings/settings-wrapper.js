import angular from "angular";
import { Elm } from "../../../elm/Settings.elm";
import { actionCreators } from "../../actions/actions";
import "../identicon.js";
import {
  getOwnedPersonas,
  currentSkin,
} from "../../selectors/general-selectors.js";
import { get } from "../../utils";
import * as accountUtils from "../../account-utils.js";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    link: (scope, element) => {
      const elmApp = Elm.Settings.init({
        node: element[0],
        flags: {
          skin: currentSkin(),
          flags: {
            width: window.innerWidth,
            height: window.innerHeight,
          },
        },
      });

      elmApp.ports.personaOut.subscribe(persona => {
        $ngRedux.dispatch(actionCreators.personas__create(persona));
      });

      elmApp.ports.updatePersonas.subscribe(() => {
        const personas = getOwnedPersonas($ngRedux.getState());
        if (personas) {
          elmApp.ports.personaIn.send(personas.toJS());
        }
      });

      elmApp.ports.getVerified.subscribe(() => {
        const isVerified = accountUtils.isEmailVerified(
          get($ngRedux.getState(), "account")
        );
        elmApp.ports.isVerified.send(isVerified);
      });

      const personas = getOwnedPersonas($ngRedux.getState());
      if (personas) {
        elmApp.ports.personaIn.send(personas.toJS());
      }

      const disconnect = $ngRedux.connect(state => {
        return {
          personas: getOwnedPersonas(state),
          isVerified: accountUtils.isEmailVerified(get(state, "account")),
        };
      })(state => {
        if (state.personas) {
          elmApp.ports.personaIn.send(state.personas.toJS());
        }
        elmApp.ports.isVerified.send(state.isVerified);
      });

      const disconnectSkin = $ngRedux.connect(state => {
        return {
          skin: state.getIn(["config", "theme"]),
        };
      })(() => {
        elmApp.ports.skin.send(currentSkin());
      });

      scope.$on("$destroy", () => {
        elmApp.ports.personaOut.unsubscribe();
        elmApp.ports.getVerified.unsubscribe();
        disconnectSkin();
        disconnect();
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];
export default angular
  .module("won.owner.components.settingsWrapper", [])
  .directive("wonSettingsWrapper", genComponentConf).name;
