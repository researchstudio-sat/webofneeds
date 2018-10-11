import angular from "angular";
import { Elm } from "../../elm/PublishButton.elm";
import "./svg-icon.js";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    link: (scope, element) => {
      const elmApp = Elm.PublishButton.init({ node: element[0] });

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

      scope.$on("$destroy", () => {
        disconnect();
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];
export default angular
  .module("won.owner.components.publishButton", [])
  .directive("wonPublishButton", genComponentConf).name;
