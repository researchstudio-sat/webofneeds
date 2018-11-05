import angular from "angular";
import { Elm } from "../../elm/RatingView.elm";
import { currentSkin } from "../selectors/general-selectors.js";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      rating: "=",
    },
    link: (scope, element) => {
      const elmApp = Elm.RatingView.init({
        node: element[0],
        flags: {
          skin: currentSkin(),
          flags: scope.rating || 0,
        },
      });

      const disconnectSkin = $ngRedux.connect(state => {
        return {
          skin: state.getIn(["config", "theme"]),
        };
      })(() => {
        const skin = currentSkin();
        elmApp.ports.skin.send(skin);
      });

      scope.$on("$destroy", () => {
        disconnectSkin();
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];
export default angular
  .module("won.owner.components.ratingView", [])
  .directive("wonRatingView", genComponentConf).name;
