/**
 * Component for rendering the connection indicators as an svg images, with unread count and select handle on the latest (possibly unread) connnectionuri
 * Created by fsuda on 10.04.2017.
 */
import angular from 'angular';
import won from '../won-es6.js';
import 'ng-redux';
import { labels, } from '../won-label-utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    selectAllOwnNeeds,
} from '../selectors.js';

import {
    attach,
    sortByDate,
} from '../utils.js'
import {
    connect2Redux,
} from '../won-utils.js'

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <a
            class="indicators__item clickable"
            ng-show="self.latestConnectedUri"
            ng-click="self.setOpen(self.latestConnectedUri)">
                <svg class="indicators__item__icon"
                    style="--local-primary:#F09F9F;"
                    ng-show="!self.unreadConnectedCount">
                        <use href="#ico36_message"></use>
                </svg>

                <svg style="--local-primary:var(--won-primary-color);"
                     ng-show="self.unreadConnectedCount"
                     class="indicators__item__icon">
                        <use href="#ico36_message"></use>
                </svg>

                <span class="indicators__item__caption" title="Number of chats with unread messages">
                    {{ self.unreadConnectedCount }}
                </span>
        </a>
        <div class="indicators__item" ng-show="!self.latestConnectedUri" title="No chats in this post">
            <svg class="indicators__item__icon"
                style="--local-primary:var(--won-disabled-color);">
                    <use href="#ico36_message"></use>
            </svg>
             <span class="indicators__item__caption"></span>
        </div>
        <a
            class="indicators__item clickable"
            ng-show="self.latestIncomingRequestUri"
            ng-click="self.setOpen(self.latestIncomingRequestUri)">
                <svg class="indicators__item__icon"
                    style="--local-primary:#F09F9F;"
                    ng-show="!self.unreadRequestsCount">
                        <use href="#ico36_incoming"></use>
                </svg>
                <svg style="--local-primary:var(--won-primary-color);"
                    ng-show="self.unreadRequestsCount"
                    class="indicators__item__icon">
                        <use href="#ico36_incoming"></use>
                </svg>
                <span class="indicators__item__caption" title="Number of new requests">
                    {{ self.unreadRequestsCount }}
                </span>
        </a>
        <div class="indicators__item" ng-show="!self.latestIncomingRequestUri" title="No requests to this post">
            <svg class="indicators__item__icon"
                style="--local-primary:var(--won-disabled-color);">
                    <use href="#ico36_incoming"></use>
            </svg>
             <span class="indicators__item__caption"></span>
        </div>
        <a
            class="indicators__item clickable"
            ng-show="self.latestMatchUri"
            ng-click="self.setOpen(self.latestMatchUri)">

                <svg class="indicators__item__icon"
                    style="--local-primary:#F09F9F;"
                    ng-show="!self.unreadMatchesCount">
                        <use href="#ico36_match"></use>
                </svg>

                <svg style="--local-primary:var(--won-primary-color);"
                    ng-show="self.unreadMatchesCount"
                    class="indicators__item__icon">
                        <use href="#ico36_match"></use>
                </svg>
                <span class="indicators__item__caption" title="Number of new matches">
                    {{ self.unreadMatchesCount }}
                </span>
        </a>
        <div class="indicators__item" ng-show="!self.latestMatchUri" title="No matches for this post">
            <svg class="indicators__item__icon"
                style="--local-primary:var(--won-disabled-color);">
                    <use href="#ico36_match"></use>
            </svg>
            <span class="indicators__item__caption"></span>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.labels = labels;

            const selectFromState = (state) => {
                const ownNeeds = selectAllOwnNeeds(state);
                const need = ownNeeds && ownNeeds.get(this.needUri);
                const allConnectionsByNeedUri = need && need.get("connections");

                const matches = allConnectionsByNeedUri && allConnectionsByNeedUri.filter(conn => conn.get("state") === won.WON.Suggested);
                const requests = allConnectionsByNeedUri && allConnectionsByNeedUri.filter(conn => conn.get("state") === won.WON.RequestReceived);
                const connected = allConnectionsByNeedUri && allConnectionsByNeedUri.filter(conn =>conn.get("state") === won.WON.Connected);

                const unreadMatches = matches && matches.filter(conn => conn.get("unread"));
                const unreadRequests = requests && requests.filter(conn => conn.get("unread"));
                const unreadConversations = connected && connected.filter(conn => conn.get("unread"));

                const unreadMatchesCount = unreadMatches && unreadMatches.size;
                const unreadRequestsCount = unreadRequests && unreadRequests.size;
                const unreadConnectedCount = unreadConversations && unreadConversations.size;

                const sortedUnreadMatches = sortByDate(unreadMatches);
                const sortedUnreadRequests = sortByDate(unreadRequests);
                const sortedUnreadConversations = sortByDate(unreadConversations);

                return {
                    WON: won.WON,
                    need,
                    unreadConnectedCount: unreadConnectedCount > 0 ? unreadConnectedCount : undefined,
                    unreadRequestsCount: unreadRequestsCount > 0 ? unreadRequestsCount : undefined,
                    unreadMatchesCount: unreadMatchesCount > 0 ? unreadMatchesCount : undefined,
                    latestConnectedUri: this.retrieveLatestUri(connected),
                    latestIncomingRequestUri: this.retrieveLatestUri(requests),
                    latestMatchUri: this.retrieveLatestUri(matches),
                }
            };

            connect2Redux(
                selectFromState, actionCreators,
                ['self.needUri'],
                this
            );
        }

        /**
         * This method returns either the latest unread uri of the given connection elements, or the latest uri of a read connection, if nothing is found undefined is returned
         * @param elements connection elements to retrieve the latest uri from
         * @returns {*}
         */
        retrieveLatestUri(elements) {
            const unreadElements = elements && elements.filter(conn => conn.get("unread"));

            const sortedUnreadElements = sortByDate(unreadElements);
            const unreadUri = sortedUnreadElements && sortedUnreadElements[0] && sortedUnreadElements[0].get("uri");

            if(unreadUri){
                return unreadUri;
            }else{
                const sortedElements = sortByDate(elements);
                return sortedElements && sortedElements[0] && sortedElements[0].get("uri");
            }
        }

        setOpen(connectionUri) {
            this.onSelectedConnection({connectionUri: connectionUri}); //trigger callback with scope-object
            //TODO either publish a dom-event as well; or directly call the route-change
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            needUri: '=',
            onSelectedConnection: "&",
        },
        template: template
    }
}

export default angular.module('won.owner.components.connectionIndicators', [
])
    .directive('wonConnectionIndicators', genComponentConf)
    .name;
