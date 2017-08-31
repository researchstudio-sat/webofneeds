/**
 * Created by ksinger on 24.08.2017.
 */


import angular from 'angular';
import Immutable from 'immutable';
import won from '../won-es6';
import { actionCreators }  from '../actions/actions';
import { attach } from '../utils';
import {
    connect2Redux,
} from '../won-utils';

const serviceDependencies = ['$scope', '$ngRedux'];
function genComponentConf() {
    let template = `
      <div
        class="dd--closed__inner"
        ng-click="self.ddOpen = true;"
        style="border: 1px solid transparent;"
        class="clickable"
        ng-transclude="header" >
      </div>
      <div
        class="dd--big__inner"
        style="position: absolute; top:0; width: 400px; background-color: white; border: 1px solid #cbd2d1;"
        ng-show="self.ddOpen"
      >
        <div
          ng-transclude="header"
          class="clickable"
          ng-click="self.ddOpen = false;"
        >
        </div>
        <div ng-transclude="menu"></div>
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
