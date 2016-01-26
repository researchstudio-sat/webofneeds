;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import won from '../won-es6';
import { labels } from '../won-label-utils';

function genComponentConf() {
    let template = `
            <a ui-sref="postInfo({myUri: self.item.uri})">
                <won-square-image src="self.item.titleImgSrc" title="self.item.title"></won-square-image>
            </a>
            <a class="pil__description clickable" ui-sref="postInfo({myUri: self.item.uri})">
                <div class="pil__description__topline">
                    <div class="pil__description__topline__title">{{self.item.title}}</div>
                    <div class="pil__description__topline__creationdate">{{self.creationDate()}}</div>
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
                <a class="pil__indicators__item clickable" ui-sref="postConversations({myUri: self.item.uri})">
                    <img src="generated/icon-sprite.svg#ico36_message"
                         ng-show="self.unreadConversationsCount()"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_message_grey"
                         ng-show="!self.unreadConversationsCount()"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{ self.unreadConversationsCount() }}
                    </span>
                </a>
                <a class="pil__indicators__item clickable"  ui-sref="postRequests({myUri: self.item.uri})">
                    <img src="generated/icon-sprite.svg#ico36_incoming"
                         ng-show="self.unreadRequestsCount()"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_incoming_grey"
                         ng-show="!self.unreadRequestsCount()"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{ self.unreadRequestsCount() }}
                    </span>
                </a>
                <a class="pil__indicators__item clickable" ui-sref="postMatches({myUri: self.item.uri})">
                    <img src="generated/icon-sprite.svg#ico36_match"
                         ng-show="self.unreadMatchesCount()"
                         class="pil__indicators__item__icon">
                    <img src="generated/icon-sprite.svg#ico36_match_grey"
                         ng-show="!self.unreadMatchesCount()"
                         class="pil__indicators__item__icon">
                    <span class="pil__indicators__item__caption">
                        {{ self.unreadMatchesCount() }}
                    </span>
                </a>
            </div>
    `;

    class Controller {
        constructor() {
            window.pil4dbg = this; //TODO deletme
            this.labels = labels;
            //this.EVENT = won.EVENT;
        }

        creationDate() {
            return new Date(this.item.creationDate).toLocaleDateString();
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

