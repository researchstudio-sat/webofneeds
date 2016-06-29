;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import won from '../won-es6';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import { labels, relativeTime, updateRelativeTimestamps   } from '../won-label-utils';
import { createSelector } from 'reselect';
import { selectAllByConnections } from '../selectors';

const serviceDependencies = ['$scope', '$interval', '$ngRedux'];
function genComponentConf() {
    let template = `
            <a ui-sref="post({postUri: self.item.uri})">
                <won-square-image  
                    ng-class="{'inactive' : !self.isActive()}" 
                    src="self.item.titleImgSrc" 
                    title="self.item.title"
                    uri="self.item.uri">
                </won-square-image>
            </a>
            <a class="pil__description clickable" ui-sref="post({postUri: self.item.uri})">
                <div class="pil__description__topline">
                    <div class="pil__description__topline__title">{{self.item.title}}</div>
                    <div class="pil__description__topline__creationdate">{{self.creationDate}}</div>
                </div>
                <div class="pil__description__subtitle">
                    <span class="pil__description__subtitle__group" ng-show="self.item.group">
                        <img src="generated/icon-sprite.svg#ico36_group"
                             class="pil__description__subtitle__group__icon">
                         {{self.item.group}}
                         <span class="pil__description__subtitle__group__dash"> &ndash; </span>
                    </span>
                    <span class="pil__description__subtitle__type">
                         {{self.labels.type[self.item.basicNeedType]}}
                    </span>
                </div>
            </a>
            <div class="pil__indicators">
                <a class="pil__indicators__item clickable" ui-sref="post({postUri: self.item.uri, connectionType: self.WON.Connected})">
                    <img src="generated/icon-sprite.svg#ico36_message_light"
                         ng-show="self.hasConversations && !self.unreadConversationsCount()"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_message"
                         ng-show="!self.hasConversations && self.unreadConversationsCount()"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_message_grey"
                         ng-show="!self.hasConversations && !self.unreadConversationsCount()"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{ self.unreadConversationsCount() }}
                    </span>
                </a>
                <a class="pil__indicators__item clickable"  ui-sref="post({postUri: self.item.uri, connectionType: self.WON.RequestReceived})">
                    <img src="generated/icon-sprite.svg#ico36_incoming_light"
                             ng-show="self.hasRequests && !self.unreadRequestsCount()"
                             class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_incoming"
                         ng-show="!self.hasRequests && self.unreadRequestsCount()"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_incoming_grey"
                         ng-show="!self.hasRequests && !self.unreadRequestsCount()"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{ self.unreadRequestsCount() }}
                    </span>
                </a>
                <a class="pil__indicators__item clickable" ui-sref="post({postUri: self.item.uri, connectionType: self.WON.Suggested})">
                    <img src="generated/icon-sprite.svg#ico36_match_light"
                         ng-show="self.hasMatches && !self.unreadMatchesCount()"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_match"
                         ng-show="!self.hasMatches && self.unreadMatchesCount()"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_match_grey"
                         ng-show="!self.hasMatches && !self.unreadMatchesCount()"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{ self.unreadMatchesCount() }}
                    </span>
                </a>
            </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.pil4dbg = this; //TODO deletme
            this.labels = labels;
            //this.EVENT = won.EVENT;

            const selectFromState = (state) => {
                const allConnectionsByNeedUri = selectAllByConnections(state)
                    .filter(conn => conn.getIn(['ownNeed', 'uri']) === this.item.uri);

                return {
                    hasConversations: allConnectionsByNeedUri.filter(conn => conn.getIn(['connection', 'hasConnectionState']) === won.WON.Connected).length > 0,
                    hasRequests: allConnectionsByNeedUri.filter(conn => conn.getIn(['connection', 'hasConnectionState']) === won.WON.RequestReceived).length > 0,
                    hasMatches: allConnectionsByNeedUri.filter(conn => conn.getIn(['connection', 'hasConnectionState']) === won.WON.Suggested).length > 0,
                    WON: won.WON
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            updateRelativeTimestamps(
                this.$scope,
                this.$interval,
                this.item.creationDate,
                t => this.creationDate = t);

        }

        isActive() {
            return this.item.state === won.WON.Active;
        }

        unreadXCount(type) {
            return !this.unreadCounts?
                undefined : //ensure existence of count object
                this.unreadCounts.get(type)
        }
        unreadMatchesCount() {
            return this.unreadXCount(won.EVENT.HINT_RECEIVED)
        }
        unreadRequestsCount() {
            return this.unreadXCount(won.EVENT.CONNECT_RECEIVED)
        }
        unreadConversationsCount() {
            return this.unreadXCount(won.EVENT.WON_MESSAGE_RECEIVED)
        }

    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            item: "=",
            unreadCounts: "="
        },
        template: template
    }
}

export default angular.module('won.owner.components.postItemLine', [
    squareImageModule
])
    .directive('wonPostItemLine', genComponentConf)
    .name;

