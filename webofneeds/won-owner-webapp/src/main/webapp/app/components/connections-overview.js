/**
 * A list over all owned posts/needs with their connections
 * below each other. Usually limited to a connections of a
 * specific state (e.g. "hint")
 * Created by ksinger on 12.04.2017.
 */

import won from '../won-es6.js';
import angular from 'angular';
import squareImageModule from './square-image.js';
import postHeaderModule from './post-header.js';
import connectionIndicatorsModule from './connection-indicators.js';
import connectionSelectionItemModule from './connection-selection-item.js';

import {
    labels,
} from '../won-label-utils.js';

import {
    attach,
    sortByDate,
} from '../utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import { actionCreators }  from '../actions/actions.js';

import {
    selectAllOwnNeeds,
    selectRouterParams,
    selectNeedByConnectionUri,
} from '../selectors.js';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div ng-repeat="need in self.sortedNeeds">
            <div class="covw__own-need"
                ng-class="{'won-unread': need.get('unread'), 'selected' : need.get('uri') === self.needUriInRoute}">
                <won-post-header
                    need-uri="need.get('uri')"
                    timestamp="'TODOlatestOfThatType'"
                    ng-click="self.selectNeed(need.get('uri'))"
                    class="clickable">
                </won-post-header>
                <won-connection-indicators 
                    ng-show="need.get('state') === self.WON.ActiveCompacted"
                    on-selected-connection="self.selectConnection(connectionUri)" 
                    need-uri="need.get('uri')">
                </won-connection-indicators>
                <div ng-style="{'visibility': self.showConnectionsDropdown(need) ? 'visible' : 'hidden'}">
                    <svg
                        style="--local-primary:var(--won-secondary-color);"
                        class="covw__arrow clickable"
                        ng-show="self.isOpen(need.get('uri'))"
                        ng-click="self.closeConnections(need.get('uri'))" >
                            <use href="#ico16_arrow_up"></use>
                    </svg>
                    <svg style="--local-primary:var(--won-secondary-color);"
                        class="covw__arrow clickable"
                        ng-show="!self.isOpen(need.get('uri'))"
                        ng-click="self.openConnections(need.get('uri'))" >
                            <use href="#ico16_arrow_down"></use>
                    </svg>
                </div>
            </div>
            <won-connection-selection-item
                ng-if="self.isOpen(need.get('uri')) && self.showConnectionsDropdown(need)"
                ng-repeat="conn in self.getOpenConnectionsArraySorted(need)"
                on-selected-connection="self.selectConnection(connectionUri)"
                connection-uri="conn.get('uri')"
                ng-class="{'won-unread': conn.get('unread')}">
            </won-connection-selection-item>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.open = open;
            this.WON = won.WON;
            //this.labels = labels;
            window.co4dbg = this;

            const self = this;
            const selectFromState = (state)=> {
                const allOwnNeeds = selectAllOwnNeeds(state).filter(post => !(post.get("isWhatsAround") && post.get("state") === won.WON.InactiveCompacted)); //FILTER ALL CLOSED WHATS AROUNDS

                const routerParams = selectRouterParams(state);
                const connUriInRoute = routerParams && decodeURIComponent(routerParams['connectionUri']);
                const needUriInRoute = routerParams && decodeURIComponent(routerParams['postUri']);
                const needImpliedInRoute = connUriInRoute && selectNeedByConnectionUri(state, connUriInRoute);
                const needUriImpliedInRoute = needImpliedInRoute && needImpliedInRoute.get("uri");

                let sortedNeeds = self.sortNeeds(allOwnNeeds);

                return {
                    needUriInRoute,
                    needUriImpliedInRoute,
                    sortedNeeds: sortedNeeds,
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.connectionUri'], this);

            this.$scope.$watch(
                'self.needUriImpliedInRoute',
                (newValue, oldValue) => {
                    if(newValue && !oldValue) {
                        self.open[newValue] = true;
                    }
                }
            )
        }

        openConnections(ownNeedUri) {
            this.open[ownNeedUri] = true;
        }

        closeConnections(ownNeedUri) {
            this.open[ownNeedUri] = false;
            if(this.isOpenByConnection(ownNeedUri)) {
                this.router__stateGoCurrent({connectionUri: null});
            }
        }

        isOpen(ownNeedUri) {
            return this.isOpenByConnection(ownNeedUri) || !!this.open[ownNeedUri];
        }

        isOpenByConnection(ownNeedUri) {
            return this.needUriImpliedInRoute === ownNeedUri;
        }

        selectConnection(connectionUri) {
            this.onSelectedConnection({connectionUri}); //trigger callback with scope-object
        }
        selectNeed(needUri) {
            this.onSelectedNeed({needUri}); //trigger callback with scope-object
        }

        // sort needs by date and put closed needs at the end of the list
        sortNeeds(allNeeds) {
            openNeeds = sortByDate(allNeeds.filter(post => post.get("state") === won.WON.ActiveCompacted));
            closedNeeds = sortByDate(allNeeds.filter(post => post.get("state") === won.WON.InactiveCompacted));

            return openNeeds.concat(closedNeeds);
        }

        showConnectionsDropdown(need){
            return need.get("state") === won.WON.ActiveCompacted && need.get("connections").filter(conn => conn.get("state") !== won.WON.Closed).size > 0;
        }

        getOpenConnectionsArraySorted(need){
            return sortByDate(need.get('connections').filter(conn => conn.get('state') !== won.WON.Closed));
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            open: "=",
            //connectionType: "=",
            /*
             * Usage:
             *  on-selected-connection="myCallback(connectionUri)"
             */
            onSelectedConnection: "&",
            onSelectedNeed: "&",
        },
        template: template
    }

}



export default angular.module('won.owner.components.connectionsOverview', [
        squareImageModule,
        connectionSelectionItemModule,
        postHeaderModule,
        connectionIndicatorsModule,
])
    .directive('wonConnectionsOverview', genComponentConf)
    .name;
