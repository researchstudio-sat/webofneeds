import angular from "angular";
import { Elm } from "../../../elm/ForgotPassword.elm";
import "../identicon.js";
import { currentSkin } from "../../selectors/general-selectors.js";

function genComponentConf() {
  return {
    restrict: "E",
    scope: {
      module: "<",
      props: "<",
      onAction: "&",
    },
    link: (scope, element) => {
      Elm.ForgotPassword.init({
        node: element[0],
        flags: {
          skin: currentSkin(),
          flags: {
            width: window.innerWidth,
            height: window.innerHeight,
          },
        },
      });
    },
  };
}

export default angular
  .module("won.owner.components.forgotPasswordWrapper", [])
  .directive("wonForgotPasswordWrapper", genComponentConf).name;
