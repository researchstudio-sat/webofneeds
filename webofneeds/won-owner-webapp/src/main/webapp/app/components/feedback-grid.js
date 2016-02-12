;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <div class="feedback" ng-click="self.openRequestDialog(self.item)">
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_good"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_good_white"/>
                <span class="feedback__text">Good - request conversation</span>
            </div>
            <div class="feedback" ng-click="self.openRequestDialog(self.item)">
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_ok"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_ok_white"/>
                <span class="feedback__text">OK - request conversation</span>
            </div>
            <div class="feedback" ng-click="self.openRequestDialog(self.item)">
                <img class="feedback__icon unselected" src="generated/icon-sprite.svg#ico36_feedback_notatall_hi"/>
                <img class="feedback__icon selected" src="generated/icon-sprite.svg#ico36_feedback_notatall_white"/>
                <span class="feedback__text">Not at all - remove!</span>
            </div>
        `;

    class Controller {
        constructor() {}

        openRequestDialog(item){
            //TODO: SEND FEEDBACK MESSAGE OVER WS
            console.log("SELECTING ITEM: "+item);
            this.requestItem = item;
        }
    }

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

