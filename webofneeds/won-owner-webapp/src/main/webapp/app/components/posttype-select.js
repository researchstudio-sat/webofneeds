/**
 * Created by ksinger on 31.08.2015.
 */

;

import angular from 'angular';
import {broadcastEvent} from '../cstm-ng-utils';

function genComponentConf() {
    /*
     * The template for the directive.
     *
     * The $event.stopPropagation() makes sure clicking help doesn't also select the item.
     */
    let template = `
        <ul ng-class="self.expanded() ? 'typeselect--expanded' : 'typeselect--collapsed'">
            <li ng-repeat="o in self.options"
                ng-class="$index === self.selectedIdx? 'ts__option--selected' : 'ts__option'"
                ng-click="self.unSelect($index)">
                    <span>{{o.text}}</span>
                    <img src="generated/icon-sprite.svg#ico36_help"
                         ng-click="self.unSelectHelpFor($index); $event.stopPropagation();"
                         class="ts__option__help-btn">
                    <img src="generated/icon-sprite.svg#ico16_arrow_down_hi" class="ts__option__carret">
                    <div class="ts__option__help"
                         ng-show="self.isHelpVisible($index)"> {{o.helpText}} </div>
            </li>
        </dl>
    `

    class Controller {
        constructor($scope, $element) {
            this.selectedIdx = undefined;
            this.selectedHelp = undefined;
            this.$scope = $scope;
            this.$element = $element;

            //TODO debug; deleteme
            window.ptctrl = this;
            console.log('posttype-select.js : in ctrl', this, $element)
        }
        /*
         * sets selection to that item or entirely unsets it if type-select was already collapsed.
         */
        unSelect(idx) {
            if(this.selectedIdx !== idx) {
                this.selectedIdx = idx;
                this.selectedHelp = undefined;
                this.onSelect({idx: idx});
                broadcastEvent(this, 'selected-type', { idx });
            } else {
                this.selectedIdx = undefined;
                this.onUnselect();
                broadcastEvent(this, 'unselected-type');
            }
            //TODO initialise from draft
        }
        expanded() {
            return isNaN(this.selectedIdx) || this.selectedIdx < 0 || this.selectedIdx >= this.options.length;
        }
        unSelectHelpFor(idx) {
            //TODO also do this onHover

            if(this.selectedHelp !== idx) {
                this.selectedHelp = idx;
            } else {
                this.selectedHelp = undefined;
            }

            console.log('help: ', idx, this.options[idx].helpText);
        }
        isHelpVisible(idx) {
            return !isNaN(this.selectedHelp) && this.selectedHelp === idx;
        }
    }
    Controller.$inject = ['$scope', '$element'/*injections as strings here*/];

    return {
        restrict: 'E',
        template: template,
        controller: Controller,
        controllerAs: 'self',
        /*
         * make sure the isolated-scope/directive's properties below are
         * are bound to the controller instead
         */
        bindToController: true,
        scope: {
            /*
             * An array of objects in the form of:
             * [ { text: '...', helpText: '...' }, ..., { text: '...', helpText: '...' }]
             */
            options: '=',
            /*
             * Usage:
             *  on-select="myCallack(idx)"
             */
            onSelect: '&',
            onUnselect: '&'
        }
    }
}

export default angular.module('won.owner.components.posttypeSelect', [])
    .directive('wonPosttypeSelect', genComponentConf)
    .name;
