
;

import angular from 'angular';
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
    getIn,
    deepFreeze,
} from '../utils.js'
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
        <div class="won-cm__content">
            <div class="won-cm__content__text"
            	title="{{ self.shouldShowRdf ? self.rdfToString(self.message.get('contentGraphs')) : undefined }}"
            	ng-class="{'propose' : self.message.get('isProposeMessage')}">
                <span ng-show="self.message.get('isProposeMessage')"><h3>Proposal</h3></span>	
                <span ng-show="self.message.get('isAcceptMessage')"><h3>Agreement</h3></span>	
                {{ self.message.get('text') }}
                <div class="won-cm__content__button" 
                	ng-if="self.message.get('isProposeMessage') 
                		&& !self.message.get('outgoingMessage')
                		&& !self.message.get('isAcceptMessage')
                		&& !self.message.isAccepted
    					&& !self.clicked">
                	<button class="won-button--filled thin red" ng-click="self.acceptProposal()">Accept</button>
                </div>
                <!--
                <div class="won-cm__content__button" 
                	ng-if="self.message.get('outgoingMessage')
                		&& !self.message.get('isProposeMessage') 
                		&& !self.message.get('isAcceptMessage')">
                	<button class="won-button--filled thin black" ng-click="self.sendProposal()">Propose</button>
                </div>
                -->
            </div>
            <div
                ng-show="self.message.get('unconfirmed')"
                class="won-cm__content__time">
                    Pending&nbsp;&hellip;
            </div>
            <div
                ng-hide="self.message.get('unconfirmed')"
                class="won-cm__content__time">
                    {{ self.relativeTime(self.lastUpdateTime, self.message.get('date')) }}
            </div>
            <a
                ng-show="self.shouldShowRdf && self.message.get('outgoingMessage')"
                target="_blank"
                href="/owner/rest/linked-data/?requester={{self.encodeParam(self.ownNeed.get('uri'))}}&uri={{self.encodeParam(self.message.get('uri'))}}&deep=true">
                <svg class="rdflink__small clickable">
                        <use href="#rdf_logo_2"></use>
                </svg>
            </a>
                <a
                ng-show="self.shouldShowRdf && !self.message.get('outgoingMessage')"
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
            window.cmsg4dbg = this;
            
            const self = this;

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
                    lastUpdateTime: state.get('lastUpdateTime'),
                    shouldShowRdf: state.get('showRdf'),
                }
            };

            connect2Redux(selectFromState, actionCreators, ['self.connectionUri', 'self.messageUri'], this);

            this.$scope.$watch(
                () => this.message.get('outgoingMessage'),
                (newVal, oldVal) => this.updateAlignment(newVal)
            )
        }
        
        sendProposal(){
        	this.clicked = true;
        	const trimmedMsg = this.buildProposalMessage(this.messageUri, "proposes", this.message.get("text"));
        	this.connections__sendChatMessage(trimmedMsg, this.connectionUri, isTTL=true);
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
        },
        template: template,
    }
}

export default angular.module('won.owner.components.connectionMessage', [
    squareImageModule,
])
    .directive('wonConnectionMessage', genComponentConf)
    .name;
