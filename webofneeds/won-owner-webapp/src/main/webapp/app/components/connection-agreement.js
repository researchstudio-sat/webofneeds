
;

import angular from 'angular';
import jld from 'jsonld';
import Immutable from 'immutable';
import {
    relativeTime,
} from '../won-label-utils.js'
import {
    connect2Redux,
} from '../won-utils.js';
import {
    attach,
    delay,
    getIn,
    deepFreeze,
} from '../utils.js'
import {
    actionCreators
}  from '../actions/actions.js';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
} from '../selectors.js';
import autoresizingTextareaModule from '../directives/textarea-autogrow.js';


const align = deepFreeze({
    left:  "won-ca--left",
    right: "won-ca--right",
});

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

function genComponentConf() {
    let template = `
       <!-- <won-square-image
            title="self.theirNeed.get('title')"
            src="self.theirNeed.get('TODOtitleImgSrc')"
            uri="self.theirNeed.get('uri')"
            ng-click="self.router__stateGoAbs('post', {postUri: self.theirNeed.get('uri')})"
            ng-show="!self.message.get('outgoingMessage')">
        </won-square-image>-->
        <div class="won-ca__content">
            <div class="won-ca__content__text">
            	{{ self.agreementObject.text }}\n
            	{{ self.agreementObject.id }}
            </div>
            <div class="won-ca__content__button" 
            	<button class="won-button--filled thin black" ng-click="self.show()">Test</button>
            </div>
        </div>
`;



    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            //TODO debug; deleteme
            window.cis4dbg = this;

            const selectFromState = (state) => {
                return {   
                }
            };

            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, [], this);
        }
        
        show() {
        	console.log("HERE we go: " + this.agreementObject.id);
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { 
        	agreementObject: '=',
        },
        template: template,
    }
}

export default angular.module('won.owner.components.connectionAgreement', [
])
    .directive('wonConnectionAgreement', genComponentConf)
    .name;
