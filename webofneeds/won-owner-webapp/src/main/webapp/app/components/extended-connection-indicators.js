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
            class="extendedindicators__item clickable"
            ng-show="self.latestConnectedUri"
            ng-click="self.setOpen(self.latestConnectedUri)">
                <svg class="extendedindicators__item__icon"
                    title="Show latest message/request"
                    style="--local-primary:#F09F9F;"
                    ng-show="!self.unreadConnectedCount">
                        <use xlink:href="#ico36_message" href="#ico36_message"></use>
                </svg>
                <svg style="--local-primary:var(--won-primary-color);"
                     title="Show latest unread message/request"
                     ng-show="self.unreadConnectedCount"
                     class="extendedindicators__item__icon">
                        <use xlink:href="#ico36_message" href="#ico36_message"></use>
                </svg>
                <span class="extendedindicators__item__caption">
                    {{ self.connectedCount }} Chats/Requests - {{ self.unreadConnectedCount }} unread
                </span>
        </a>
        <div class="extendedindicators__item" ng-show="!self.latestConnectedUri">
            <svg class="extendedindicators__item__icon"
                style="--local-primary:var(--won-disabled-color);">
                    <use xlink:href="#ico36_message" href="#ico36_message"></use>
            </svg>
             <span class="extendedindicators__item__caption">No Chats or Requests</span>
        </div>
        <a
            class="extendedindicators__item clickable"
            ng-show="self.latestMatchUri"
            ng-click="self.setOpen(self.latestMatchUri)">

                <svg class="extendedindicators__item__icon"
                    style="--local-primary:var(--won-primary-color-light);"
                    ng-show="!self.unreadMatchesCount">
                        <use xlink:href="#ico36_match" href="#ico36_match"></use>
                </svg>

                <svg style="--local-primary:var(--won-primary-color);"
                    ng-show="self.unreadMatchesCount"
                    class="extendedindicators__item__icon">
                        <use xlink:href="#ico36_match" href="#ico36_match"></use>
                </svg>
                <span class="extendedindicators__item__caption">
                    {{ self.matchesCount }} Matches - {{ self.unreadMatchesCount }} unread
                </span>
        </a>
        <div class="extendedindicators__item" ng-show="!self.latestMatchUri">
            <svg class="extendedindicators__item__icon"
                style="--local-primary:var(--won-disabled-color);">
                    <use xlink:href="#ico36_match" href="#ico36_match"></use>
            </svg>
            <span class="extendedindicators__item__caption">No Matches</span>
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
                const connected = allConnectionsByNeedUri && allConnectionsByNeedUri.filter(conn => conn.get("state") !== won.WON.Suggested && conn.get("state") !== won.WON.Closed);

                const unreadMatches = matches && matches.filter(conn => conn.get("unread"));
                const unreadConversations = connected && connected.filter(conn => conn.get("unread"));

                const matchesCount = matches ? matches.size : 0;
                const connectedCount = connected ? connected.size : 0;

                const unreadMatchesCount = unreadMatches ? unreadMatches.size : 0;
                const unreadConnectedCount = unreadConversations ? unreadConversations.size : 0;

                const sortedUnreadMatches = sortByDate(unreadMatches);
                const sortedUnreadConversations = sortByDate(unreadConversations);

                return {
                    WON: won.WON,
                    need,
                    connectedCount,
                    matchesCount,
                    unreadConnectedCount,
                    unreadMatchesCount,
                    latestConnectedUri: this.retrieveLatestUri(connected),
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

export default angular.module('won.owner.components.exctendedConnectionIndicators', [
])
    .directive('wonExtendedConnectionIndicators', genComponentConf)
    .name;
