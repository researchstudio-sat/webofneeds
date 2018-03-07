
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
    selectNeedByConnectionUri,
} from '../selectors.js';


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
            	{{ self.agreementNumber+1  }}: {{ self.message.get('text') }}<br />
            	{{ self.eventUri }}
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
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposal) && !self.isOwn">
            		 Accept
            	</button>
            </div>
        	<div class="won-ca__content__text" ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposal) && self.isOwn">
        		You proposed this
        	</div>
            	<!--
            	<button class="won-button--filled thin red"
            		ng-click="self.show()"
            		ng-show="self.showDetail">
            		 Test
            	</button>
            	-->
            </div>
        </div>
`;



    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            //TODO debug; deleteme
            window.cis4dbg = this;
            
            this.declarations = clone(declarations);
            
            const self = this;
            this.clicked = false;
            this.showDetail = false;
            //this.stateLookUp();
            
            const selectFromState = state => {
            	
            	const ownNeed = this.connectionUri && selectNeedByConnectionUri(state, this.connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", this.connectionUri]);

                //const theirNeed = connection && state.getIn(["needs", connection.get('remoteNeedUri')]);
                const chatMessages = connection && connection.get("messages");
                const message = chatMessages && chatMessages.get(this.eventUri);
                const outgoingMessage = message && message.get("outgoingMessage");
                return {
                	message: message,
                	isOwn: outgoingMessage,
                }
            };

            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, ['self.connectionUri', 'self.eventUri'], this);
        }
        
        acceptProposal() {
        	this.clicked = true;
        	//const trimmedMsg = this.buildProposalMessage(this.message.get("remoteUri"), "accepts", this.message.get("text"));
        	const msg = ("Accepted proposal : " + this.message.get("remoteUri"));
        	const trimmedMsg = this.buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	//TODO: isAccepted = true;
        }
      
        buildProposalMessage(uri, type, text) {
        	const msgP = won.WONMSG.msguriPlaceholder;
        	const sc = "http://purl.org/webofneeds/agreement#"+type;
        	const whM = "\n won:hasTextMessage ";
        	return "<"+msgP+"> <"+sc+"> <"+uri+">;"+whM+" '''"+text.replace(/'/g, "///'")+"'''.";
        }
        
        checkDeclaration(declaration) {
        	return (this.agreementDeclaration === declaration)? true : false;
        }
        
        show() {
        	console.log("HERE we go: " + this.eventUri);
        	console.log("My Text: " + this.message.get("text"));
        	console.log("My Outgoing: " + this.isOwn);
        	
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
        	eventUri: "=",
        	agreementNumber: '=',
        	agreementDeclaration: '=',
        	connectionUri: '=',
        	//agreementObject: '=',
        },
        template: template,
    }
}

export default angular.module('won.owner.components.connectionAgreement', [
])
    .directive('wonConnectionAgreement', genComponentConf)
    .name;
