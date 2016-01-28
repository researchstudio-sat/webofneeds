;

import angular from 'angular';
import 'ng-redux';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
            <div class="feedback" ng-click="self.rateMatch(0)">
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_good"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_good_white"/>
                <span class="feedback__text">Good - request conversation</span>
            </div>
            <div class="feedback" ng-click="self.rateMatch(1)">
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_ok"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_ok_white"/>
                <span class="feedback__text">OK - request conversation</span>
            </div>
            <div class="feedback" ng-click="self.rateMatch(2)">
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_notatall_hi"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_notatall_white"/>
                <span class="feedback__text">Not at all - remove!</span>
            </div>
        `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const disconnect = this.$ngRedux.connect(null, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        rateMatch(rating) {
            switch(rating) {
                case 0:
                    console.log("RATE GOOD");
                    //TODO: ADD GOOD RATING
                    this.connections__rate(this.item, 0);
                    this.openRequestDialog();
                    break;
                case 1:
                    console.log("RATE OK");
                    //TODO: ADD OK RATING
                    this.connections__rate(this.item, 1);
                    this.openRequestDialog();
                    break;
                case 2:
                    console.log("RATE BAD");
                    this.connections__close(this.item);
                    this.connections__rate(this.item, 2);
                    //TODO: ADD A BAD RATING, CLOSE MATCH
                    break;
            }
        }

        openRequestDialog(){
            this.requestItem = this.item;
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "=",
                requestItem: "="},
        template: template
    }
}

export default angular.module('won.owner.components.feedbackGrid', [])
    .directive('wonFeedbackGrid', genComponentConf)
    .name;

