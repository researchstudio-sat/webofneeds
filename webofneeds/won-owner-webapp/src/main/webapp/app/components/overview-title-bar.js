/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import {    selectUnreadCountsByType,
            selectUnreadEventsByNeed } from '../selectors';
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
                            <span class="mtb__tabs__unread">{{ self.unreadRequests }}</span>
                        </a>
                    </li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 3}">
                        <a ui-sref="overviewMatches"
                            ng-class="{'disabled' : !self.hasMatches}">
                            Matches
                            <span class="mtb__tabs__unread">{{ self.unreadMatches }}</span>
                        </a>
                    </li>
                </ul>
                <div class="mtb__inner__right">
                    <a href="#" class="mtb__searchbtn">
                        <img src="generated/icon-sprite.svg#ico36_search_nomargin" class="mtb__icon">
                    </a>
                </div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const selectFromState = (state) => {
                const unreadCounts = selectUnreadCountsByType(state);
                const nrOfNeedsWithUnread = selectUnreadEventsByNeed(state).size;

                return {
                    hasPosts: state.getIn(["needs", "ownNeeds"]).length > 0,
                    hasRequests: Object.keys(state.getIn(['connections','connections']).toJS())
                        .map(key=>state.getIn(['connections','connections']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.RequestReceived){
                                return true
                            }
                        }).length > 0,
                    hasMatches: Object.keys(state.getIn(['connections','connections']).toJS())
                        .map(key=>state.getIn(['connections','connections']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.Suggested){
                                return true
                            }
                        }).length > 0,
                    unreadRequests: unreadCounts.get(won.EVENT.CONNECT_RECEIVED),
                    unreadMatches: unreadCounts.get(won.EVENT.HINT_RECEIVED),
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
