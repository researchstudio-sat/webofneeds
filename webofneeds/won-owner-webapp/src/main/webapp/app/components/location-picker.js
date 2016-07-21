/**
 * Created by ksinger on 15.07.2016.
 */

import won from '../won-es6';
import angular from 'angular';
import 'ng-redux';
//import { labels } from '../won-label-utils';
import { attach, searchNominatim } from '../utils.js';
import { actionCreators }  from '../actions/actions';
import { } from '../selectors';
import { doneTypingBufferNg } from '../cstm-ng-utils'

const serviceDependencies = ['$scope', '$ngRedux', '$element'];
function genComponentConf() {
    //TODO input as text-input or contenteditable? need to overl
    let template = `


        <input type="text" class="lp__searchbox" placeholder="Search for location"/>
        <img class="lp__mapmount" src="images/some_map_screenshot.png"alt=""/>
            `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.lp4dbg = this;
            const selectFromState = (state)=>{
                return {
                };
            }

            doneTypingBufferNg(
                e => this.doneTyping(e),
                this.textfieldNg(), 1000
            )

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

        }
        doneTyping() {
            console.log('TODO starting type-ahead search for: ' + this.textfield().value);
            //buffer for 1s before starting the search

        }

        textfieldNg() {
            if(!this._textfield) {
                this._textfield = this.$element.find('.lp__searchbox');
            }
            return this._textfield;
        }

        textfield() {
            return this.textfieldNg()[0];
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


window.searchNominatim4dbg = searchNominatim;