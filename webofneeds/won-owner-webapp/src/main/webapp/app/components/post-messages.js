;

import won from '../won-es6.js';
import angular from 'angular';
import jld from 'jsonld';
import Immutable from 'immutable';
import chatTextFieldModule from './chat-textfield.js';
import chatTextFieldSimpleModule from './chat-textfield-simple.js';
import connectionMessageModule from './connection-message.js';
import connectionAgreementModule from './connection-agreement.js';
import {
} from '../won-label-utils.js'
import {
    connect2Redux,
} from '../won-utils.js';
import {
    attach,
    delay,
    checkHttpStatus,
} from '../utils.js'
import {
	callAgreementsFetch,
} from '../won-message-utils.js';
import {
    actionCreators
}  from '../actions/actions.js';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
} from '../selectors.js';
import autoresizingTextareaModule from '../directives/textarea-autogrow.js';
//import won.owner.web.rest.highlevel.HighlevelProtocolsController;

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

function genComponentConf() {
    let template = `
        <div class="pm__header">
            <a class="clickable"
                ng-click="self.router__stateGoCurrent({connectionUri : null})">
                <img class="pm__header__icon clickable"
                     src="generated/icon-sprite.svg#ico36_close"/>
            </a>
            <div class="pm__header__title clickable"
                ng-click="self.router__stateGoAbs('post', { postUri: self.theirNeed.get('uri')})">
                {{ self.theirNeed.get('title') }}
            </div>
        </div>
        <div class="pm__content">
            <img src="images/spinner/on_white.gif"
                alt="Loading&hellip;"
                ng-show="self.connection.get('loadingEvents')"
                class="hspinner"/>
            <a ng-show="self.eventsLoaded && !self.connection.get('loadingEvents') && !self.allLoaded"
                ng-click="self.connections__showMoreMessages(self.connection.get('uri'), 5)"
                href="">
                    show more
            </a>
            <won-connection-message
                ng-repeat="msg in self.chatMessages"
                connection-uri="self.connectionUri"
                message-uri="msg.get('uri')"
                message="msg">
            </won-connection-message>
            <won-connection-agreement
            	ng-repeat="prop in self.proposalList"
                agreement-object="prop">
            </won-connection-agreement>
        </div>
        <chat-textfield
            class="pm__footer"
            placeholder="::'Your Message'"
            on-input="::self.input(value)"
            on-paste="::self.input(value)"
            on-submit="::self.send()"
            submit-button-label="::'Send'"
            >
        </chat-textfield>
        <!-- quick and dirty button to get agreements -->
        <div  ng-show="self.shouldShowRdf">
	        	<button 
	                class="rdfMsgBtnTmpDeletme" 
	                ng-click="self.getAgreementsData()">
	                    Load Agreements Data
	            </button>
         </div>
         <br>
        <!--
        <chat-textfield-simple
            class="pm__footer"
            placeholder="::'Your Message'"
            on-input="::self.input(value)"
            on-paste="::self.input(value)"
            on-submit="::self.send()"
            submit-button-label="::'Send'"
            >
        </chat-textfield-simple>
        -->

        <!-- 
        quick'n'dirty textfield and button so flo can use it for his branch. 
        TODO implement and style chat-textfield-simple and use that instead.
        -->
        <div style="display: flex;">
            <textarea 
                class="rdfTxtTmpDeletme" 
                ng-show="self.shouldShowRdf" 
                won-textarea-autogrow 
                style="resize: none; height: auto;   flex-grow: 1;   font-family: monospace;"
                placeholder="Expects valid turtle. <{{self.msguriPlaceholder}}> will be the uri generated for this message. See \`won.minimalTurtlePrefixes \` for prefixes that will be added automatically."
            ></textarea>
            <button 
                class="rdfMsgBtnTmpDeletme" 
                ng-show="self.shouldShowRdf" 
                ng-click="self.sendRdfTmpDeletme()">
                    Send RDF
            </button>
        </div>
        <div>
            <a class="rdflink withlabel clickable"
               ng-click="self.toggleRdfDisplay()">
                   <svg class="rdflink__small">
                       <use href="#rdf_logo_1"></use>
                   </svg>
                  <span class="rdflink__text">[{{self.shouldShowRdf? "HIDE" : "SHOW"}}]</span> 
            </a>
        </div>
    `;



    class Controller {
        constructor(/* arguments = dependency injections */) {
            attach(this, serviceDependencies, arguments);
            window.pm4dbg = this;
            
            const self = this;
            
            this.proposalList = [];
            
            this.agreementData = {
            		proposals: [], 
            		agreements: [], 
            		proposeToCancel: [],
            		acceptedProposalsToCancel: [],
            };
            
            this.scrollContainer().addEventListener('scroll', e => this.onScroll(e));
            this.msguriPlaceholder = won.WONMSG.msguriPlaceholder;

            //this.postmsg = this;
            const selectFromState = state => {
                const connectionUri = selectOpenConnectionUri(state);
                const ownNeed = selectNeedByConnectionUri(state, connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", connectionUri]);

                const theirNeed = connection && state.getIn(["needs", connection.get('remoteNeedUri')]);
                const chatMessages = connection && connection.get("messages");
                const allLoaded = chatMessages && chatMessages.filter(msg => msg.get("connectMessage")).size > 0;

                let sortedMessages = chatMessages && chatMessages.toArray();
                if(sortedMessages){
                    sortedMessages.sort(function(a,b) {
                        return a.get("date").getTime() - b.get("date").getTime();
                    });
                }

                return {
                    ownNeed,
                    theirNeed,
                    connectionUri,
                    connection,
                    eventsLoaded: true, //TODO: CHECK IF MESSAGES ARE CURRENTLY LOADED
                    chatMessages: sortedMessages,
                    debugmode: won.debugmode,
                    shouldShowRdf: state.get('showRdf'),
                    // if the connect-message is here, everything else should be as well
                    allLoaded,
                }
            };

            connect2Redux(selectFromState, actionCreators, [], this);

            this.snapToBottom();

            this.$scope.$watchGroup(
                ['self.connection'],
                () => this.ensureMessagesAreLoaded()
            );

            this.$scope.$watch(
                () => this.chatMessages && this.chatMessages.length && this.proposalList, // trigger if there's messages added (or removed)
                () => delay(0).then(() =>
                    // scroll to bottom directly after rendering, if snapped
                    this.updateScrollposition()
                )
            )
            
        }
        
        ensureMessagesAreLoaded() {
            delay(0).then(() => {
                // make sure latest messages are loaded
                if (
                    this.connection &&
                    !this.connection.get('loadingEvents')
                    //&& !this.eventsLoaded
                ) {
                    this.connections__showLatestMessages(this.connection.get('uri'), 4);
                }
            })
        }

        snapToBottom() {
            this._snapBottom = true;
            this.scrollToBottom();
        }
        unsnapFromBottom() {
            this._snapBottom = false;
        }
        updateScrollposition() {
            if(this._snapBottom) {
                this.scrollToBottom();
            }
        }
        scrollToBottom() {
            this._programmaticallyScrolling = true;

            this.scrollContainer().scrollTop = this.scrollContainer().scrollHeight;
        }
        onScroll(e) {
            if(!this._programmaticallyScrolling) {
                //only unsnap if the user scrolled themselves
                this.unsnapFromBottom();
            }

            const sc = this.scrollContainer();
            const isAtBottom = sc.scrollTop + sc.offsetHeight >= sc.scrollHeight;
            if(isAtBottom) {
                this.snapToBottom();
            }

            this._programmaticallyScrolling = false
        }
        scrollContainerNg() {
            return angular.element(this.scrollContainer());
        }
        scrollContainer() {
            if(!this._scrollContainer) {
                this._scrollContainer = this.$element[0].querySelector('.pm__content');
            }
            return this._scrollContainer;
        }

        input(userInput) {
            this.chatMessage = userInput;
        }

        send() {
            const trimmedMsg = this.chatMessage.trim();
            if(trimmedMsg) {
               this.connections__sendChatMessage(trimmedMsg, this.connection.get('uri'));
            }
        }
        
        getAgreementsData() {
        	//this.getAgreements();
        	this.getProposals();
        	//this.getAgreementsProposedToBeCancelled();
        	//this.getAcceptedPropsalsToCancel();
        	
        	//this.sendAgreementsOverviewMsg();
        }
        
        getAgreements() {
        	var url = '/owner/rest/highlevel/getAgreements/?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
        		.then(response => {
                	if(response["@graph"]) {this.agreementData.agreements = parseAgreementsData(Array.from(response['@graph']));}
                }).catch(error => console.error('Error:', error))
        }
        
        getProposals() {
        	var url = '/owner/rest/highlevel/getProposals/?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			//if(response["@graph"]) {this.agreementData.proposals = this.parseAgreementsData(Array.from(response['@graph']));}
    			if(response["@graph"]) {this.proposalList = this.parseAgreementsData(Array.from(response['@graph']));}
    		}).catch(error => console.error('Error:', error))
        }
        
        getAgreementsProposedToBeCancelled() {
        	var url = '/owner/rest/highlevel/getAgreementsProposedToBeCancelled/?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			if(response["@graph"]) {this.agreementData.proposeToCancel = Array.from(response['@graph']);}
    		}).catch(error => console.error('Error:', error))
        }
        
        getAcceptedPropsalsToCancel() {
        	var url = '/owner/rest/highlevel/getAcceptedPropsalsToCancel/?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			if(response["@graph"]) {this.agreementData.acceptedProposalsToCancel = Array.from(response['@graph']);}
    		}).catch(error => console.error('Error:', error))
        }
        
        parseAgreementsData(obj) {
        	var list = [];
        	const getText = "http://purl.org/webofneeds/model#hasTextMessage";
        	
        	if(obj.length < 2) {
        		list.push({id: obj[0]["@id"], text: obj[0][getText]});
        	}else {
	        	for(i = 0; i < obj.length; i++){
		        	list.push({id: obj[i]["@graph"][0]["@id"], text: obj[i]["@graph"][0][getText]});
		        }
        	}
        	return list;
        }
        
        sendAgreementsOverviewMsg(){
        	const obj = this.agreementData;
        	const getText = "http://purl.org/webofneeds/model#hasTextMessage";
        	
        	var msg = "Agreements: '";
        	if(obj.agreements){
	        	for(i = 0; i < obj.agreements.length; i++){
	        		msg += (i+1) + ": " + obj.agreements[i]["@graph"][0][getText] + " - ";
	        	}
        	}
        	msg += "  |  "
        	msg += "Proposals: ";
        	if(obj.proposals){
	        	for(i = 0; i < obj.proposals.length; i++){
	        		msg += (i+1) + ": " + obj.proposals[i]["@graph"][0][getText] + " - ";
	        	}
        	}
        	
        	//const message = {text: msg, outgoingMessage: false};
        	//this.chatMessages.push(message);
        	this.connections__sendChatMessage(msg, this.connection.get('uri'));
        }
        
        sendRdfTmpDeletme() { //TODO move to own component
            const rdftxtEl = this.$element[0].querySelector('.rdfTxtTmpDeletme');
            if(rdftxtEl) {
                console.log('found rdftxtel: ', rdftxtEl.value);
                const trimmedMsg = rdftxtEl.value.trim();
                if(trimmedMsg) {
                    this.connections__sendChatMessage(trimmedMsg, this.connection.get('uri'), isTTL=true);
                }
            }

        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { },
        template: template,
    }
}

export default angular.module('won.owner.components.postMessages', [
    chatTextFieldModule,
    autoresizingTextareaModule,
    chatTextFieldSimpleModule,
    connectionMessageModule,
    connectionAgreementModule,
])
    .directive('wonPostMessages', genComponentConf)
    .name;
