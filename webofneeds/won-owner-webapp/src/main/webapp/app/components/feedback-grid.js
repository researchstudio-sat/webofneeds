import angular from "angular";
import won from "../won-es6.js";
import "ng-redux";
import { attach } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors/general-selectors.js";

import "style/_feedback-grid.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
            <div ng-class="{'feedback--good': self.isConnected, 'feedback--disabled': !self.isConnected}"
              class="clickable" 
              ng-click="self.rateMatch(self.WON.binaryRatingGood); self.router__stateGoCurrent({connectionUri : self.connectionUri})">
                <svg class="feedback__icon unselected">
                  <use xlink:href="#ico36_feedback_good" href="#ico36_feedback_good"></use>
                </svg>
                <span class="feedback__text">Good match - connect!</span>
            </div>
            <div
              ng-class="{'feedback--bad': self.isConnected, 'feedback--disabled': !self.isConnected}"
              class="clickable" 
              ng-click="self.rateMatch(self.WON.binaryRatingBad)">
                <svg class="feedback__icon unselected">
                    <use xlink:href="#ico36_feedback_notatall" href="#ico36_feedback_notatall"></use>
                </svg>
                <span class="feedback__text">Bad match - remove!</span>
            </div>
        `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.WON = won.WON;
      window.fgrd4dbg = this;

      const selectFromState = state => ({
        isConnected: selectIsConnected(state),
      });
      connect2Redux(selectFromState, actionCreators, [], this);
    }

    rateMatch(rating) {
      if (!this.isConnected) {
        return;
      }
      switch (rating) {
        case won.WON.binaryRatingGood:
          this.connections__rate(this.connectionUri, won.WON.binaryRatingGood);
          break;

        case won.WON.binaryRatingBad:
          this.connections__close(this.connectionUri);
          this.connections__rate(this.connectionUri, won.WON.binaryRatingBad);
          this.router__stateGoCurrent({ connectionUri: null });
          break;
      }
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: { connectionUri: "=" },
    template: template,
  };
}

export default angular
  .module("won.owner.components.feedbackGrid", [])
  .directive("wonFeedbackGrid", genComponentConf).name;
