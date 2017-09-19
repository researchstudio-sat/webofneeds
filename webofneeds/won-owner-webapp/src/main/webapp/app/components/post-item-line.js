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
                            <img src="generated/icon-sprite.svg#ico36_group"
                                 class="pil__description__subtitle__group__icon">
                             {{self.ownNeed.get('group')}}
                             <span class="pil__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="pil__description__subtitle__type">
                             {{self.labels.type[self.get('type')]}}
                        </span>
                    </div>
                </a>
            </div>
            <div class="pil__indicators">
                <a
                    class="pil__indicators__item clickable"
                    ng-show="self.hasConversations"
                    ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.Connected})">
                        <img src="generated/icon-sprite.svg#ico36_message_light"
                             ng-show="!self.unreadConversationsCount"
                             class="pil__indicators__item__icon">
                        <img src="generated/icon-sprite.svg#ico36_message"
                             ng-show="self.unreadConversationsCount"
                             class="pil__indicators__item__icon">
                        <span class="pil__indicators__item__caption">
                            {{ self.unreadConversationsCount }}
                        </span>
                </a>
                <div class="pil__indicators__item" ng-show="!self.hasConversations">
                    <img src="generated/icon-sprite.svg#ico36_message_grey"
                         class="pil__indicators__item__icon">
                     <span class="pil__indicators__item__caption"></span>
                </div>
                <a
                    class="pil__indicators__item clickable"
                    ng-show="self.hasRequests"
                    ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.RequestReceived})">
                        <img src="generated/icon-sprite.svg#ico36_incoming_light"
                                 ng-show="!self.unreadRequestsCount"
                                 class="pil__indicators__item__icon">
                        <img src="generated/icon-sprite.svg#ico36_incoming"
                             ng-show="self.unreadRequestsCount"
                             class="pil__indicators__item__icon">
                        <span class="pil__indicators__item__caption">
                            {{ self.unreadRequestsCount }}
                        </span>
                </a>
                <div class="pil__indicators__item" ng-show="!self.hasRequests">
                    <img src="generated/icon-sprite.svg#ico36_incoming_grey"
                         class="pil__indicators__item__icon">
                     <span class="pil__indicators__item__caption"></span>
                </div>
                <a
                    class="pil__indicators__item clickable"
                    ng-show="self.hasMatches"
                    ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.Suggested})">
                        <img src="generated/icon-sprite.svg#ico36_match_light"
                             ng-show="!self.unreadMatchesCount"
                             class="pil__indicators__item__icon">
                        <img src="generated/icon-sprite.svg#ico36_match"
                             ng-show="self.unreadMatchesCount"
                             class="pil__indicators__item__icon">
                        <span class="pil__indicators__item__caption">
                            {{ self.unreadMatchesCount }}
                        </span>
                </a>
                <div class="pil__indicators__item" ng-show="!self.hasMatches">
                    <img src="generated/icon-sprite.svg#ico36_match_grey"
                         class="pil__indicators__item__icon">
                     <span class="pil__indicators__item__caption"></span>
                </div>
            </div>
            <a class="debuglink" ng-show="self.debugmode" target="_blank" href="{{self.needUri}}"> [DATA]</a>
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

                const unreadConversationsCount = conversations && conversations.filter(conn => conn.get("newConnection")).size;
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
                    debugmode: won.debugmode,
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

