/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import { attach } from '../utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    selectAllOwnNeeds,
    selectAllConnections,
    selectAllMessages,
} from '../selectors.js';
import won from '../won-es6.js';


const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav ng-cloak ng-show="{{true}}" class="main-tab-bar">
            <div class="mtb__inner">
                <ul class="mtb__inner__center mtb__tabs">
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 0}"
                        class="clickable">
                        <a ng-click="self.router__stateGoResetParams('feed')"
                            ng-class="{'disabled' : !self.hasPosts}">
                            Feed
                        </a>
                    </li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 1}"
                        class="clickable">
                        <a ng-click="self.router__stateGoResetParams('overviewPosts')"
                            ng-class="{'disabled' : !self.hasPosts}"
                            class="clickable">
                            Posts
                            <span class="mtb__tabs__unread">{{ self.nrOfNeedsWithUnreadEvents }}</span>
                        </a>
                    </li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 2}"
                        class="clickable">
                        <a ng-click="self.router__stateGoResetParams('overviewIncomingRequests')"
                            ng-class="{'disabled' : !self.hasRequests}">
                            Incoming Requests
                            <span class="mtb__tabs__unread">{{ self.nrOfUnreadIncomingRequests }}</span>
                        </a>
                    </li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 3}"
                        class="clickable">
                        <a ng-click="self.router__stateGoResetParams('overviewMatches')"
                            ng-class="{'disabled' : !self.hasMatches}">
                            Matches
                            <span class="mtb__tabs__unread">{{ self.nrOfUnreadMatches }}</span>
                        </a>
                    </li>
                </ul>
                <div class="mtb__inner__right">
                    <a class="mtb__searchbtn clickable">
                        <img src="generated/icon-sprite.svg#ico36_search_nomargin" class="mtb__icon">
                    </a>
                </div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.otb4dbg = this;
            window.$ngRedux4dbg = this.$ngRedux;
            window.getState4dbg = this.$ngRedux.getState;

            const selectFromState = (state) => {
                const ownNeeds = selectAllOwnNeeds(state);
                const allConnections = selectAllConnections(state);
                const allMessages = selectAllMessages(state);

                const nrOfUnreadMessages= allMessages && allMessages.filter(msg => !msg.get("outgoingMessage") && msg.get("newMessage")).size; //only count incoming messages
                const nrOfUnreadIncomingRequests= allConnections && allConnections.filter(conn => conn.get("state") === won.WON.RequestReceived && conn.get("newConnection")).size;
                const nrOfUnreadMatches= allConnections && allConnections.filter(conn => conn.get("state") === won.WON.Suggested && conn.get("newConnection")).size;
                const nrOfNeedsWithUnreadEvents= undefined; //TODO: COUNT HOW MANY NEEDS HAVE AT LEAST ONE NEW CONNECTION OR ONE NEW MESSAGE

                return {
                    hasPosts: ownNeeds && ownNeeds.size > 0,
                    hasRequests: allConnections && allConnections.filter(conn => conn.get("state") === won.WON.RequestReceived).size > 0,
                    hasMatches: allConnections && allConnections.filter(conn => conn.get("state") === won.WON.Suggested).size > 0,
                    nrOfUnreadMessages: nrOfUnreadMessages ? nrOfUnreadMessages : undefined,
                    nrOfUnreadIncomingRequests: nrOfUnreadIncomingRequests ? nrOfUnreadIncomingRequests : undefined,
                    nrOfUnreadMatches: nrOfUnreadMatches ? nrOfUnreadMatches : undefined,
                    nrOfNeedsWithUnreadEvents: nrOfNeedsWithUnreadEvents ? nrOfNeedsWithUnreadEvents : undefined,
                };
            };

            connect2Redux(selectFromState, actionCreators, [], this);
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {selection: "="},
        template: template
    }
}

export default angular.module('won.owner.components.overviewTitleBar', [])
    .directive('wonOverviewTitleBar', genComponentConf)
    .name;
