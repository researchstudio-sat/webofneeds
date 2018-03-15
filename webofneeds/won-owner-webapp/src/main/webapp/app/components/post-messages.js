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
    deepFreeze,
    clone,
    checkHttpStatus,
} from '../utils.js'
import {
	callAgreementsFetch,
	callAgreementEventFetch,
} from '../won-message-utils.js';
import {
    actionCreators
}  from '../actions/actions.js';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
} from '../selectors.js';
import autoresizingTextareaModule from '../directives/textarea-autogrow.js';

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

const declarations = deepFreeze({
	proposal: "proposal",
	agreement: "agreement",
	proposeToCancel: "proposeToCancel",
	acceptedProposalToCancel: "acceptedProposalToCancel",
	
});
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
                message="msg"
                on-update="::self.showAgreementData = false">
            </won-connection-message>
            <div class="pm__content__agreement" ng-if="self.showAgreementData && self.agreementDataIsValid()">           	
	            <img class="pm__content__agreement__icon clickable"
            		src="generated/icon-sprite.svg#ico36_close"
            		ng-click="self.showAgreementData = !self.showAgreementData"/>
            	<!-- Agreements-->
            	<div class="pm__content__agreement__title" ng-show="self.agreementData.agreement.size"> 
            		Agreements
            		<span ng-show="self.loading.agreement"> (loading...)</span>
            		<span ng-if="!self.loading.agreement"> (up-to-date)</span>
            	</div>
	            <won-connection-agreement
	            	ng-repeat="agree in self.getArrayFromSet(self.agreementData.agreement) track by $index"
	                event-uri="agree"
	                agreement-number="$index"
	                agreement-declaration="self.declarations.agreement"
	                connection-uri="self.connectionUri"
	                on-update="::self.showAgreementData = false">
	            </won-connection-agreement>
	            <!-- /Agreements -->
            	<!-- PROPOSALS -->
            	<div class="pm__content__agreement__title" ng-show="self.agreementData.proposal.size">
            		<br ng-show="self.agreementData.agreement.size" />
            		<hr ng-show="self.agreementData.agreement.size" />
            		Proposals
    				<span ng-show="self.loading.proposal"> (loading...)</span>
            		<span ng-if="!self.loading.proposal"> (up-to-date)</span>
            	</div>
	            <won-connection-agreement
	            	ng-repeat="prop in self.getArrayFromSet(self.agreementData.proposal) track by $index"
	                event-uri="prop"
	                agreement-number="$index"
	                agreement-declaration="self.declarations.proposal"
	                connection-uri="self.connectionUri"
	                on-update="(self.showAgreementData = false) && (self.updateAgreementData(draft))">
	            </won-connection-agreement>
	            <!-- /PROPOSALS -->
            </div>
            <!-- Loading Text -->
            <div class="pm__content__agreement" ng-if="self.showAgreementData && self.isStillLoading() && self.showLoadingInfo && !self.agreementDataIsValid()">
	            <img class="pm__content__agreement__icon clickable"
	            		src="generated/icon-sprite.svg#ico36_close"
	            		ng-click="(self.showAgreementData = !self.showAgreementData) && (self.showLoadingInfo = !self.showLoadingInfo)"/>
	            <div class="pm__content__agreement__title"> 
	            		Loading the Agreement Data. Please be patient, because patience is a talent :)
            	</div>
            </div>
    		<!-- Show if no Agrrement Data exists -->
            <div class="pm__content__agreement" ng-if="self.showAgreementData && !self.isStillLoading() && self.showLoadingInfo && !self.agreementDataIsValid()">
	            <img class="pm__content__agreement__icon clickable"
	            		src="generated/icon-sprite.svg#ico36_close"
	            		ng-click="self.showAgreementData = !self.showAgreementData"/>
	            <div class="pm__content__agreement__title"> 
	            		No Agreement Data found
            	</div>
            </div>
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
            <!-- quick and dirty button to get agreements -->
	        <button class="won-button--filled thin black"
	            ng-click="self.showAgreementDataField()"
	            ng-show="!self.showAgreementData">
	                Show Agreement Data
		     </button>
        </div>
    `;



    class Controller {
        constructor(/* arguments = dependency injections */) {
            attach(this, serviceDependencies, arguments);
            window.pm4dbg = this;
            
            
            this.reload = true;
            
            this.showLoadingInfo = false;
            
            const self = this;
                     
            this.declarations = clone(declarations);
            
            this.agreementData = {
            		proposal: new Set(), 
            		agreement: new Set(), 
            		proposeToCancel: new Set(),
            		acceptedProposalToCancel: new Set(),
            };
            this.loading = {
            		proposal: false, 
            		agreement: false, 
            		proposeToCancel: false,
            		acceptedProposalToCancel: false,
            };
            
            this.showAgreementData = false;
            
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
                
                //Filter already accepted proposals
                let sortedMessages = chatMessages && chatMessages.toArray();
                if(sortedMessages) {
                	var msgSet = new Set(sortedMessages);
                	for(msg of msgSet) {
                		if(msg.get("isProposeMessage")){
	                		if(this.agreementData.agreement.has(msg.get("uri")) || this.agreementData.agreement.has(msg.get("remoteUri"))) {
	                			msgSet.delete(msg);
	                		} else {
	                			//TODO: add messages from state to agreementDate with right uri
	                			msg.get("remoteUri")? this.agreementData.proposal.add(msg.get("remoteUri")) : this.agreementData.proposal.add(msg.get("uri"));
	                		}
                		}
                	}
                	sortedMessages = Array.from(msgSet);
	            	sortedMessages.sort(function(a,b) {
	                    return a.get("date").getTime() - b.get("date").getTime();
	                });
	            	
	            	this.filterAgreementDataList();
                }
                //Load AgreementData from State -> faster than Api call
                /*
                if(chatMessages) {
                	for(msg of chatMessages.toArray()){                    	
		              	if(msg.get("isProposeMessage")) {
		              		this.agreementData.proposal.add(msg.get("uri"));
		              	}
		              	else if(msg.get("isAcceptMessage")) {
		              		this.agreementData.agreement.add(msg.get("uri"));
		              		chatMessages.filter(e => e !== msg);
		              	}
                	}
                	this.filterAgreementDataList();
                }*/
                
                /*
                if(sortedMessages){
                    sortedMessages.sort(function(a,b) {
                        return a.get("date").getTime() - b.get("date").getTime();
                    });
                }*/

                if(this.reload && connection) {
                	this.getAgreementData(connection)
                	this.reload = false;
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
                () => (this.chatMessages && this.chatMessages.length) || this.agreementData, // trigger if there's messages added (or removed)
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
        	this.showAgreementData = false;
            const trimmedMsg = this.chatMessage.trim();
            if(trimmedMsg) {
               this.connections__sendChatMessage(trimmedMsg, this.connection.get('uri'));
            }
        }
       
        showAgreementDataField() {
        	this.getAgreementData();
        	this.showLoadingInfo = true;
        	this.showAgreementData = true;
        	//TODO activate Component?
        }
        
        agreementDataIsValid() {
        	var aD = this.agreementData;
        	if(aD.proposal.size ||aD.agreement.size ||aD.proposeToCancel.size || aD.acceptedProposalToCancel.size){
        		return true;
        	}
        	return false;
        }
        
        getAgreementData(connection) {
        	if(connection) {
        		this.connection = connection;
        	}
        	console.log("Load Agreement Data");
        	this.startLoading();
        	this.getAgreements();
        	this.getProposals();
        	//this.getAgreementsProposedToBeCancelled();
        	//this.getAcceptedPropsalsToCancel();
        	
        	this.filterAgreementDataList();
        }
        
        updateAgreementData(uri) {
        	console.log("Is Accepted: " + uri);
        	
        	//TODO: Reaload AgreementData, refresh uri list
        }
        
        startLoading() {
        	this.loading.proposal = true;
        	this.loading.agreement = true;
        	/*
        	this.loading = {
        		proposal: true, 
        		agreement: true, 
        		proposeToCancel: true,
        		acceptedProposalToCancel: true,
            }*/
        }
        
        isStillLoading(){
        	if(!this.loading.proposal && !this.loading.agreement && !this.loading.proposeToCancel && !this.loading.acceptedProposalToCancel) {
        		return false;
        	}
        	return true;
        }
        
        //Filter and update the agreementData object
        filterAgreementDataList() {
        	for(agreement of this.agreementData.agreement) {
        		if(this.agreementData.proposal.has(agreement)) {
        			this.agreementData.proposal.delete(agreement)
        		}
        		if(this.agreementData.proposeToCancel.has(agreement)) {
        			this.agreementData.agreement.delete(agreement)
        		}
        	}
        	for(agreement of this.agreementData.acceptedProposalToCancel) {
        		if(this.agreementData.proposeToCancel.has(agreement)) {
        			this.agreementData.proposeToCancel.delete(agreement)
        		}
        	}
        }
        
        getAgreements() {
        	var url = '/owner/rest/highlevel/getAgreements?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			this.handleApiResponse(response, this.declarations.agreement);
    		}).catch(error => {
    				console.error('Error:', error);
    				this.loading.agreement = false;
    		})
        }
        
        getProposals() {
        	var url = '/owner/rest/highlevel/getProposals?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			this.handleApiResponse(response, this.declarations.proposal);
    		}).catch(error => {
				console.error('Error:', error);
				this.loading.proposal = false;
			})
        }
        
        getAgreementsProposedToBeCancelled() {
        	var url = '/owner/rest/highlevel/getAgreementsProposedToBeCancelled?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			this.handleApiResponse(response, this.declarations.proposeToCancel);
    		}).catch(error => {
				console.error('Error:', error);
				this.loading.proposeToCancel = false;
			})
        }
        
        getAcceptedPropsalsToCancel() {
        	var url = '/owner/rest/highlevel/getAcceptedPropsalsToCancel?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			this.handleApiResponse(response, this.declarations.acceptedPropsalToCancel);
    		}).catch(error => {
				console.error('Error:', error);
				this.loading.acceptedPropsalToCancel = false;
			})
        }
        
        handleApiResponse(response, type){
        	if(response["@id"]) {
				var eventUri = response["@id"];
				if(!this.agreementData[type].has(eventUri)) {
					this.parseResponseGraph(eventUri, type);
				}
			}
			else if(response["@graph"]) {
				var graph = response["@graph"];
				for(i = 0; i<graph.length; i++) {
					var eventUri = graph[i]["@id"];
					if(!this.agreementData[type].has(eventUri)) {
						this.parseResponseGraph(eventUri, type);
					}
				}
			}
			this.loading[type] = false;
        }
        
        //Create WonMessage and add to State
        parseResponseGraph(eventUri, type) {
            const ownNeedUri = this.ownNeed.get("uri");
            callAgreementEventFetch(ownNeedUri, eventUri)
			.then(response => {
				won.wonMessageFromJsonLd(response)
				.then(msg => {
                    if(msg.isFromOwner() && msg.getReceiverNeed() === ownNeedUri){
                        /*if we find out that the receiverneed of the crawled event is actually our
                        need we will call the method again but this time with the correct eventUri
                        */
                        this.parseResponseGraph(msg.getRemoteMessageUri(), type);
                    }else{
                        this.messages__connectionMessageReceived(msg);
                        this.agreementData[type].add(eventUri);
                    }
                })
			})
        }
        
        getArrayFromSet(set) {
        	return Array.from(set);
        }
             
        sendRdfTmpDeletme() { //TODO move to own component
        	this.showAgreementData = false;
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
