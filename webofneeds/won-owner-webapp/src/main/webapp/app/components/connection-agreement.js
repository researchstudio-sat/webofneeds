
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
    dispatchEvent,
} from '../utils.js'
import {
	buildProposalMessage,
} from '../won-message-utils.js';
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
            	{{ self.agreementNumber+1  }}: {{self.checkDeclaration(self.declarations.proposeToCancel)? "Propose to cancel: " : "" }}{{ self.message.get('text') }}<br />
            	EventUri: {{ self.eventUri }}<br />
            	RealUri: {{ self.isOwn? self.message.get("uri") : self.message.get("remoteUri") }}
            </div>
            <div class="won-ca__content__button" ng-show="!self.clicked">
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
            		ng-click="self.proposeToCancel()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.agreement)">
            		 Cancel
            	</button>
            	<button class="won-button--filled thin red"
            		ng-click="self.acceptProposal()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposal) && !self.isOwn"">
            		 Accept
            	</button>
            	<button class="won-button--filled thin red"
            		ng-click="self.acceptProposeToCancel()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposeToCancel) && !self.isOwn">
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
        	const trimmedMsg = buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);

        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	//TODO: isAccepted = true;
        	/*	
        	this.message = this.message.set("isAccepted", true);
        	this.connections__sendChatMessage(this.message, this.connectionUri);
        	*/
        	this.onUpdate({draft: this.eventUri});
        	dispatchEvent(this.$element[0], 'update', {draft: this.eventUri});
        }
      
        proposeToCancel() {
        	this.clicked = true;
        	const uri = this.isOwn? this.message.get("uri") : this.message.get("remoteUri");
        	const msg = ("Propose to cancel agreement : " + uri);
        	const trimmedMsg = buildProposalMessage(uri, "proposesToCancel", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.onUpdate({draft: this.eventUri});
        	dispatchEvent(this.$element[0], 'update', {draft: this.eventUri});
        }
        
        acceptProposeToCancel() {
        	//TODO: send accept msg
        	this.clicked = true;
        	const msg = ("Accepted propose to cancel : " + this.message.get("remoteUri"));
        	const trimmedMsg = buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);

        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	this.onUpdate({draft: this.eventUri});
        	dispatchEvent(this.$element[0], 'update', {draft: this.eventUri});
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
        	 /*
             * Usage:
             *  on-update="::myCallback(draft)"
             */
            onUpdate: '&',
        },
        template: template,
    }
}

export default angular.module('won.owner.components.connectionAgreement', [
])
    .directive('wonConnectionAgreement', genComponentConf)
    .name;
