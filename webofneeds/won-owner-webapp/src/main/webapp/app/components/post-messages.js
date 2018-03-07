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
//import won.owner.web.rest.highlevel.HighlevelProtocolsController;

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

const declarations = deepFreeze({
	proposal: "proposal",
	agreement: "agreement",
	proposeToCancel: "proposeToCancel",
	
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
            	<div class="pm__content__agreement__title" ng-show="self.agreementData.agreements.size"> 
            		Agreements
            		<span ng-show="self.loading.agreements"> (loading...)</span>
            		<span ng-show="!self.loading.agreements"> (up-to-date)</span>
            	</div>
	            <won-connection-agreement
	            	ng-repeat="agree in self.getArrayFromSet(self.agreementData.agreements) track by $index"
	                event-uri="agree"
	                agreement-number="$index"
	                agreement-declaration="self.declarations.agreement"
	                connection-uri="self.connectionUri">
	            </won-connection-agreement>
	            <!-- /Agreements -->
            	<!-- PROPOSALS -->
            	<div class="pm__content__agreement__title" ng-show="self.agreementData.proposals.size">
            		<br ng-show="self.agreementData.agreements.size" />
            		<hr ng-show="self.agreementData.agreements.size" />
            		Proposals
    				<span ng-show="self.loading.proposals"> (loading...)</span>
            		<span ng-show="!self.loading.proposals"> (up-to-date)</span>
            	</div>
	            <won-connection-agreement
	            	ng-repeat="prop in self.getArrayFromSet(self.agreementData.proposals) track by $index"
	                event-uri="prop"
	                agreement-number="$index"
	                agreement-declaration="self.declarations.proposal"
	                connection-uri="self.connectionUri">
	            </won-connection-agreement>
	            <!-- /PROPOSALS -->
            </div>
            <!-- Show if no Agrrement Data exists -->
            <div class="pm__content__agreement" ng-if="self.showAgreementData && !self.agreementDataIsValid() && !self.showLoadingInfo">
	            <img class="pm__content__agreement__icon clickable"
	            		src="generated/icon-sprite.svg#ico36_close"
	            		ng-click="self.showAgreementData = !self.showAgreementData"/>
	            <div class="pm__content__agreement__title"> 
	            		No Agreement Data found
            	</div>
            </div>
            <!-- Loading Text -->
            <div class="pm__content__agreement" ng-if="self.showAgreementData && self.isStillLoading && self.showLoadingInfo && !self.agreementDataIsValid()">
	            <img class="pm__content__agreement__icon clickable"
	            		src="generated/icon-sprite.svg#ico36_close"
	            		ng-click="(self.showAgreementData = !self.showAgreementData) && (self.showLoadingInfo = !self.showLoadingInfo)"/>
	            <div class="pm__content__agreement__title"> 
	            		Loading the Agreement Data. Please be patient, because patience is a talent :)
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
            		proposals: new Set(), 
            		agreements: new Set(), 
            		proposeToCancel: new Set(),
            		acceptedProposalsToCancel: new Set(),
            };
            this.loading = {
            		proposals: false, 
            		agreements: false, 
            		proposeToCancel: false,
            		acceptedProposalsToCancel: false,
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
                
                let sortedMessages = chatMessages && chatMessages.toArray();
                if(sortedMessages){
                    sortedMessages.sort(function(a,b) {
                        return a.get("date").getTime() - b.get("date").getTime();
                    });
                }

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
        	if(aD.proposals.size ||aD.agreements.size ||aD.proposeToCancel.size || aD.acceptedProposalsToCancel.size){
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
        	
        	
        	//this.sendAgreementsOverviewMsg();
        }
        
        startLoading() {
        	this.loading = {
        		proposals: true, 
        		agreements: true, 
        		proposeToCancel: true,
        		acceptedProposalsToCancel: true,
            };
        }
        
        isStillLoading(){
        	var ld = this.loading
        	if(!ld.proposals && !ld.agreements && !ld.proposeToCancel && !ld.acceptedProposalsToCancel) {
        		return true;
        	}
        	return false;
        }
        
        getAgreements() {
        	var url = '/owner/rest/highlevel/getAgreements?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			if(response["@id"]) {
    				var uri = response["@id"];
    				if(!this.agreementData.agreements.has(uri)) {
	    				this.parseResponseGraph(uri);
	    				this.agreementData.agreements.add(uri);
	    				this.loading.agreements = false;
    				}
				}
    			else if(response["@graph"]) {
    				var graph = response["@graph"];
    				for(i = 0; i<graph.length; i++) {
    					var uri = graph[i]["@id"];
    					if(!this.agreementData.agreements.has(uri)) {
	    					this.parseResponseGraph(uri);
	    					this.agreementData.agreements.add(uri);
    					}
    				}
    				this.loading.agreements = false;
				}
    			this.loading.agreements = false;
    		}).catch(error => console.error('Error:', error))
        }
        
        getProposals() {
        	var url = '/owner/rest/highlevel/getProposals?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			if(response["@id"]) {
    				var uri = response["@id"];
    				if(!this.agreementData.proposals.has(uri)) {
	    				this.parseResponseGraph(uri);
	    				this.agreementData.proposals.add(uri);
	    				this.loading.proposals = false;
    				}
				}
    			else if(response["@graph"]) {
    				var graph = response["@graph"];
    				for(i = 0; i<graph.length; i++) {
    					var uri = graph[i]["@id"];
    					if(!this.agreementData.proposals.has(uri)) {
	    					this.parseResponseGraph(uri);
	    					this.agreementData.proposals.add(uri);
    					}
    				}
    				this.loading.proposals = false;
				}
    			this.loading.proposals = false;
    		}).catch(error => console.error('Error:', error))
        }
        
        /*
        getAgreementsProposedToBeCancelled() {
        	var url = '/owner/rest/highlevel/getAgreementsProposedToBeCancelled?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			if(response["@graph"]) {
    				this.agreementData.proposeToCancel = this.parseAgreementsData(Array.from(response['@graph']));
    				this.loading.proposeToCancel = false;
    			}
    		}).catch(error => console.error('Error:', error))
        }
        
        getAcceptedPropsalsToCancel() {
        	var url = '/owner/rest/highlevel/getAcceptedPropsalsToCancel?connectionUri='+this.connection.get('uri');
        	callAgreementsFetch(url)
    		.then(response => {
    			if(response["@graph"]) {
    				this.agreementData.acceptedProposalsToCancel = this.parseAgreementsData(Array.from(response['@graph']));
    				this.loading.acceptedProposalsToCancel = false;
    			}
    		}).catch(error => console.error('Error:', error))
        }
        */
        
        //Create WonMEssage and add to State
        parseResponseGraph(eventUri) {
        	callAgreementEventFetch(this.ownNeed.get("uri"), eventUri)
			.then(response => {
				won.wonMessageFromJsonLd(response)
				.then(msg => {
                    if(msg.getSenderNeed() === this.ownNeed.get("uri")){
					    this.messages__connectionMessageReceived(msg);
                    }else{
                        parseResponseGraph(msg.getRemoteMessageUri());
                    }
                })
			})
        }
        
        getArrayFromSet(set) {
        	return Array.from(set);
        }
        
        parseAgreementsData(obj) {
        	var list = [];
        	const getText = "http://purl.org/webofneeds/model#hasTextMessage";
        	
        	if(obj.length < 2) {
        		list.push({id: obj["@id"], text: obj["graph"][0][getText]});
        	}else {
	        	for(i = 0; i < obj.length; i++){
		        	//list.push({id: obj[i]["@graph"][0]["@id"], text: obj[i]["@graph"][0][getText]});
	        		list.push({id: obj[i]["@id"], text: obj[i]["@graph"][0][getText]});
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
        	this.connections__sendChatMessage(msg, this.connection.get('uri'));
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
