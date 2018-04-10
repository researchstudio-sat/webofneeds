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
                            <span class="mtb__tabs__unread">{{ self.nrOfUnreadNeeds }}</span>
                        </a>
                    </li>
                    <li ng-class="{'mtb__tabs__selected' : self.selection == 2}"
                        class="clickable">
                        <a ng-click="self.router__stateGoResetParams('overviewIncomingRequests')"
                            ng-class="{'disabled' : !self.hasConnections}">
                            Chats
                            <span class="mtb__tabs__unread"> {{ self.nrOfUnreadConnections }}</span>
                        </a>
                    </li>
                </ul>
                <div class="mtb__inner__right">
                    <a class="mtb__searchbtn clickable">
                        <svg style="--local-primary:var(--won-primary-color);"
                            class="mtb__icon">
                                <use href="#ico36_search_nomargin"></use>
                        </svg>
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

                const nrOfUnreadNeeds = ownNeeds && ownNeeds.filter(need => need.get("unread")).size;

                const nonClosedConnections = allConnections && allConnections.filter(conn => conn.get("state") !== won.WON.Closed);
                const nrOfUnreadConnections = nonClosedConnections && nonClosedConnections.filter(conn => conn.get("unread"));

                return {
                    hasPosts: ownNeeds && ownNeeds.size > 0,
                    hasConnections: allConnections && allConnections.filter(conn => conn.get("state") !== won.WON.Closed).size > 0,
                    nrOfUnreadNeeds: nrOfUnreadNeeds > 0 ? nrOfUnreadNeeds : undefined,
                    nrOfUnreadConnections: nrOfUnreadConnections > 0 ? nrOfUnreadConnections : undefined,
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