;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import won from '../won-es6';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import { labels, relativeTime, updateRelativeTimestamps   } from '../won-label-utils';
import { createSelector } from 'reselect';
import {
    selectAllByConnections,
    selectUnreadCountsByNeedAndType,
    selectOwnNeeds,
} from '../selectors';

const serviceDependencies = ['$scope', '$interval', '$ngRedux'];
function genComponentConf() {
    let template = `
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
                    <div class="pil__description__topline__title">{{self.ownNeed.get('title')}}</div>
                    <div class="pil__description__topline__creationdate">{{self.creationDate}}</div>
                </div>
                <div class="pil__description__subtitle">
                    <span class="pil__description__subtitle__group" ng-show="self.ownNeed.get('group')">
                        <img src="generated/icon-sprite.svg#ico36_group"
                             class="pil__description__subtitle__group__icon">
                         {{self.ownNeed.get('group')}}
                         <span class="pil__description__subtitle__group__dash"> &ndash; </span>
                    </span>
                    <span class="pil__description__subtitle__type">
                         {{self.labels.type[self.ownNeed.get('basicNeedType')]}}
                    </span>
                </div>
            </a>
            <div class="pil__indicators">
                <a
                    class="pil__indicators__item clickable"
                    ng-show="self.hasConversations"
                    ui-sref="post({postUri: self.needUri, connectionType: self.WON.Connected})">
                        <img src="generated/icon-sprite.svg#ico36_message_light"
                             ng-show="!self.unreadConversationsCount()"
                             class="pil__indicators__item__icon">
                        <img src="generated/icon-sprite.svg#ico36_message"
                             ng-show="self.unreadConversationsCount()"
                             class="pil__indicators__item__icon">
                        <span class="pil__indicators__item__caption">
                            {{ self.unreadConversationsCount() }}
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
                                 ng-show="!self.unreadRequestsCount()"
                                 class="pil__indicators__item__icon">
                        <img src="generated/icon-sprite.svg#ico36_incoming"
                             ng-show="self.unreadRequestsCount()"
                             class="pil__indicators__item__icon">
                        <span class="pil__indicators__item__caption">
                            {{ self.unreadRequestsCount() }}
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
                             ng-show="!self.unreadMatchesCount()"
                             class="pil__indicators__item__icon">
                        <img src="generated/icon-sprite.svg#ico36_match"
                             ng-show="self.unreadMatchesCount()"
                             class="pil__indicators__item__icon">
                        <span class="pil__indicators__item__caption">
                            {{ self.unreadMatchesCount() }}
                        </span>
                </a>
                <div class="pil__indicators__item" ng-show="!self.hasMatches">
                    <img src="generated/icon-sprite.svg#ico36_match_grey"
                         class="pil__indicators__item__icon">
                     <span class="pil__indicators__item__caption"></span>
                </div>
            </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.pil4dbg = this; //TODO deletme
            this.labels = labels;
            //this.EVENT = won.EVENT;

            const selectFromState = (state) => {

                const ownNeeds = selectOwnNeeds(state);
                const need = ownNeeds? ownNeeds.get(this.needUri) : undefined;

                const allConnectionsByNeedUri = selectAllByConnections(state)
                    .filter(conn => conn.getIn(['ownNeed', 'uri']) === this.needUri);

                const unreadCounts = selectUnreadCountsByNeedAndType(state).get(this.needUri);


                return {
                    hasConversations: allConnectionsByNeedUri
                        .filter(conn =>
                            conn.getIn(['connection', 'hasConnectionState']) === won.WON.Connected
                        ).size > 0,
                    hasRequests: allConnectionsByNeedUri
                        .filter(conn =>
                            conn.getIn(['connection', 'hasConnectionState']) === won.WON.RequestReceived
                        ).size > 0,
                    hasMatches: allConnectionsByNeedUri
                        .filter(conn =>
                            conn.getIn(['connection', 'hasConnectionState']) === won.WON.Suggested
                        ).size > 0,
                    WON: won.WON,
                    ownNeed: need,
                    unreadCounts,
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            updateRelativeTimestamps(
                this.$scope,
                this.$interval,
                this.ownNeed.get('creationDate'),
                t => this.creationDate = t);

        }

        isActive() {
            return this.ownNeed.get('state') === won.WON.Active;
        }

        unreadXCount(type) {
            return !this.unreadCounts?
                undefined : //ensure existence of count object
                this.unreadCounts.get(type)
        }
        unreadMatchesCount() {
            return this.unreadXCount(won.WONMSG.hintMessage);
        }
        unreadRequestsCount() {
            return this.unreadXCount(won.WONMSG.connectMessage);
        }
        unreadConversationsCount() {
            return this.unreadXCount(won.WONMSG.connectionMessage);
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

