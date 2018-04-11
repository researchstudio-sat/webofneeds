;

import won from '../won-es6.js';
import angular from 'angular';
import jld from 'jsonld';
import Immutable from 'immutable';
import chatTextFieldSimpleModule from './chat-textfield-simple.js';
import connectionMessageModule from './connection-message.js';
import connectionAgreementModule from './connection-agreement.js';
import postHeaderModule from './post-header.js';
import labelledHrModule from './labelled-hr.js';

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
    dispatchEvent,
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
});

const defaultAgreementData = deepFreeze({
    agreementUris: new Set(),
    pendingProposalUris: new Set(),
    pendingProposals: new Set(),
    acceptedCancellationProposalUris: new Set(),
    cancellationPendingAgreementUris: new Set(),
    pendingCancellationProposalUris: new Set(),
    cancelledAgreementUris: new Set(),
    rejectedMessageUris: new Set(),
    retractedMessageUris: new Set(),
});
function genComponentConf() {
    let template = `
        <div class="pm__header">
            <a class="clickable"
               ng-click="self.router__stateGoCurrent({connectionUri : undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="pm__header__icon clickable">
                    <use href="#ico36_close"></use>
                </svg>
            </a>
            <won-post-header
                need-uri="self.theirNeed.get('uri')"
                timestamp="self.lastUpdateTimestamp"
                hide-image="::true">
            </won-post-header>
            <svg class="pm__header__icon__small clickable"
                style="--local-primary:#CCD2D2;" 
                ng-show="!self.contextMenuOpen" 
                ng-click="self.contextMenuOpen = true">
                    <use href="#ico16_arrow_down"></use>
            </svg>
            <div class="pm__header__contextmenu contextmenu" ng-show="self.contextMenuOpen">
                <div class="content">
                    <div class="topline">
                      <svg class="pm__header__icon__small__contextmenu clickable"
                        style="--local-primary:black;"
                        ng-click="self.contextMenuOpen = false">
                            <use href="#ico16_arrow_up"></use>
                      </svg>
                    </div>
                    <button
                        class="won-button--outlined thin red"
                        ng-click="self.goToPost()">
                        Show Post Details
                    </button>
                    <a class="won-button--outlined thin red"
                        target="_blank"
                        href="{{self.connectionUri}}"
                        ng-if="!self.isConnected">
                        <svg class="won-button-icon" style="--local-primary:var(--won-primary-color);">
                            <use href="#ico36_rdf_logo"></use>
                        </svg>
                        <span>Show RDF</span>
                    </a>
                    <a class="won-button--outlined thin red"
                        ng-click="self.toggleRdfDisplay()"
                        ng-if="self.isConnected">
                        <svg class="won-button-icon" style="--local-primary:var(--won-primary-color);">
                            <use href="#ico36_rdf_logo"></use>
                        </svg>
                        <span>{{self.shouldShowRdf? "Hide RDF" : "Show RDF"}}</span>
                    </a>
                    <button
                        ng-if="self.isConnected"
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        Close Connection
                    </button>
                    <button
                        ng-if="self.isSentRequest"
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        Cancel Request
                    </button>
                </div>
            </div>
        </div>
        <div class="pm__content">
            <div class="pm__content__loadspinner"
                ng-if="self.connection.get('isLoading')">
                <img src="images/spinner/on_white.gif"
                    alt="Loading&hellip;"
                    class="hspinner"/>
            </div>
            <button class="pm__content__loadbutton won-button--outlined thin red"
                ng-if="!self.connection.get('isLoading') && !self.allLoaded"
                ng-click="self.loadPreviousMessages()">
                Load previous messages
            </button>
            <won-connection-message
                ng-repeat="msg in self.chatMessages"
                connection-uri="self.connectionUri"
                message-uri="msg.get('uri')"
                ng-class="{
                    'won-unread' : msg.get('newMessage'),
                    'won-not-relevant': !msg.get('isRelevant'),
                    'won-cm--left' : !msg.get('outgoingMessage'),
                    'won-cm--right' : msg.get('outgoingMessage')
                }"
                on-update="[self.showAgreementData = false, self.filterMessages(draft)]">
                on-update="[self.showAgreementData = false, self.filterMessages(draft)]"
                on-send-proposal="::self.addProposal(proposalUri)">
            </won-connection-message>
            <div class="pm__content__agreement" ng-if="self.showAgreementData && self.agreementDataIsValid()">           	
	            <img class="pm__content__agreement__icon clickable"
            		src="generated/icon-sprite.svg#ico36_close"
            		ng-click="self.showAgreementData = !self.showAgreementData"/>
            	<!-- Agreements-->
            	<div class="pm__content__agreement__title" ng-show="self.agreementStateData.agreementUris.size || self.agreementStateData.cancellationPendingAgreementUris.size"> 
            		Agreements
            		<span ng-show="self.loading"> (loading...)</span>
            		<span ng-if="!self.loading"> (up-to-date)</span>
            	</div>
	            <won-connection-agreement
	            	ng-repeat="agreement in self.getArrayFromSet(self.agreementStateData.agreementUris) track by $index"
	                state-Uri="agreement.stateUri"
	                agreement-number="$index"
	                agreement-declaration="self.declarations.agreement"
	                connection-uri="self.connectionUri"
	                on-update="self.showAgreementData = false;">
	            </won-connection-agreement>
	            <!-- /Agreements -->
	            <!-- ProposeToCancel -->
	            <won-connection-agreement
	            	ng-repeat="proposeToCancel in self.getArrayFromSet(self.agreementStateData.cancellationPendingAgreementUris) track by $index"
	                state-Uri="proposeToCancel.stateUri"
	                cancel-uri="self.getCancelUri(proposeToCancel.headUri)"
	                own-cancel="self.checkOwnCancel(proposeToCancel.headUri)"
	                agreement-number="self.agreementStateData.agreementUris.size + $index"
	                agreement-declaration="self.declarations.proposeToCancel"
	                connection-uri="self.connectionUri"
	                on-update="[self.showAgreementData = false, self.filterMessages(draft)]">
	            </won-connection-agreement>
	            <!-- /ProposeToCancel -->           	
            	<!-- PROPOSALS -->
            	<div class="pm__content__agreement__title" ng-show="self.agreementStateData.pendingProposalUris.size">
            		<br ng-show="self.agreementStateData.agreementUris.size || self.agreementStateData.cancellationPendingAgreementUris.size" />
            		<hr ng-show="self.agreementStateData.agreementUris.size || self.agreementStateData.cancellationPendingAgreementUris.size" />
            		Proposals
    				<span ng-show="self.loading.pendingProposalUris"> (loading...)</span>
            		<span ng-if="!self.loading.pendingProposalUris"> (up-to-date)</span>
            	</div>
	            <won-connection-agreement
	            	ng-repeat="proposal in self.getArrayFromSet(self.agreementStateData.pendingProposalUris) track by $index"
	                state-Uri="proposal.stateUri"
	                agreement-number="$index"
	                agreement-declaration="self.declarations.proposal"
	                connection-uri="self.connectionUri"
	                on-update="self.showAgreementData = false;">
	            </won-connection-agreement>
	            <!-- /PROPOSALS -->
	            
            </div>
            <!-- Loading Text -->
            <div class="pm__content__agreement" ng-if="self.showAgreementData && self.showLoadingInfo && !self.agreementDataIsValid()">
	            <img class="pm__content__agreement__icon clickable"
	            		src="generated/icon-sprite.svg#ico36_close"
	            		ng-click="(self.showAgreementData = !self.showAgreementData) && (self.showLoadingInfo = !self.showLoadingInfo)"/>
	            <div class="pm__content__agreement__title"> 
	            		Loading the Agreement Data. Please be patient, because patience is a talent :)
            	</div>
            </div>
    		<!-- Show if no Agrrement Data exists -->
            <div class="pm__content__agreement" ng-if="self.showAgreementData && !self.loading && self.showLoadingInfo && !self.agreementDataIsValid()">
	            <img class="pm__content__agreement__icon clickable"
	            		src="generated/icon-sprite.svg#ico36_close"
	            		ng-click="self.showAgreementData = !self.showAgreementData"/>
	            <div class="pm__content__agreement__title">
	            		No Agreement Data found
	            		<span class="ng-hide" ng-show="loading">Loading the Agreement Data. Please be patient, because patience is a talent :)</span>
	            		<span class="ng-hide" ng-show="!loading">No Agreement Data found</span>
                </div>
            </div>
        </div>
        <div class="pm__footer" ng-if="self.isConnected">

            <chat-textfield-simple
                class="pm__footer__chattexfield"
                placeholder="self.shouldShowRdf? 'Enter ttl...' : 'Your message...'"
                submit-button-label="self.shouldShowRdf? 'Send RDF' : 'Send'"
                on-submit="self.send(value, self.shouldShowRdf)"
                help-text="self.shouldShowRdf? self.rdfTextfieldHelpText : ''"
                allow-empty-submit="::false"
                is-code="self.shouldShowRdf? 'true' : ''"
            >
            </chat-textfield-simple>

            <div class="pm__footer__agreement">
                <!-- quick and dirty button to get agreements -->
                <button class="won-button--filled thin black"
                    ng-click="self.showAgreementDataField()"
                    ng-show="!self.showAgreementData">
                        Show Agreement Data
                 </button>
            </div>
        </div>
        <div class="pm__footer" ng-show="self.isSentRequest">
            Waiting for them to accept your chat request.
        </div>

        <div class="pm__footer" ng-if="self.isReceivedRequest">
            <chat-textfield-simple
                class="pm__footer__chattexfield"
                placeholder="::'Reply Message (optional)'"
                on-submit="::self.openRequest(value)"
                allow-empty-submit="::true"
                submit-button-label="::'Accept Chat'"
            >
            </chat-textfield-simple>
            <won-labelled-hr label="::'Or'" class="pm__footer__labelledhr"></won-labelled-hr>
            <button class="pm__footer__button won-button--filled black" ng-click="self.closeConnection()">
                Decline
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
            this.baseString = "/owner/";
            this.declarations = clone(declarations);

            this.agreementHeadData = this.cloneDefaultData();
            this.agreementStateData = this.cloneDefaultSateData();
            this.showAgreementData = false;

            this.loading = false;

            this.rdfTextfieldHelpText = 'Expects valid turtle. ' +
            `<${won.WONMSG.msguriPlaceholder}> will ` +
            'be the uri generated for this message. See \`won.minimalTurtlePrefixes\` ' +
            'for prefixes that will be added automatically. E.g.' +
            `\`<${won.WONMSG.msguriPlaceholder}> won:hasTextMessage "hello world!". \``;


            this.scrollContainer().addEventListener('scroll', e => this.onScroll(e));

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

                	// TODO: Optimization
                	//filter proposals
                	/*
                	for(msg of msgSet) {
                		if(!msg.get("isRelevant")) {
                			console.log("Message is no longer relevant: " + msg);
                			msgSet.delete(msg);
                		}
                		/*
                		if(msg.get("isProposeMessage") || msg.get("isProposeToCancel") || msg.get("isAcceptMessage")) {
	                		if(this.isOldAgreementMsg(msg)) {
	                			msgSet.delete(msg);
	                		} else {
                                //TODO: optimization?
                            }
                        }
                    }*/

                    sortedMessages = Array.from(msgSet);
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
                    chatMessages: sortedMessages,
                    lastUpdateTimestamp: connection && connection.get('lastUpdateDate'),
                    isSentRequest: connection && connection.get('state') === won.WON.RequestSent,
                    isReceivedRequest: connection && connection.get('state') === won.WON.RequestReceived,
                    isConnected: connection && connection.get('state') === won.WON.Connected,
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
                () => (this.chatMessages && this.chatMessages.length), // trigger if there's messages added (or removed)
                () => delay(0).then(() =>
                        // scroll to bottom directly after rendering, if snapped
                        this.updateScrollposition()
                )
            )
        }

        ensureMessagesAreLoaded() {
            delay(0).then(() => {
                // make sure latest messages are loaded
                const INITIAL_MESSAGECOUNT = 15;
                if ( this.connection && !this.connection.get('isLoading') && !(this.allLoaded || this.connection.get('messages').size > 0)) {
                    this.connections__showLatestMessages(this.connection.get('uri'), INITIAL_MESSAGECOUNT);
                }
            })
        }

        loadPreviousMessages() {
            delay(0).then(() => {
                const MORE_MESSAGECOUNT = 5;
                if ( this.connection && !this.connection.get('isLoading') ) {
                    this.connections__showMoreMessages(this.connection.get('uri'), MORE_MESSAGECOUNT);
                }
            });

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

        send(chatMessage, isTTL=false) {
            this.showAgreementData = false;
            const trimmedMsg = chatMessage.trim();
            if(trimmedMsg) {
                this.connections__sendChatMessage(
                    trimmedMsg,
                    this.connection.get('uri'),
                    isTTL
                );
            }
        }

        showAgreementDataField() {
            this.getAgreementData();
            this.showLoadingInfo = true;
            this.showAgreementData = true;
        }

        agreementDataIsValid() {
            var aD = this.agreementStateData;
            if(aD.agreementUris.size ||aD.pendingProposalUris.size ||aD.cancellationPendingAgreementUris.size) {
                return true;
            }
            return false;
        }

        getAgreementData(connection) {
            if(connection) {
                this.connection = connection;
            }

        	this.$scope.loading = true;
        	//this.setLoading(true);
        	//this.loading["value"] = true;
        	this.agreementLoadingData = this.cloneDefaultStateData();
            this.getAgreementDataUris();
        }


        getAgreementDataUris() {
            var url = this.baseString + 'rest/agreement/getAgreementProtocolUris?connectionUri='+this.connection.get('uri');
            callAgreementsFetch(url)
                .then(response => {
                    this.agreementHeadData = this.transformDataToSet(response);
    			var keySet = new Set(["agreementUris", "pendingProposalUris", "cancellationPendingAgreementUris"])
                    for(key of keySet) {
                        if(this.agreementHeadData.hasOwnProperty(key)) {
                            for(data of this.agreementHeadData[key]) {
	    					this.addAgreementDataToSate(data, key);
                            }
                        }
                    }

    			    this.loading = false;
                }).then(response => {
    				this.agreementStateData = this.agreementLoadingData;
    				this.$scope.loading = false;
    				this.snapToBottom();
    		}).catch(error => {
    				console.error('Error:', error);
    				this.$scope.loading = false;
                })
        }


        transformDataToSet(response) {
            var tmpAgreementData = {
                agreementUris: new Set(response.agreementUris),
                pendingProposalUris: new Set(response.pendingProposalUris),
                pendingProposals: new Set(response.pendingProposals),
                acceptedCancellationProposalUris: new Set(response.acceptedCancellationProposalUris),
                cancellationPendingAgreementUris: new Set(response.cancellationPendingAgreementUris),
                pendingCancellationProposalUris: new Set(response.pendingCancellationProposalUris),
                cancelledAgreementUris: new Set(response.cancelledAgreementUris),
                rejectedMessageUris: new Set(response.rejectedMessageUris),
                retractedMessageUris: new Set(response.retractedMessageUris),
            }

            return this.filterAgreementSet(tmpAgreementData);
        }

        filterAgreementSet(tmpAgreementData) {
            for(prop of tmpAgreementData.cancellationPendingAgreementUris) {
                if(tmpAgreementData.agreementUris.has(prop)){
                    tmpAgreementData.agreementUris.delete(prop);
                }
            }

            return tmpAgreementData;
        }

        addAgreementDataToSate(eventUri, key, obj) {
            const ownNeedUri = this.ownNeed.get("uri");
            callAgreementEventFetch(ownNeedUri, eventUri)
            .then(response => {
                won.wonMessageFromJsonLd(response)
                .then(msg => {
                    var agreementObject = obj;

                    if(msg.isFromOwner() && msg.getReceiverNeed() === ownNeedUri){
                        /*if we find out that the receiverneed of the crawled event is actually our
                         need we will call the method again but this time with the correct eventUri
                         */
                        if(!agreementObject) {
                            agreementObject = this.cloneDefaultAgreementObject();
                        }
                        agreementObject.headUri = msg.getMessageUri();
                        this.addAgreementDataToSate(msg.getRemoteMessageUri(), key, agreementObject);
                    }else {
                        if(!agreementObject) {
                            agreementObject = this.cloneDefaultAgreementObject();
                            agreementObject.headUri = msg.getMessageUri();
                        }
                    
                    	agreementObject.stateUri = msg.getMessageUri();
                    	this.agreementLoadingData[key].add(agreementObject);
                    	
                    	//Dont load in state agein!
                    	var found = false;
                    	for(i = 0; i < this.chatMessages.length; i++) {
                    		if(agreementObject.stateUri === this.chatMessages[i].get("uri")) {
                    			found = true;
                    		}
                    	}
                    	if(!found) {
                    		this.messages__connectionMessageReceived(msg);
                    	}
                    }
                })
            })
        }

        filterAgreementStateData(agreementObject, remove) {
        	var keySet = new Set(["agreementUris", "pendingProposalUris", "cancellationPendingAgreementUris"]);
			for(key of keySet) {
    			this.checkObject(this.agreementStateData[key], agreementObject, remove)
			}
        }
        
        checkObject(data, agreementObject, remove) {
        	for(object of data) {
        		if(object.stateUri === agreementObject.stateUri) {
        			if(remove) {
        				data.delete(object);
        			}
        			return true;
        		}
        	}
        	
        	return false;
        }
        
        filterMessages(stateUri) {
        	var object = {
        			stateUri: stateUri,
        			headUri: undefined,
        	}
        	this.filterAgreementStateData(object, true);
        }
        
        getCancelUri(agreementUri) {
            const pendingProposals = this.agreementHeadData.pendingProposals;
            for(prop of pendingProposals) {
                if(prop.proposesToCancel.includes(agreementUri)){
                    return prop.uri;
                }
            }
            return undefined;
        }

        checkOwnCancel(headUri) {
            const pendingProposals = this.agreementHeadData.pendingProposals;
            for(prop of pendingProposals) {
                if(prop.proposesToCancel.includes(headUri)){
                    if(prop.proposingNeedUri === this.ownNeed.get("uri")) {
                        return true;
                    }
                }
            }
            return false;
        }

        startLoading() {
        	this.loading.proposal = true;
        	this.loading.agreement = true;
        	this.proposeToCancel = true;
        }


        isStillLoading(){
        	if(!this.loading.proposal && !this.loading.agreement && !this.loading.proposeToCancel/* && !this.loading.acceptedProposalToCancel*/) {
        		return false;
        	}
        	return true;
        }

        isOldAgreementMsg(msg) {
        	var aD = this.agreementHeadData
            if(aD.agreementUris.has(msg.get("uri")) ||
                aD.agreementUris.has(msg.get("remoteUri")) ||
                aD.cancellationPendingAgreementUris.has(msg.get("uri")) ||
                aD.cancellationPendingAgreementUris.has(msg.get("remoteUri")) ||
                aD.cancelledAgreementUris.has(msg.get("uri")) ||
                aD.cancelledAgreementUris.has(msg.get("remoteUri")) ||
                aD.acceptedCancellationProposalUris.has(msg.get("uri")) ||
                aD.acceptedCancellationProposalUris.has(msg.get("remoteUri"))) {
                return true;
            }
            return false;
        }

        getArrayFromSet(set) {
            return Array.from(set);
        }

        cloneDefaultData() {
            return defaultData = {
                agreementUris: new Set(),
                pendingProposalUris: new Set(),
                pendingProposals: new Set(),
                acceptedCancellationProposalUris: new Set(),
                cancellationPendingAgreementUris: new Set(),
                pendingCancellationProposalUris: new Set(),
                cancelledAgreementUris: new Set(),
                rejectedMessageUris: new Set(),
                retractedMessageUris: new Set(),
            };
        }

        cloneDefaultSateData() {
            return defaultStateData = {
                pendingProposalUris: new Set(),
                agreementUris: new Set(),
                cancellationPendingAgreementUris: new Set(),
            }
        }

        cloneDefaultAgreementObject() {
            return agreementObject = {
                stateUri: undefined,
                headUri: undefined,
            }
        }

        openRequest(message){
            this.connections__open(this.connectionUri, message);
        }

        closeConnection(){
            this.connections__close(this.connection.get('uri'));
            this.router__stateGoCurrent({connectionUri: null});
        }

        goToPost() {
            this.router__stateGoCurrent({postUri: this.connection.get('remoteNeedUri')});
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
    autoresizingTextareaModule,
    chatTextFieldSimpleModule,
    connectionMessageModule,
    connectionAgreementModule,
    postHeaderModule,
    labelledHrModule
])
    .directive('wonPostMessages', genComponentConf)
    .name;