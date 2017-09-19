/**
 * Created by ksinger on 24.08.2017.
 */


import angular from 'angular';
import Immutable from 'immutable';
import won from '../won-es6.js';
import { actionCreators }  from '../actions/actions.js';
import { attach } from '../utils.js';
import {
    connect2Redux,
} from '../won-utils.js';

const serviceDependencies = ['$scope', '$ngRedux'];
function genComponentConf() {
    let template = `
      <div
        ng-transclude="header"
        class="dd__open-button clickable"
        ng-class="{ 'dd--closed' : !self.ddOpen }"
        ng-click="self.ddOpen = true;"
      >
      </div>
      <div class="dd__dropdown" ng-show="self.ddOpen">
        <div
          ng-transclude="header"
          class="dd__close-button clickable"
          ng-class="{ 'dd--open' : self.ddOpen }"
          ng-click="self.ddOpen = false;"
        >
        </div>
        <div
          class="dd__menu"
          ng-transclude="menu"
         >
         </div>
      </div>
    `

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            //Object.assign(this, srefUtils); // bind srefUtils to scope
            //this.labels = labels;
            window.covdd4dbg = this;

            const self = this;

            //const selectFromState = (state) => { }
            //connect2Redux(selectFromState, actionCreators, ['self.needUri'], this);
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        transclude: {
            header: 'wonDdHeader',
            menu: 'wonDdMenu',
        },

        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        // //scope: { }, // not isolated on purpose to allow using parent's scope
        scope: { },
        template: template
    }
}

export default angular.module('won.owner.components.coveringDropdown', [
])
    .directive('wonDropdown', genComponentConf)
    .name;
