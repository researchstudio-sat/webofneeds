;

import angular from 'angular';
import 'ng-redux';
import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    connect2Redux,
} from '../won-utils.js';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
            <a class="feedback clickable" ng-click="self.rateMatch(0); self.router__stateGoCurrent({connectionUri : self.connectionUri})">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="feedback__icon unselected">
                        <use xlink:href="#ico36_feedback_good" href="#ico36_feedback_good"></use>
                </svg>
                <svg style="--local-primary:white;"
                    class="feedback__icon selected">
                        <use xlink:href="#ico36_feedback_good" href="#ico36_feedback_good"></use>
                </svg>
                <span class="feedback__text">Good match - connect!</span>
            </a>
            <!--div class="feedback" ng-click="self.rateMatch(1)">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="feedback__icon unselected">
                        <use xlink:href="#ico36_feedback_ok" href="#ico36_feedback_ok"></use>
                </svg>
                <svg style="--local-primary:white;"
                     class="feedback__icon selected">
                        <use xlink:href="#ico36_feedback_ok" href="#ico36_feedback_ok"></use>
                </svg>
                <span class="feedback__text">OK - request conversation</span>
            </div-->
            <div class="feedback clickable" ng-click="self.rateMatch(2)">
                <svg class="feedback__icon unselected" style="--local-primary:black;">
                    <use xlink:href="#ico36_feedback_notatall" href="#ico36_feedback_notatall"></use>
                </svg>
                <svg style="--local-primary:white;"
                    class="feedback__icon selected">
                        <use xlink:href="#ico36_feedback_notatall" href="#ico36_feedback_notatall"></use>
                </svg>
                <span class="feedback__text">Bad match - remove!</span>
            </div>
        `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            const selectFromState = (state) => ({});
            connect2Redux(selectFromState, actionCreators, [], this);
        }

        rateMatch(rating) {


            switch(rating) {
                case 0:
                    console.log("RATE GOOD");
                    this.connections__rate(this.connectionUri, won.WON.binaryRatingGood);
                    this.connections__markAsRated({connectionUri: this.connectionUri});
                    break;
                /*case 1:
                    //OPTION OK WILL NOT BE IMPLEMENTED ANYMORE
                    console.log("RATE OK");
                    this.connections__rate(this.item, 1);
                    break;*/
                case 2:
                    console.log("RATE BAD");
                    this.connections__close(this.connectionUri);
                    this.connections__rate(this.connectionUri, won.WON.binaryRatingBad);
                    this.router__stateGoCurrent({connectionUri: null})
                    //TODO: ADD A BAD RATING, CLOSE MATCH
                    break;
            }
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { connectionUri: "=" },
        template: template
    }
}

export default angular.module('won.owner.components.feedbackGrid', [])
    .directive('wonFeedbackGrid', genComponentConf)
    .name;

