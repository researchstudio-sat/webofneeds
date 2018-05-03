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

const serviceDependencies = ['$scope', '$ngRedux', '$element'];
function genComponentConf() {
    let template = `
      <div
        ng-transclude="header"
        class="dd__open-button clickable"
        ng-class="{ 'dd--closed' : !self.showMainMenu }"
        ng-click="self.showMainMenuDisplay()"
      >
      </div>
      <div class="dd__dropdown" ng-show="self.showMainMenu">
        <div
          ng-transclude="header"
          class="dd__close-button clickable"
          ng-class="{ 'dd--open' : self.showMainMenu }"
          ng-click="self.hideMainMenuDisplay()"
        >
        </div>
        <div
          class="dd__menu"
          ng-transclude="menu"
         >
         </div>
      </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            const self = this;

            const selectFromState = (state) => ({
                showMainMenu: state.get('showMainMenu'),
            });

            connect2Redux(selectFromState, actionCreators, [], this);


            this.$scope.$on('$destroy', () => {
                angular.element(window.document).unbind('click');
            });

            angular.element(window.document).bind('click',
                    event => {
                        var clickedElement = event.target;
                        //hide MainMenu if click was outside of the component and menu was open
                        if(this.showMainMenu && !this.$element[0].contains(clickedElement)){
                            this.hideMainMenuDisplay();
                        }
                    }
            );
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
