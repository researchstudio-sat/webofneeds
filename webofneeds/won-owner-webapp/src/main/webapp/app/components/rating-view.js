import angular from "angular";
import { Elm } from "../../elm/RatingView.elm";
import { currentSkin } from "../selectors/general-selectors.js";
import { actionCreators } from "../actions/actions";

function genComponentConf($ngRedux) {
  return {
    restrict: "E",
    scope: {
      rating: "=",
      ratingConnectionUri: "=",
    },
    link: (scope, element) => {
      const elmApp = Elm.RatingView.init({
        node: element[0],
        flags: {
          skin: currentSkin(),
          flags: {
            rating: scope.rating || 0,
            canRate: scope.ratingConnectionUri ? true : false,
          },
        },
      });

      elmApp.ports.reviewSubmitted.subscribe(rating => {
        if (scope.ratingConnectionUri) {
          $ngRedux.dispatch(
            actionCreators.personas__review(scope.ratingConnectionUri, rating)
          );
        } else {
          console.warn(
            "Could not rate, because no ratable connection is open!"
          );
        }
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
        elmApp.ports.reviewSubmitted.unsubscribe();
      });
    },
  };
}

genComponentConf.$inject = ["$ngRedux"];
export default angular
  .module("won.owner.components.ratingView", [])
  .directive("wonRatingView", genComponentConf).name;
