import angular from 'angular';
import {
    attach,
    delay,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    doneTypingBufferNg,
    DomCache,
} from '../cstm-ng-utils.js'

const serviceDependencies = ['$scope', '$element', '$sce'];
function genComponentConf() {
    let template = `
        <input type="text" id="tp__textbox" placeholder="#"/>
            `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.domCache = new DomCache(this.$element);
            
            window.tp4dbg = this;

            doneTypingBufferNg(
                e => this.doneTyping(e),
                this.textfieldNg(), 1000
            );
        }

        doneTyping() {
            const text = this.textfield().value;

            if(!text) {
                // do stuff
            } else {
                // do stuff
            }
        }

        
        textfieldNg() { return this.domCache.ng('#tp__textbox'); }

        textfield() { return this.domCache.dom('#tp__textbox'); }
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

export default angular.module('won.owner.components.tagsPicker', [
    ])
    .directive('wonTagsPicker', genComponentConf)
    .name;