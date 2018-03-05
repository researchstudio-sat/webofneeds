
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
    clone,
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


const declarations = deepFreeze({
	proposal: "proposal",
	agreement: "agreement",
	proposeToCancel: "proposeToCancel",
	
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
            	{{ self.agreementNumber+1  }}: {{ self.agreementObject.text }}<br />
            	{{ self.agreementObject.id }}
            </div>
            <div class="won-ca__content__button">
            	<svg class="won-ca__content__carret clickable"
            	 		ng-click="self.showDetail = !self.showDetail"
            	 		ng-show="!self.showDetail">
                    <use href="#ico16_arrow_down"></use>
                </svg>
                <svg class="won-ca__content__carret clickable"
						ng-click="self.showDetail = !self.showDetail"
						ng-show="self.showDetail">
                    <use href="#ico16_arrow_up"></use>
                </svg>
            	<button class="won-button--filled thin black"
            		ng-click="self.show()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.agreement)">
            		 Cancel
            	</button>
            	<button class="won-button--filled thin red"
            		ng-click="self.show()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposal)">
            		 Accept
            	</button>
            	<button class="won-button--filled thin red"
            		ng-click="self.show()"
            		ng-show="self.showDetail">
            		 Test
            	</button>
            </div>
            <!--
                
            <div class="won-ca__content__addButton clickable" ng-click="self.self.showDetail = !self.showDetail">
            	<svg class="won-ca__content__carret" ng-show="!self.showDetail">
                    <use href="#ico16_arrow_down"></use>
                </svg>
                <svg class="won-ca__content__carret" ng-show="self.showDetail">
                    <use href="#ico16_arrow_up"></use>
                </svg>
		    <div>
		    -->
        </div>
`;



    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            //TODO debug; deleteme
            window.cis4dbg = this;
            
            this.declarations = clone(declarations);
            
            this.showDetail = false;
            
            const selectFromState = (state) => {
                return {   
                }
            };

            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, [], this);
        }
        
        checkDeclaration(declaration) {
        	return (this.agreementDeclaration === declaration)? true : false;
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
        	agreementNumber: '=',
        	agreementDeclaration: '=',
        },
        template: template,
    }
}

export default angular.module('won.owner.components.connectionAgreement', [
])
    .directive('wonConnectionAgreement', genComponentConf)
    .name;
