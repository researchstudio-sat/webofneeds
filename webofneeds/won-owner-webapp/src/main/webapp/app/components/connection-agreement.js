
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
            	{{ self.text }}<br />
            	<span class="subtitle" ng-show="self.checkDeclaration(self.declarations.proposeToCancel)">Proposed to cancel</span>
            	<!-- TODO: hide 
            	<div class="won-ca__content__text__subtext">
	            	<code>StateUri: {{ self.stateUri }}</code><br />
	            	<code>HeadUri:   {{ self.isOwn? self.message.get("uri") : self.message.get("remoteUri") }}</code>
	            	<code ng-show="self.cancelUri">CancelUri: {{ self.cancelUri }} </code>
            	</div>-->
            	
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
            	<button class="won-button--filled thin red"
            		ng-click="self.acceptProposeToCancel()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposeToCancel) && !self.ownCancel">
            		 Accept
            	</button>
            	<button class="won-button--filled thin black"
            		ng-click="self.rejectMessage()"
            		ng-show="self.showDetail && self.checkDeclaration(self.declarations.proposeToCancel) && !self.ownCancel">
            		 Reject
            	</button>
            	<span ng-show="self.showDetail && ((self.checkDeclaration(self.declarations.proposal) && self.isOwn) || (self.checkDeclaration(self.declarations.proposeToCancel) && self.ownCancel))">
        			You proposed this - 
        			<button class="won-button--filled thin black"
            			ng-click="self.retractMessage()">
    					 Retract
            		</button>
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
                const message = chatMessages && chatMessages.get(this.stateUri);
                const remoteUri = message && !!message.get("remoteUri");
                let text = undefined;
                
                if(chatMessages && message && (message.get("isProposeMessage") || message.get("isProposeToCancel"))) {
                	const clauses = message.get("clauses");
                	//TODO: delete me
                	//console.log("clauses: " + clauses);
                	
                	//TODO: Array from clauses
                	//now just one message proposed at a time
                	text = this.getClausesText(chatMessages, message, clauses);
                }
                
                return {
                	message: message,
                	text: text? text : message? message.get("text") : undefined,
                	isOwn: !remoteUri,
                	ownNeed: ownNeed,
                }
            };

            // Using actionCreators like this means that every action defined there is available in the template.
            connect2Redux(selectFromState, actionCreators, ['self.connectionUri', 'self.stateUri'], this);
        }
        
        getClausesText(chatMessages, message, clausesUri) {
        	for(msg of Array.from(chatMessages)) {
    			if(msg[1].get("uri") === clausesUri || msg[1].get("remoteUri") === clausesUri) {
    				//Get through the caluses "chain" and add the original text
    				if(!msg[1].get("clauses")) {
    					return msg[1].get("text");
    				} else {
    					//TODO: Mutliple clauses
    					return this.getClausesText(chatMessages, msg, msg[1].get("clauses"));
    				}
    				
    			}
        	}
        }

        markAsRelevant(relevant, uri) {
        	
        	if(!uri) {
        		uri = this.message.get("uri");
        	}
        	const payload = {
                messageUri: uri,
                connectionUri: this.connectionUri,
                needUri: this.ownNeed.get("uri"),
                relevant: relevant,
            };
            	
        	this.messages__markAsRelevant(payload);
        }
        
        acceptProposal() {
        	this.clicked = true;
        	const msg = ("Accepted proposal : " + this.message.get("remoteUri"));
        	const trimmedMsg = buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);

        	this.markAsRelevant(false);
        	this.onRemoveData({proposalUri: this.stateUri});
        }
      
        proposeToCancel() {
        	this.clicked = true;
        	const uri = this.isOwn? this.message.get("uri") : this.message.get("remoteUri");
        	const msg = ("Propose to cancel agreement : " + uri);
        	const trimmedMsg = buildProposalMessage(uri, "proposesToCancel", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.onUpdate({draft: this.stateUri});
        }
        
        acceptProposeToCancel() {
        	this.clicked = true;
        	var uri = this.cancelUri;
        	if(!uri) {
        		uri = this.message.get("remoteUri");
        	}
        	
        	const msg = ("Accepted propose to cancel agreement: " + uri);
        	const trimmedMsg = buildProposalMessage(uri, "accepts", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.markAsRelevant(false, uri);
        	this.onRemoveData({proposalUri: uri});
        }
        
        retractMessage() {
        	this.clicked = true;
        	var uri = this.cancelUri;
        	if(!uri) {
        		uri = this.isOwn? this.message.get("uri") : this.message.get("remoteUri");
        	}
        	const trimmedMsg = buildModificationMessage(uri, "retracts", ("Retract: " + this.text));
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.markAsRelevant(false, uri);
        	this.onUpdate();
        }
        
        rejectMessage() {
        	this.clicked = true;
        	var uri = this.cancelUri;
        	if(!uri) {
        		uri = this.message.get("remoteUri");
        	}
        	
        	const trimmedMsg = buildProposalMessage(uri, "rejects",  ("Reject: " + this.text));
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.markAsRelevant(false, uri);
        	this.onUpdate();
        }
        
        checkDeclaration(declaration) {
        	return (this.agreementDeclaration === declaration)? true : false;
        }
        
        //TODO: delete
        show() {
        	console.log("HERE we go: " + this.stateUri);
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
        	stateUri: "=",
        	headUri: "=",
        	cancelUri: "=",
        	ownCancel: "=",
        	agreementNumber: '=',
        	agreementDeclaration: '=',
        	connectionUri: '=',
        	//agreementObject: '=',
        	 /*
             * Usage:
             *  on-update="::myCallback(draft)"
             */
            onUpdate: '&',
            onRemoveData: '&',
        },
        template: template,
    }
}

export default angular.module('won.owner.components.connectionAgreement', [
])
    .directive('wonConnectionAgreement', genComponentConf)
    .name;
