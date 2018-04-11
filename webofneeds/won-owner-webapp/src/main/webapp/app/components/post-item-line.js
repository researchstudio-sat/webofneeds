;

import angular from 'angular';
import squareImageModule from './square-image.js';
import connectionIndicatorsModule from './connection-indicators.js';
import won from '../won-es6.js';
import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import { labels, relativeTime, } from '../won-label-utils.js';
import {
    selectAllOwnNeeds,
    selectNeedByConnectionUri,
} from '../selectors.js';
import {
    connect2Redux,
} from '../won-utils.js';

const serviceDependencies = ['$scope', '$interval', '$ngRedux'];
function genComponentConf() {
    let template = `
            <div class="pil__information">
                <a ng-click="self.router__stateGoAbs('post', {postUri: self.needUri})"
                    class="clickable">
                    <won-square-image  
                        ng-class="{'inactive' : !self.isActive()}" 
                        src="self.ownNeed.get('titleImgSrc')"
                        title="self.ownNeed.get('title')"
                        uri="self.needUri">
                    </won-square-image>
                </a>
                <a class="pil__description clickable" ng-click="self.router__stateGoAbs('post', {postUri: self.needUri})">
                    <div class="pil__description__topline">
                        <div class="pil__description__topline__title">{{self.need.get('title')}}</div>
                        <div class="pil__description__topline__creationdate">{{self.relativeCreationDate}}</div>
                    </div>
                    <div class="pil__description__subtitle">
                        <span class="pil__description__subtitle__group" ng-show="self.need.get('group')">
                            <svg style="--local-primary:var(--won-primary-color);"
                                 class="pil__description__subtitle__group__icon">
                                    <use href="#ico36_group"></use>
                            </svg>
                             {{self.ownNeed.get('group')}}
                             <span class="pil__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="pil__description__subtitle__type">
                             {{self.labels.type[self.need.get('type')]}}{{self.need.get('matchingContexts')? ' in '+ self.need.get('matchingContexts').join(', ') : ' (no matching context specified)' }}
                        </span>
                    </div>
                </a>
            </div>
            <won-connection-indicators need-uri="self.need.get('uri')" on-selected-connection="self.selectConnection(connectionUri)"></won-connection-indicators>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.pil4dbg = this; //TODO deletme
            this.labels = labels;
            const self = this;

            const selectFromState = (state) => {
                const ownNeeds = selectAllOwnNeeds(state);
                const need = ownNeeds && ownNeeds.get(self.needUri);
                const allConnectionsByNeedUri = need && need.get("connections");

                const conversations = allConnectionsByNeedUri && allConnectionsByNeedUri.filter(conn =>conn.get("state") === won.WON.Connected);
                const requests = allConnectionsByNeedUri && allConnectionsByNeedUri.filter(conn => conn.get("state") === won.WON.RequestReceived);
                const matches = allConnectionsByNeedUri && allConnectionsByNeedUri.filter(conn => conn.get("state") === won.WON.Suggested);

                const unreadConversationsCount = conversations && conversations.filter(conn => (conn.get("messages").filter(msg => msg.get("unread")).size > 0)).size;
                const unreadRequestsCount = requests && requests.filter(conn => conn.get("unread")).size;
                const unreadMatchesCount = matches && matches.filter(conn => conn.get("unread")).size;

                return {
                    need,
                    ownNeed: need,
                    relativeCreationDate: need ?
                        relativeTime(state.get('lastUpdateTime'), need.get('creationDate')) :
                        "",
                    WON: won.WON,
                };
            };

            connect2Redux(selectFromState, actionCreators, ['self.needUri'], this);
        }

        selectConnection(connectionUri) {
            this.markAsRead(connectionUri);
            this.router__stateGoAbs('post', {connectionUri: connectionUri, postUri: this.needUri, connectionType: won.WON.Connected});
        }

        markAsRead(connectionUri) {
            const need = selectNeedByConnectionUri(this.$ngRedux.getState(), connectionUri);
            const connections = need && need.get("connections");
            const connection = connections && connections.get(connectionUri);

            if (connection && connection.get("unread") && connection.get("state") !== won.WON.Connected) {
                const payload = {
                    connectionUri: connectionUri,
                    needUri: this.needUri,
                };

                this.connections__markAsRead(payload);
            }
        }

        isActive() {
            return this.ownNeed && this.ownNeed.get("state") === won.WON.ActiveCompacted;
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            needUri: "=",
        },
        template: template
    }
}

export default angular.module('won.owner.components.postItemLine', [
    squareImageModule,
    connectionIndicatorsModule,
])
    .directive('wonPostItemLine', genComponentConf)
    .name;

