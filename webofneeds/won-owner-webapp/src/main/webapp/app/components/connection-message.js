
;

import angular from 'angular';
import inviewModule from 'angular-inview';

import won from '../won-es6.js';
import jld from 'jsonld';
import Immutable from 'immutable';
import squareImageModule from './square-image.js';
import labelledHrModule from './labelled-hr.js';
import {
    relativeTime,
} from '../won-label-utils.js'
import {
    connect2Redux,
} from '../won-utils.js';
import {
    attach,
    delay,
    get,
    getIn,
    deepFreeze,
} from '../utils.js'
import {
	buildProposalMessage,
	buildModificationMessage,
} from '../won-message-utils.js';
import {
    actionCreators
}  from '../actions/actions.js';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
} from '../selectors.js';
import autoresizingTextareaModule from '../directives/textarea-autogrow.js';

const MESSAGE_READ_TIMEOUT = 1500;

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

function genComponentConf() {
    let template = `
        <won-square-image
            title="self.theirNeed.get('title')"
            src="self.theirNeed.get('TODOtitleImgSrc')"
            uri="self.theirNeed.get('uri')"
            ng-click="self.router__stateGoCurrent({postUri: self.theirNeed.get('uri')})"
            ng-show="!self.message.get('outgoingMessage')"><!-- TODO: MAKE THIS LINK WORK FOR SPECIFIC POST.JS/HTML usage too if the view will still exist then -->
        </won-square-image>
        <div class="won-cm__center"
                ng-class="{'won-cm__center--nondisplayable': !self.text}"
                in-view="$inview && self.markAsRead()">

            <div 
                class="won-cm__center__bubble" 
                title="{{ self.shouldShowRdf ? self.rdfToString(self.message.get('contentGraphs')) : undefined }}"
    			ng-class="{'agreement' : 	!self.isNormalMessage()}">
                    <span class="won-cm__center__bubble__text">
                    <span ng-show="self.message.get('isProposeMessage')"><h3>Proposal</h3></span>	
                	<span ng-show="self.message.get('isAcceptMessage')"><h3>Accept</h3></span>
                	<span ng-show="self.message.get('isProposeToCancel')"><h3>ProposeToCancel</h3></span>
                	<!--<span ng-show="self.message.get('isRetractMessage')"><h3>Retract</h3></span>-->
                        {{ self.text? self.text : self.noTextPlaceholder }}
                         <span class="won-cm__center__button" ng-if="self.isNormalMessage()">
	                        <svg class="won-cm__center__carret clickable"
	                                ng-click="self.showDetail = !self.showDetail"
	                                ng-if="self.allowProposals"
	                                ng-show="!self.showDetail">
	                            <use href="#ico16_arrow_down"></use>
	                        </svg>
	                        <span class="won-cm__center__carret clickable"
	                            ng-click="self.showDetail = !self.showDetail"
	                            ng-show="self.showDetail">
	                        	<won-labelled-hr arrow="'up'" style="margin-top: .5rem; margin-bottom: .5rem;"></won-labelled-hr>   
                    		</span>
                    	</span>
                    	<span ng-show="self.showDetail"><br /></span>
                    	<button class="won-button--filled thin black"
                        		ng-click="self.sendProposal(); self.showDetail = !self.showDetail"
                        		ng-show="self.showDetail">Propose <span ng-show="self.clicked">(again)</span>
                        </button>
                        <!--
                        <button class="won-button--filled thin black"
                        		ng-click="self.retractMessage(); self.showDetail = !self.showDetail"
                        		ng-show="self.showDetail && self.message.get('outgoingMessage')">
                        		Retract
                        </button>
                        -->
                    </span>

                    <br ng-show="self.shouldShowRdf && self.contentGraphTrig"/>
                    <hr ng-show="self.shouldShowRdf && self.contentGraphTrig"/>

                    <div 
                        class="clickable"
                        ng-click="self.showTrigPrefixes = !self.showTrigPrefixes" 
                        ng-show="self.shouldShowRdf && self.contentGraphTrig"
                    >
                        <div
                            class="won-cm__center__trig"
                            ng-show="self.contentGraphTrigPrefixes">
<!-- no intendation here, so it renders correctly -->
<code ng-show="!self.showTrigPrefixes">@prefix ...</code>
<code ng-show="self.showTrigPrefixes">{{ self.contentGraphTrigPrefixes }}</code>
                        </div>
                        <div class="won-cm__center__trig">
<!-- no intendation here, so it renders correctly -->
<code>{{ self.contentGraphTrig }}</code>
                        </div>
                    </div>

                    <!--
                    <div class="won-cm__center__button" 
                        ng-if="!self.message.get('isProposeMessage')
                            && !self.message.get('outgoingMessage')
                            && self.message.get('isAcceptMessage')
                            && !self.clicked">
                        <button class="won-button--filled thin black" ng-click="self.proposeToCancel()">Cancel</button>
                    </div>
                    -->
                    <div class="won-cm__center__button" 
                        ng-if="self.message.get('isProposeMessage')
                            && !self.message.get('isAcceptMessage')
                            && !self.message.isAccepted
                            && !self.clicked">
                        <button class="won-button--filled thin red" 
                        		ng-show="!self.message.get('outgoingMessage')" 
    							ng-click="self.acceptProposal()">
    						Accept
    					</button>
    					<!--
                        <button class="won-button--filled thin black"
    							ng-show="!self.message.get('outgoingMessage')"
    							ng-click="self.rejectMessage()">
    						Reject
    					</button>
    					<button class="won-button--filled thin black"
    							ng-click="self.retractMessage()">
    						Retract
    					</button>
    					-->
                    </div>
                    <div class="won-cm__center__button" 
                        ng-if="self.message.get('isProposeToCancel')
                            && !self.message.get('isAcceptMessage')
                            && !self.clicked">
                        <button class="won-button--filled thin red" 
                        		ng-show="!self.message.get('outgoingMessage')" 
                        		ng-click="self.acceptProposeToCancel()">
                        	Accept
                        </button>
                        <!-- <button class="won-button--filled thin black" ng-click="self.rejectMessage()">Reject</button> -->
                    </div>
            </div>
            <div
                ng-show="self.message.get('unconfirmed')"
                class="won-cm__center__time">
                    Pending&nbsp;&hellip;
            </div>
            <div
                ng-hide="self.message.get('unconfirmed')"
                class="won-cm__center__time">
                    {{ self.relativeTime(self.lastUpdateTime, self.message.get('date')) }}
            </div>
            <a ng-show="self.shouldShowRdf && self.message.get('outgoingMessage')"
                target="_blank"
                href="/owner/rest/linked-data/?requester={{self.encodeParam(self.ownNeed.get('uri'))}}&uri={{self.encodeParam(self.message.get('uri'))}}&deep=true">
                    <svg class="rdflink__small clickable">
                            <use href="#rdf_logo_2"></use>
                    </svg>
            </a>
            <a ng-show="self.shouldShowRdf && !self.message.get('outgoingMessage')"
                target="_blank"
                href="/owner/rest/linked-data/?requester={{self.encodeParam(self.ownNeed.get('uri'))}}&uri={{self.encodeParam(self.message.get('uri'))}}">
                    <svg class="rdflink__small clickable">
                        <use href="#rdf_logo_2"></use>
                    </svg>
            </a>
        </div>
    `;



    class Controller {
        constructor(/* arguments = dependency injections */) {
            attach(this, serviceDependencies, arguments);
            this.relativeTime = relativeTime;
            this.clicked = false;
            this.showDetail = false;
            
            window.cmsg4dbg = this;
            
            const self = this;

            self.noTextPlaceholder = 
                        '«This message couldn\'t be displayed as it didn\'t contain text! ' +
                        'Click on the \"RDF\"-logo in ' +
                        'the context-menu on the right side of the header to see the \"raw\" message-data.»'

            const selectFromState = state => {
                /*
                const connectionUri = selectOpenConnectionUri(state);
                */
                
                const ownNeed = this.connectionUri && selectNeedByConnectionUri(state, this.connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", this.connectionUri]);
                const theirNeed = connection && state.getIn(["needs", connection.get('remoteNeedUri')]);
                const message = connection && this.messageUri ? 
                    getIn(connection, ['messages', this.messageUri]) :
                    Immutable.Map();

                
                
                return {
                    ownNeed,
                    theirNeed,
                    connection,
                    message,
                    text: message.get('text'), 
                    contentGraphs: get(message, 'contentGraphs') || Immutable.List(),
                    contentGraphTrigPrefixes: getIn(message, ['contentGraphTrig', 'prefixes']),
                    contentGraphTrig: getIn(message, ['contentGraphTrig', 'body']),
                    lastUpdateTime: state.get('lastUpdateTime'),
                    shouldShowRdf: state.get('showRdf'),
                    allowProposals: connection && connection.get("state") === won.WON.Connected, //allow showing details only when the connection is already present
                }
            };

            connect2Redux(selectFromState, actionCreators, ['self.connectionUri', 'self.messageUri'], this);

            // gotta do this via a $watch, as the whole message parsing before 
            // this point happens synchronously but jsonLdToTrig needs to be async.
            /*
            this.$scope.$watch(
                () => this.contentGraphs,
                (newVal, oldVal) => {
                    won.jsonLdToTrig(newVal.toJS())
                    .then(trig => {
                        this.contentGraphTrig = trig;
                    })
                    .catch(e => {
                        this.contentGraphTrig = JSON.stringify(e);
                    })
                }
            )
            */
        }

        markAsRead(){
            if(this.message && this.message.get("unread")){
                const payload = {
                    messageUri: this.message.get("uri"),
                    connectionUri: this.connectionUri,
                    needUri: this.ownNeed.get("uri")
                };

                const tmp_messages__markAsRead = this.messages__markAsRead;

                setTimeout(function(){
                    tmp_messages__markAsRead(payload);
                }, MESSAGE_READ_TIMEOUT);
            }
        }
        
        sendProposal(){
        	this.clicked = true;
        	const uri = this.message.get("remoteUri")? this.message.get("remoteUri") : this.message.get("uri");
        	const trimmedMsg = buildProposalMessage(uri, "proposes", this.message.get("text"));
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	this.onUpdate();
        }
        
        acceptProposal() {
        	this.clicked = true;
        	//const trimmedMsg = this.buildProposalMessage(this.message.get("remoteUri"), "accepts", this.message.get("text"));
        	const msg = ("Accepted proposal : " + this.message.get("remoteUri"));
        	const trimmedMsg = buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	//TODO: isAccepted = true;
        	this.onUpdate();
        }
        
        proposeToCancel() {
        	this.clicked = true;
        	const uri = this.isOwn? this.message.get("uri") : this.message.get("remoteUri");
        	const msg = ("Propose to cancel agreement : " + uri);
        	const trimmedMsg = buildProposalMessage(uri, "proposesToCancel", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.onUpdate();
        }
        
        acceptProposeToCancel() {
        	this.clicked = true;
        	//const trimmedMsg = this.buildProposalMessage(this.message.get("remoteUri"), "accepts", this.message.get("text"));
        	const msg = ("Accepted propose to cancel : " + this.message.get("remoteUri"));
        	const trimmedMsg = buildProposalMessage(this.message.get("remoteUri"), "accepts", msg);
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	//TODO: isAccepted = true;
        	this.onUpdate();
        }
        
        retractMessage() {
        	this.clicked = true;
        	const uri = this.message.get("remoteUri")? this.message.get("remoteUri") : this.message.get("uri");
        	const trimmedMsg = buildModificationMessage(uri, "retracts", this.message.get("text"));
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	this.onUpdate();
        }
        
        rejectMessage() {
        	this.clicked = true;
        	const uri = this.message.get("remoteUri")? this.message.get("remoteUri") : this.message.get("uri");
        	const trimmedMsg = buildProposalMessage(uri, "rejects",  this.message.get("text"));
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
        	
        	this.onUpdate();
            
        }

        rdfToString(jsonld){
        	return JSON.stringify(jsonld);
        }
        
        isNormalMessage() {
        	return !(this.message.get('isProposeMessage') ||
					this.message.get('isAcceptMessage') || 
					this.message.get('isProposeToCancel') ||
					this.message.get('isRetractMessage'));
        }

        encodeParam(param) {
            var encoded = encodeURIComponent(param);
            // console.log("encoding: ",param);
            // console.log("encoded: ",encoded)
            return encoded;
        }

        
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { 
            messageUri: '=',
            connectionUri: '=',
            /*
             * Usage:
             *  on-update="::myCallback(draft)"
             */
            onUpdate: '&',
        },
        template: template,
    }
}

export default angular.module('won.owner.components.connectionMessage', [
    squareImageModule,
    labelledHrModule,
    inviewModule.name
])
    .directive('wonConnectionMessage', genComponentConf)
    .name;
