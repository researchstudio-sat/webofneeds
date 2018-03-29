;

import angular from 'angular';
import squareImageModule from '../components/square-image.js';
import won from '../won-es6.js';
import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import { labels, relativeTime, } from '../won-label-utils.js';
import {
    selectAllOwnNeeds,
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
            <div class="pil__indicators">
                <a
                    class="pil__indicators__item clickable"
                    ng-show="self.hasConversations"
                    ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.Connected})">
                        <svg class="pil__indicators__item__icon"
                            style="--local-primary:#F09F9F;"
                            ng-show="!self.unreadConversationsCount">
                                <use href="#ico36_message"></use>
                        </svg>

                        <svg style="--local-primary:var(--won-primary-color);"
                             ng-show="self.unreadConversationsCount"
                             class="pil__indicators__item__icon">
                                <use href="#ico36_message"></use>
                        </svg>

                        <span class="pil__indicators__item__caption" title="Number of chats with unread messages">
                            {{ self.unreadConversationsCount }}
                        </span>
                </a>
                <div class="pil__indicators__item" ng-show="!self.hasConversations" title="No chats in this post">
                    <svg class="pil__indicators__item__icon"
                        style="--local-primary:#CCD2D2;">
                            <use href="#ico36_message"></use>
                    </svg>
                     <span class="pil__indicators__item__caption"></span>
                </div>
                <a
                    class="pil__indicators__item clickable"
                    ng-show="self.hasRequests"
                    ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.Connected})"> <!-- TODO: set the connectionType to connected since we pulled these views together -->

                        <svg class="pil__indicators__item__icon"
                            style="--local-primary:#F09F9F;"
                            ng-show="!self.unreadRequestsCount">
                                <use href="#ico36_incoming"></use>
                        </svg>
                        <svg style="--local-primary:var(--won-primary-color);"
                            ng-show="self.unreadRequestsCount"
                            class="pil__indicators__item__icon">
                                <use href="#ico36_incoming"></use>
                        </svg>
                        <span class="pil__indicators__item__caption" title="Number of new requests">
                            {{ self.unreadRequestsCount }}
                        </span>
                </a>
                <div class="pil__indicators__item" ng-show="!self.hasRequests" title="No requests to this post">
                    <svg class="pil__indicators__item__icon"
                        style="--local-primary:#CCD2D2;">
                            <use href="#ico36_incoming"></use>
                    </svg>
                     <span class="pil__indicators__item__caption"></span>
                </div>
                <a
                    class="pil__indicators__item clickable"
                    ng-show="self.hasMatches"
                    ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.Suggested})">

                        <svg class="pil__indicators__item__icon"
                            style="--local-primary:#F09F9F;"
                            ng-show="!self.unreadMatchesCount">
                                <use href="#ico36_match"></use>
                        </svg>

                        <svg style="--local-primary:var(--won-primary-color);"
                            ng-show="self.unreadMatchesCount"
                            class="pil__indicators__item__icon">
                                <use href="#ico36_match"></use>
                        </svg>
                        <span class="pil__indicators__item__caption" title="Number of new matches">
                            {{ self.unreadMatchesCount }}
                        </span>
                </a>
                <div class="pil__indicators__item" ng-show="!self.hasMatches" title="No matches for this post">
                    <svg class="pil__indicators__item__icon"
                        style="--local-primary:#CCD2D2;">
                            <use href="#ico36_match"></use>
                    </svg>
                    <span class="pil__indicators__item__caption"></span>
                </div>
            </div>
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

                const unreadConversationsCount = conversations && conversations.filter(conn => (conn.get("messages").filter(msg => msg.get("newMessage")).size > 0)).size;
                const unreadRequestsCount = requests && requests.filter(conn => conn.get("newConnection")).size;
                const unreadMatchesCount = matches && matches.filter(conn => conn.get("newConnection")).size;

                return {
                    need,
                    ownNeed: need,
                    relativeCreationDate: need ?
                        relativeTime(state.get('lastUpdateTime'), need.get('creationDate')) :
                        "",
                    hasConversations: conversations && conversations.size > 0,
                    hasRequests: requests && requests.size > 0,
                    hasMatches: matches && matches.size > 0,
                    unreadConversationsCount: unreadConversationsCount > 0 ? unreadConversationsCount : undefined,
                    unreadRequestsCount: unreadRequestsCount > 0 ? unreadRequestsCount : undefined,
                    unreadMatchesCount: unreadMatchesCount > 0 ? unreadMatchesCount : undefined,
                    WON: won.WON,
                };
            };

            connect2Redux(selectFromState, actionCreators, ['self.needUri'], this);
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
    squareImageModule
])
    .directive('wonPostItemLine', genComponentConf)
    .name;

