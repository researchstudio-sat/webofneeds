import angular from "angular";
import ngAnimate from "angular-animate";
import { Elm } from "../../../elm/ForgotPassword.elm";
import { currentSkin } from "../../selectors/general-selectors.js";

import "style/_signup.scss";

const serviceDependencies = ["$element"];

class ForgotPasswordController {
  constructor(element) {
    Elm.ForgotPassword.init({
      node: element[0].querySelector(".elmHost"),
      flags: {
        skin: currentSkin(),
        flags: null,
      },
    });
  }
}

export default angular
  .module("won.owner.components.forgotPassword", [ngAnimate])
  .controller("ForgotPasswordController", [
    ...serviceDependencies,
    ForgotPasswordController,
  ]).name;
