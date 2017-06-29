/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import {
    selectUnreadCountsByType,
    selectUnreadEventsByNeed,
    selectAllByConnections,
} from '../selectors';
import won from '../won-es6';


const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav ng-cloak ng-show="{{true}}" class="main-tab-bar">
            <div class="mtb__inner">
                <ul class="mtb__inner__center mtb__tabs">
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 0}">
                        <a ui-sref="feed"
                            ng-class="{'disabled' : !self.hasPosts}">
                            Feed
                        </a>
                    </li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 1}">
                        <a ui-sref="overviewPosts"
                            ng-class="{'disabled' : !self.hasPosts}">
                            Posts
                            <span class="mtb__tabs__unread">{{ self.nrOfNeedsWithUnreadEvents }}</span>
                        </a>
                    </li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 2}">
                        <a ui-sref="overviewIncomingRequests"
                            ng-class="{'disabled' : !self.hasRequests}">
                            Incoming Requests
                            <span class="mtb__tabs__unread">{{ self.nrOfUnreadIncomingRequests }}</span>
                        </a>
                    </li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 3}">
                        <a ui-sref="overviewMatches()"
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
                const unreadCounts = selectUnreadCountsByType(state);
                const nrOfNeedsWithUnread = selectUnreadEventsByNeed(state).size;
                const ownNeeds = state.getIn(["needs", "ownNeeds"]);
                const connectionsDeprecated = selectAllByConnections(state).toJS();

                return {
                    hasPosts: ownNeeds && ownNeeds.size > 0,
                    hasRequests: Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.RequestReceived){
                                return true
                            }
                        }).length > 0,
                    hasMatches: Object.keys(connectionsDeprecated)
                        .map(key => connectionsDeprecated[key])
                        .filter(conn => {
                            if(conn.connection.hasConnectionState===won.WON.Suggested){
                                return true
                            }
                        }).length > 0,
                    nrOfUnreadMessages: unreadCounts.get(won.WONMSG.connectionMessage),
                    nrOfUnreadIncomingRequests: unreadCounts.get(won.WONMSG.connectMessage),
                    nrOfUnreadSentRequests: unreadCounts.get(won.WONMSG.connectSentMessage),
                    nrOfUnreadMatches: unreadCounts.get(won.WONMSG.hintMessage),
                    nrOfNeedsWithUnreadEvents: nrOfNeedsWithUnread > 0? nrOfNeedsWithUnread : undefined,
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
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
