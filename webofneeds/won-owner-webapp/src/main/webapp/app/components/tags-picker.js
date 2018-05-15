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
        <div class="cis__addDetail__header tags" ng-click="self.resetTags() && self.updateDraft()">
            <svg class="cis__circleicon">
                <use xlink:href="#ico36_tags_circle" href="#ico36_tags_circle"></use>
            </svg>
        </div>
        <div class="cis__taglist">
            <span class="cis__taglist__tag" ng-repeat="tag in self.tags">#{{tag}}</span>
        </div>
        <input class="tp__input"
            placeholder="e.g. #couch #free" type="text"
            ng-keyup="::self.updateTags()"
        />



        <input type="text" class="tp__textbox" placeholder="e.g. #couch #free"/>
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

        
        textfieldNg() { return this.domCache.ng('.tp__input'); }

        textfield() { return this.domCache.dom('.tp__input'); }
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