import angular from "angular";
import { Elm } from "../../elm/PublishButton.elm";
import "./svg-icon.js";
import { currentSkin } from "../selectors";

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
        flags: currentSkin(),
      });

      scope.$watch("isValid", newValue => {
        elmApp.ports.publishIn.send({
          draftValid: newValue ? true : false,
          loggedIn: $ngRedux.getState().getIn(["user", "loggedIn"]),
        });
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

      elmApp.ports.publishIn.send({
        draftValid: scope.isValid ? true : false,
        loggedIn: $ngRedux.getState().getIn(["user", "loggedIn"]),
      });

      const personas = $ngRedux.getState().get("personas");
      if (personas) {
        elmApp.ports.personaIn.send(convertPersonas(personas));
      }

      const disconnectOptions = $ngRedux.connect(state => {
        return {
          personas: state.get("personas"),
          loggedIn: state.getIn(["user", "loggedIn"]),
        };
      })(state => {
        elmApp.ports.publishIn.send({
          draftValid: scope.isValid ? true : false,
          loggedIn: state.loggedIn,
        });
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
