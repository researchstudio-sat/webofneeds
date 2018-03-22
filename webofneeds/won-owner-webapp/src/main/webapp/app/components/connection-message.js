
;

import angular from 'angular';
import won from '../won-es6.js';
import jld from 'jsonld';
import Immutable from 'immutable';
import squareImageModule from './square-image.js';
import chatTextFieldModule from './chat-textfield.js';
import chatTextFieldSimpleModule from './chat-textfield-simple.js';
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
} from '../won-message-utils.js';
import {
    actionCreators
}  from '../actions/actions.js';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
} from '../selectors.js';
import autoresizingTextareaModule from '../directives/textarea-autogrow.js';


const align = deepFreeze({
    left:  "won-cm--left",
    right: "won-cm--right",
});

const serviceDependencies = ['$ngRedux', '$scope', '$element'];

function genComponentConf() {
    let template = `
        <won-square-image
            title="self.theirNeed.get('title')"
            src="self.theirNeed.get('TODOtitleImgSrc')"
            uri="self.theirNeed.get('uri')"
            ng-click="self.router__stateGoAbs('post', {postUri: self.theirNeed.get('uri')})"
            ng-show="!self.message.get('outgoingMessage')">
        </won-square-image>
        <div class="won-cm__center" ng-class="{'won-cm__center--nondisplayable': !self.text}">

            <div 
                class="won-cm__center__bubble" 
                title="{{ self.shouldShowRdf ? self.rdfToString(self.message.get('contentGraphs')) : undefined }}"
    			ng-class="{'agreement' : (self.message.get('isProposeMessage') || self.message.get('isAcceptMessage') || self.message.get('isProposeToCancel'))}">
                    <span class="won-cm__center__bubble__text">
                    <span ng-show="self.message.get('isProposeMessage')"><h3>Proposal</h3></span>	
                	<span ng-show="self.message.get('isAcceptMessage')"><h3>Agreement</h3></span>
                	<span ng-show="self.message.get('isProposeToCancel')"><h3>ProposeToCancel</h3></span>		
                        {{ self.text? self.text : self.noTextPlaceholder }}
                         <span class="won-cm__center__button" 
	                        ng-if="self.message.get('outgoingMessage')
	                            && !self.message.get('isProposeMessage') 
	                            && !self.message.get('isAcceptMessage')
	                            && !self.message.get('isProposeToCancel')">
	                        <svg class="won-cm__center__carret clickable"
	                                ng-click="self.showDetail = !self.showDetail"
	                                ng-show="!self.showDetail">
	                            <use href="#ico16_arrow_down"></use>
	                        </svg>
	                        <svg class="won-cm__center__carret clickable"
	                                ng-click="self.showDetail = !self.showDetail"
	                                ng-show="self.showDetail">
	                            <use href="#ico16_arrow_up"></use>
                        </svg>
                        <button class="won-button--filled thin black" ng-click="self.sendProposal()" ng-show="self.showDetail">Propose</button>
                    	</span>
                    </span>
                    <br ng-show="self.shouldShowRdf && self.contentGraphTrig"/>
                    <hr ng-show="self.shouldShowRdf && self.contentGraphTrig"/>
                    <code ng-show="self.shouldShowRdf && self.contentGraphTrig">
                        {{ self.contentGraphTrig }}
                    </code>
                    <div class="won-cm__center__button" 
                        ng-if="self.message.get('isProposeMessage') 
                            && !self.message.get('outgoingMessage')
                            && !self.message.get('isAcceptMessage')
                            && !self.message.isAccepted
                            && !self.clicked">
                        <button class="won-button--filled thin red" ng-click="self.acceptProposal()">Accept</button>
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
                        'Click on the \"RDF\"-logo at ' + 
                        'the bottom of screen to see the \"raw\" message-data.»'

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
                    contentGraphTrig: get(message, 'contentGraphTrig') || "",
                    lastUpdateTime: state.get('lastUpdateTime'),
                    shouldShowRdf: state.get('showRdf'),
                }
            };

            connect2Redux(selectFromState, actionCreators, ['self.connectionUri', 'self.messageUri'], this);

            this.$scope.$watch(
                () => this.message.get('outgoingMessage'),
                (newVal, oldVal) => this.updateAlignment(newVal)
            )

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
        
        sendProposal(){
        	this.clicked = true;
        	const trimmedMsg = buildProposalMessage(this.messageUri, "proposes", this.message.get("text"));
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

        updateAlignment(isOutgoingMessage) {
            const classes = this.$element[0].classList;
            if(isOutgoingMessage) {
                classes.remove(align.left);
                classes.add(align.right);
            } else {
                classes.add(align.left);
                classes.remove(align.right);
            }
        }

        rdfToString(jsonld){
        	return JSON.stringify(jsonld);
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
            message: '=',
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
])
    .directive('wonConnectionMessage', genComponentConf)
    .name;
