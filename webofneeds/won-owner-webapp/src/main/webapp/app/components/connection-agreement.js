
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
	buildModificationMessage,
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
        <div class="won-ca__content">
            <div class="won-ca__content__text">
            	{{ self.agreementNumber+1  }}: 
            	{{ self.message.get('text') }}<br />
            	<span class="subtitle" ng-show="self.checkDeclaration(self.declarations.proposeToCancel)">Proposed to cancel</span>
            	<div class="won-ca__content__text__subtext">
	            	<code>StateUri: {{ self.eventUri }}</code><br />
	            	<code>HeadUri:   {{ self.isOwn? self.message.get("uri") : self.message.get("remoteUri") }}</code>
            	</div>
            	
            </div>
            <div class="won-ca__content__carret">
    			<won-labelled-hr ng-click="self.showDetail = !self.showDetail" arrow="self.showDetail? 'up' : 'down'"></won-labelled-hr>
    		</div>
    		<div class="won-ca__content__footer" ng-show="!self.clicked">
            	<button class="won-button--filled thin black"
            		ng-click="self.proposeToCancel()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.agreement)">
            		 Cancel
            	</button>
            	<button class="won-button--filled thin red"
            		ng-click="self.acceptProposal()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposal) && !self.isOwn">
            		 Accept
            	</button>
            	<button class="won-button--filled thin black"
            		ng-click="self.rejectMessage()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposal) && !self.isOwn">
            		 Reject
            	</button>
            	<button class="won-button--filled thin black"
            		ng-click="self.retractMessage()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposal) && self.isOwn">
            		 Retract
            	</button>
            	<button class="won-button--filled thin red"
            		ng-click="self.acceptProposeToCancel()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposeToCancel) && !self.isOwn">
            		 Accept
            	</button>
            	<span ng-show="self.showDetail && !self.checkDeclaration(self.declarations.agreement)  && self.isOwn">
        			You proposed this
        		</span>
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
            this.showDetail = true;
            
            const selectFromState = state => {
            	
            	const ownNeed = this.connectionUri && selectNeedByConnectionUri(state, this.connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", this.connectionUri]);

                const chatMessages = connection && connection.get("messages");
                const message = chatMessages && chatMessages.get(this.eventUri);
                const remoteUri = message && !!message.get("remoteUri");
                //const remoteUri = message && message.get("remoteUri");
                
                return {
                	message: message,
                	isOwn: !remoteUri,
                	//isOwn: (this.eventUri == remoteUri)? true : false,
                }
            };

            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, ['self.connectionUri', 'self.eventUri'], this);
        }
        
        acceptProposal() {
        	this.clicked = true;
        	const msg = ("Accepted proposal : " + this.message.get("remoteUri"));
        	const trimmedMsg = buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);

        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
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
        	this.clicked = true;
        	//const trimmedMsg = this.buildProposalMessage(this.message.get("remoteUri"), "accepts", this.message.get("text"));
        	const msg = ("Accepted propose to cancel : " + this.message.get("remoteUri"));
        	const trimmedMsg = buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	this.onUpdate({draft: this.eventUri});
        	/*
        	this.clicked = true;
        	const msg = ("Accepted propose to cancel : " + this.message.get("remoteUri"));
        	const trimmedMsg = buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);

        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	this.onUpdate({draft: this.eventUri});
        	dispatchEvent(this.$element[0], 'update', {draft: this.eventUri});*/
        }
        
        retractMessage() {
        	this.clicked = true;
        	const uri = this.isOwn? this.message.get("uri") : this.message.get("remoteUri");
        	const msg = ("Retract message : " + uri);
        	const trimmedMsg = buildModificationMessage(uri, "retracts", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.onUpdate({draft: this.eventUri});
        	dispatchEvent(this.$element[0], 'update', {draft: this.eventUri});
        }
        
        rejectMessage() {
        	this.clicked = true;
        	const uri = this.isOwn? this.message.get("uri") : this.message.get("remoteUri");
        	const msg = ("Reject message : " + uri);
        	const trimmedMsg = buildProposalMessage(uri, "rejects", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.onUpdate({draft: this.eventUri});
        	dispatchEvent(this.$element[0], 'update', {draft: this.eventUri});
        }
        
        checkDeclaration(declaration) {
        	return (this.agreementDeclaration === declaration)? true : false;
        }
        
        //TODO: delete
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
