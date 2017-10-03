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
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_good"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_good_white"/>
                <span class="feedback__text">Good - request conversation</span>
            </a>
            <!--div class="feedback" ng-click="self.rateMatch(1)">
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_ok"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_ok_white"/>
                <span class="feedback__text">OK - request conversation</span>
            </div-->
            <div class="feedback clickable" ng-click="self.rateMatch(2)">
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_notatall_hi"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_notatall_white"/>
                <span class="feedback__text">Not at all - remove!</span>
            </div>
        `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            connect2Redux(selectFromState, actionCreators, [], this);
        }

        rateMatch(rating) {
            switch(rating) {
                case 0:
                    console.log("RATE GOOD");
                    this.connections__rate(this.connectionUri, won.WON.binaryRatingGood);
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

