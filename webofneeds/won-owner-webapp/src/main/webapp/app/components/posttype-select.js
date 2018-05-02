/**
 * Created by ksinger on 31.08.2015.
 */

;

import angular from 'angular';
import { dispatchEvent, attach } from '../utils.js';
import enterModule from '../directives/enter.js';

function genComponentConf() {
    /*
     * The template for the directive.
     *
     * The $event.stopPropagation() makes sure clicking help doesn't also select the item.
     */
    let template = `
        <ul ng-class="self.expanded() ? 'typeselect--expanded' : 'typeselect--collapsed'">
            <li ng-repeat="o in self.options"
                class="clickable"
                ng-class="$index === self.selectedIdx? 'ts__option--selected' : 'ts__option'"
                ng-click="self.unSelect($index)"
                won-enter="self.unSelect($index)"
                tabindex="0">
                    <span>{{o.text}}&nbsp;&hellip;</span>
                    <svg style="--local-primary:var(--won-primary-color);"
                        ng-click="self.unSelectHelpFor($index); $event.stopPropagation();"
                        class="ts__option__help-btn clickable">
                            <use xlink:href="#ico36_help" href="#ico36_help"></use>
                    </svg>
                    <svg class="ts__option__carret" style="--local-primary:black;">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <div class="ts__option__help"
                         ng-show="self.isHelpVisible($index)"> {{o.helpText}} </div>
            </li>
        </dl>
    `

    const serviceDependencies = ['$scope', '$element'/*injections as strings here*/];
    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);
            this.selectedIdx = undefined;
            this.selectedHelp = undefined;
        }
        /*
         * sets selection to that item or entirely unsets it if type-select was already collapsed.
         */
        unSelect(idx) {
            if(this.selectedIdx !== idx) {
                this.selectedIdx = idx;
                this.selectedHelp = undefined;
                this.onSelect({idx: idx});

                //this.$scope.$emit('select-type', { idx });
                dispatchEvent(this.$element[0], 'select-type', { idx });
            } else {

                this.selectedIdx = undefined;
                this.onUnselect();

                //this.$scope.$emit('unselect-type');
                dispatchEvent(this.$element[0], 'unselect-type');

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
    Controller.$inject = serviceDependencies;

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

export default angular.module('won.owner.components.posttypeSelect', [
        enterModule
    ])
    .directive('wonPosttypeSelect', genComponentConf)
    .name;
