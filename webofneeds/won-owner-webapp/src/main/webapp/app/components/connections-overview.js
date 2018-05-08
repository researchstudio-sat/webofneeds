/**
 * A list over all owned posts/needs with their connections
 * below each other. Usually limited to a connections of a
 * specific state (e.g. "hint")
 * Created by ksinger on 12.04.2017.
 */

import won from '../won-es6.js';
import angular from 'angular';
import ngAnimate from 'angular-animate';
import squareImageModule from './square-image.js';
import postHeaderModule from './post-header.js';
import connectionIndicatorsModule from './connection-indicators.js';
import extendedConnectionIndicatorsModule from './extended-connection-indicators.js';
import connectionSelectionItemModule from './connection-selection-item.js';
import createPostItemModule from './create-post-item.js';

import {
    labels,
} from '../won-label-utils.js';

import {
    attach,
    sortByDate,
    getIn,
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
        <won-create-post-item ng-class="{'selected' : self.showCreateView}"></won-create-post-item>
        <div ng-repeat="need in self.sortedOpenNeeds" class="co__item"
            ng-class="{'co__item--withconn' : self.isOpen(need.get('uri')) && self.hasOpenConnections(need)}">
            <div class="co__item__need" ng-class="{'won-unread': need.get('unread'), 'selected' : need.get('uri') === self.needUriInRoute}">
                <div class="co__item__need__header">
                    <won-post-header
                        need-uri="need.get('uri')"
                        timestamp="'TODOlatestOfThatType'"
                        ng-click="self.toggleDetails(need.get('uri'))"
                        class="clickable">
                    </won-post-header>
                    <won-connection-indicators
                        on-selected-connection="self.selectConnection(connectionUri)"
                        need-uri="need.get('uri')"
                        ng-if="!self.isOpen(need.get('uri'))">
                    </won-connection-indicators>
                    <div class="co__item__need__header__carret" ng-click="self.toggleDetails(need.get('uri'))">
                        <svg
                            style="--local-primary:var(--won-secondary-color);"
                            class="co__item__need__header__carret__icon clickable"
                            ng-if="self.isOpen(need.get('uri'))">
                                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                        </svg>
                        <svg style="--local-primary:var(--won-secondary-color);"
                            class="co__item__need__header__carret__icon clickable"
                            ng-if="!self.isOpen(need.get('uri'))">
                                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                        </svg>
                    </div>
                </div>
                <div class="co__item__need__detail" ng-if="self.isOpen(need.get('uri'))">
                    <won-extended-connection-indicators
                        class="co__item__need__detail__indicators"
                        on-selected-connection="self.selectConnection(connectionUri)"
                        need-uri="need.get('uri')">
                    </won-extended-connection-indicators>
                    <div class="co__item__need__detail__actions">
                        <button
                            ng-if="need.get('uri') !== self.needUriInRoute"
                            class="co__item__need__detail__actions__button won-button--outlined thin red"
                            ng-click="self.selectNeed(need.get('uri'))">
                            Show Post Details
                        </button>
                        <button
                            ng-if="need.get('uri') === self.needUriInRoute"
                            class="co__item__need__detail__actions__button won-button--outlined thin red"
                            ng-click="self.selectNeed(undefined)">
                            Hide Post Details
                        </button>
                    </div>
                </div>
            </div>
            <div class="co__item__connections"
                ng-if="self.isOpen(need.get('uri')) && self.hasOpenConnections(need)">
                <won-connection-selection-item
                    ng-repeat="conn in self.getOpenConnectionsArraySorted(need)"
                    on-selected-connection="self.selectConnection(connectionUri)"
                    connection-uri="conn.get('uri')"
                    ng-class="{'won-unread': conn.get('unread')}">
                </won-connection-selection-item>
            </div>
        </div>
        <div class="co__separator clickable" ng-class="{'co__separator--open' : self.showClosedNeeds}" ng-if="self.closedNeedsSize > 0" ng-click="self.toggleClosedNeedsDisplay()">
            <span class="co__separator__text">Closed Posts ({{ self.closedNeedsSize }})</span>
            <svg
                style="--local-primary:var(--won-secondary-color);"
                class="co__separator__arrow"
                ng-if="self.showClosedNeeds">
                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
            </svg>
            <svg style="--local-primary:var(--won-secondary-color);"
                class="co__separator__arrow"
                ng-if="!self.showClosedNeeds">
                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
            </svg>
        </div>
        <div class="co__closedNeeds" ng-if="self.showClosedNeeds && self.closedNeedsSize > 0">
            <div ng-repeat="need in self.sortedClosedNeeds" class="co__item">
                <div class="co__item__need" ng-class="{'won-unread': need.get('unread'), 'selected' : need.get('uri') === self.needUriInRoute}">
                    <div class="co__item__need__header">
                        <won-post-header
                            need-uri="need.get('uri')"
                            timestamp="'TODOlatestOfThatType'"
                            ng-click="self.toggleDetails(need.get('uri'))"
                            class="clickable">
                        </won-post-header>
                        <div class="co__item__need__header__carret" ng-click="self.toggleDetails(need.get('uri'))">
                            <svg
                                style="--local-primary:var(--won-secondary-color);"
                                class="co__item__need__header__carret__icon clickable"
                                ng-if="self.isOpen(need.get('uri'))">
                                    <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                            </svg>
                            <svg style="--local-primary:var(--won-secondary-color);"
                                class="co__item__need__header__carret__icon clickable"
                                ng-if="!self.isOpen(need.get('uri'))">
                                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                            </svg>
                        </div>
                    </div>
                    <div class="co__item__need__detail" ng-if="self.isOpen(need.get('uri'))">
                        <won-extended-connection-indicators
                            class="co__item__need__detail__indicators"
                            on-selected-connection="self.selectConnection(connectionUri)"
                            need-uri="need.get('uri')">
                        </won-extended-connection-indicators>
                        <div class="co__item__need__detail__actions">
                            <button
                                ng-if="need.get('uri') === self.needUriInRoute"
                                class="co__item__need__detail__actions__button won-button--outlined thin red"
                                ng-click="self.selectNeed(need.get('uri'))">
                                Show Post Details
                            </button>
                            <button
                                ng-if="need.get('uri') === self.needUriInRoute"
                                class="co__item__need__detail__actions__button won-button--outlined thin red"
                                ng-click="self.selectNeed(undefined)">
                                Hide Post Details
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="co__loadspinner"
            ng-if="self.isLoading">
            <img src="images/spinner/on_white.gif"
                alt="Loading&hellip;"
                class="hspinner"/>
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
                const allOwnNeeds = selectAllOwnNeeds(state); //FILTER ALL CLOSED WHATS AROUNDS

                const openNeeds = allOwnNeeds && allOwnNeeds.filter(post => post.get("state") === won.WON.ActiveCompacted);
                const closedNeeds = allOwnNeeds && allOwnNeeds.filter(post => post.get("state") === won.WON.InactiveCompacted && !post.get("isWhatsAround")); //Filter whatsAround needs automatically

                const routerParams = selectRouterParams(state);
                const showCreateView = getIn(state, ['router', 'currentParams', 'showCreateView']);
                const connUriInRoute = routerParams && decodeURIComponent(routerParams['connectionUri']);
                const needUriInRoute = routerParams && decodeURIComponent(routerParams['postUri']);
                const needImpliedInRoute = connUriInRoute && selectNeedByConnectionUri(state, connUriInRoute);
                const needUriImpliedInRoute = needImpliedInRoute && needImpliedInRoute.get("uri");

                let sortedOpenNeeds = sortByDate(openNeeds);
                let sortedClosedNeeds = sortByDate(closedNeeds);

                return {
                    isLoading: !state.get('initialLoadFinished'),
                    showClosedNeeds: state.get('showClosedNeeds'),
                    showCreateView,
                    needUriInRoute,
                    needUriImpliedInRoute,
                    sortedOpenNeeds,
                    sortedClosedNeeds,
                    closedNeedsSize: closedNeeds && closedNeeds.size > 0 ? closedNeeds.size : undefined,
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

        toggleDetails(ownNeedUri) {
            if(this.isOpen(ownNeedUri)){
                this.open[ownNeedUri] = false;
                if(this.isOpenByConnection(ownNeedUri)) {
                    this.router__stateGoCurrent({connectionUri: null});
                }
            }else{
                this.open[ownNeedUri] = true;
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

        hasOpenConnections(need){
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
        extendedConnectionIndicatorsModule,
        ngAnimate,
        createPostItemModule,
])
    .directive('wonConnectionsOverview', genComponentConf)
    .name;
