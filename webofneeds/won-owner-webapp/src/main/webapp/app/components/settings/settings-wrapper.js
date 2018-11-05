import angular from "angular";
import { Elm } from "../../../elm/Settings/Personas.elm";
import { actionCreators } from "../../actions/actions";
import "../identicon.js";
import { getPersonas, currentSkin } from "../../selectors/general-selectors.js";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    link: (scope, element) => {
      const elmApp = Elm.Settings.Personas.init({
        node: element[0],
        flags: {
          skin: currentSkin(),
          flags: null,
        },
      });

      elmApp.ports.personaOut.subscribe(persona => {
        $ngRedux.dispatch(actionCreators.personas__create(persona));
      });

      const personas = getPersonas($ngRedux.getState().get("needs"));
      if (personas) {
        elmApp.ports.personaIn.send(personas.toJS());
      }

      const disconnect = $ngRedux.connect(state => {
        return { personas: getPersonas(state.get("needs")) };
      })(state => {
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
        elmApp.ports.skin.send(currentSkin());
      });

      scope.$on("$destroy", () => {
        elmApp.ports.personaOut.unsubscribe();
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
