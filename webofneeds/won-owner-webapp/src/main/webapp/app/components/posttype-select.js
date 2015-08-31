/**
 * Created by ksinger on 31.08.2015.
 */

;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <dl ng-class="self.expandedFun() ? 'typeselect--expanded' : 'typeselect--collapsed'">
            <dt ng-repeat="o in self.options"
                ng-class="$index === self.selectedIdx? 'ts__option--selected' : 'ts__option'"
                ng-click="self.clickedItem($index)">
                    <span>{{o.text}}</span>
                    <img src="generated/icon-sprite.svg#ico36_help" class="ts__option__help-btn">
            </dt>
        </dl>
    `

    let options = [
        {
            text: 'I want to have something',
            helpText: 'Use this type in case (want) case sam quam aspic temod et que in prendiae perovidel.'
        },
        {
            text: 'I offer something',
            helpText: 'Use this type in case (offer) case sam quam aspic temod et que in prendiae perovidel.'
        },
        {
            text: 'I want to do something together',
            helpText: 'Use this type in case case (together) sam quam aspic temod et que in prendiae perovidel.'
        },
        {
            text: 'I want to change something',
            helpText: 'Use this type in case (change) sam quam aspic temod et que in prendiae perovidel.'
        }
    ]

    class Controller {
        constructor() {
            this.options = options;

            this.selectedIdx = undefined;
            //this.selectedIdx = 3;


            console.log('posttype-select.js : in ctrl')
        }
        clickedItem(idx) {
            if(this.selectedIdx !== idx) {
                this.selectedIdx = idx;
            } else {
                this.selectedIdx = undefined;
            }
            //TODO publish an event
            //TODO initialise from draft
        }
        expandedFun() {
            return isNaN(this.selectedIdx) || this.selectedIdx < 0 || this.selectedIdx >= this.options.length;
        }
        doSomething (arg) {
            console.log(this, arg);
        }
    }
    Controller.$inject = [/*injections as strings here*/];

    return {
        restrict: 'E',
        template: template,
        controller: Controller,
        controllerAs: 'self'
    }
}

export default angular.module('won.owner.components.posttypeSelect', [])
    .directive('wonPosttypeSelect', genComponentConf)
    .name;
