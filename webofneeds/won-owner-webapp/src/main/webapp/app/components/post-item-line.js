;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import won from '../won-es6';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import { labels, relativeTime, } from '../won-label-utils';
import {
    selectAllOwnNeeds,
} from '../selectors';

const serviceDependencies = ['$scope', '$interval', '$ngRedux'];
function genComponentConf() {
    let template = `
            <div class="pil__information">
                <a ui-sref="post({postUri: self.needUri})">
                    <won-square-image  
                        ng-class="{'inactive' : !self.isActive()}" 
                        src="self.ownNeed.get('titleImgSrc')"
                        title="self.ownNeed.get('title')"
                        uri="self.needUri">
                    </won-square-image>
                </a>
                <a class="pil__description clickable" ui-sref="post({postUri: self.needUri})">
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
                    ui-sref="post({postUri: self.needUri, connectionType: self.WON.Connected})">
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
                    ui-sref="post({postUri: self.needUri, connectionType: self.WON.RequestReceived})">
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
                    ui-sref="post({postUri: self.needUri, connectionType: self.WON.Suggested})">
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
                const allConnectionsByNeedUri = need.get("connections");

                const conversations = allConnectionsByNeedUri.filter(conn =>conn.get("state") === won.WON.Connected);
                const requests = allConnectionsByNeedUri.filter(conn => conn.get("state") === won.WON.RequestReceived);
                const matches = allConnectionsByNeedUri.filter(conn => conn.get("state") === won.WON.Suggested);

                return {
                    need,
                    ownNeed: need,
                    relativeCreationDate: need ?
                        relativeTime(state.get('lastUpdateTime'), need.get('creationDate')) :
                        "",
                    hasConversations: converstations && conversations.size > 0,
                    hasRequests: requests && requests.size > 0,
                    hasMatches: matches && matches.size > 0,
                    unreadConversationsCount: conversations && conversations.filter(conn => conn.get("newConnection")).size,
                    unreadRequestsCount: requests && requests.filter(conn => conn.get("newConnection")).size,
                    unreadMatchesCount: matches && matches.filter(conn => conn.get("newConnection")).size,
                    WON: won.WON,
                    debugmode: won.debugmode,
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
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

