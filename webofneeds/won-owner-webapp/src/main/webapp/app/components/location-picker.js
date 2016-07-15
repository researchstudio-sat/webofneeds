/**
 * Created by ksinger on 15.07.2016.
 */
import won from '../won-es6';
import angular from 'angular';
import 'ng-redux';
//import { labels } from '../won-label-utils';
import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions';
import { } from '../selectors';

const serviceDependencies = ['$scope', '$ngRedux'];
function genComponentConf() {
    //TODO input as text-input or contenteditable? need to overl
    let template = `


            `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.lp4dbg = this;
            const selectFromState = (state)=>{
                return {
                };
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
        },
        template: template
    }
}

export default angular.module('won.owner.components.locationPicker', [
    ])
    .directive('wonLocationPicker', genComponentConf)
    .name;
