import angular from "angular";
import { Elm } from "../../../elm/Settings/Personas.elm";
import { actionCreators } from "../../actions/actions";
import "../identicon.js";
import { currentSkin } from "../../selectors";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    link: (scope, element) => {
      const elmApp = Elm.Settings.Personas.init({
        node: element[0],
        flags: currentSkin(),
      });

      elmApp.ports.personaOut.subscribe(persona => {
        $ngRedux.dispatch(actionCreators.personas__create(persona));
      });
      const convertPersonas = personas => {
        const conversion = personas
          .entrySeq()
          .map(([url, persona]) => {
            return {
              url: url,
              ...persona,
            };
          })
          .toJS();
        return conversion;
      };
      const personas = $ngRedux.getState().get("personas");
      if (personas) {
        elmApp.ports.personaIn.send(convertPersonas(personas));
      }

      const disconnect = $ngRedux.connect(state => {
        return { personas: state.get("personas") };
      })(state => {
        if (!state.personas) {
          return;
        }
        elmApp.ports.personaIn.send(convertPersonas(state.personas));
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
