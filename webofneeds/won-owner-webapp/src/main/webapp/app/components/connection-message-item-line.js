;

import won from '../won-es6';
import angular from 'angular';
import squareImageModule from '../components/square-image';
import { labels } from '../won-label-utils';
import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
            <div class="conn">
                 <div class="conn__item clickable" ng-class="self.openConversation === self.item? 'selected' : ''"  ng-click="self.openMessage(self.item)">
                    <won-square-image src="request.titleImgSrc" title="self.item.remoteNeed.title"></won-square-image>
                    <div class="conn__item__description">
                        <div class="conn__item__description__topline">
                            <div class="conn__item__description__topline__title">{{self.item.remoteNeed.title}}</div>
                            <div class="conn__item__description__topline__date">{{self.item.connection.timestamp}}</div>
                            <img class="conn__item__description__topline__icon" src="generated/icon-sprite.svg#ico_settings">
                        </div>
                        <div class="conn__item__description__subtitle">
                            <span class="conn__item__description__subtitle__group" ng-show="request.group">
                                <img src="generated/icon-sprite.svg#ico36_group" class="mil__item__description__subtitle__group__icon">{{self.item.group}}<span class="mil__item__description__subtitle__group__dash"> &ndash; </span>
                            </span>
                            <span class="conn__item__description__subtitle__type">{{self.labels.type[self.item.remoteNeed.basicNeedType]}}</span>
                        </div>
                        <div class="conn__item__description__message">
                            <span class="conn__item__description__message__indicator" ng-click="self.open(self.item.connection.uri)" ng-show="!self.read(self.item.connection.uri)"/>{{self.item.lastEvent.msg}}
                        </div>
                    </div>
                </div>
            </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.needconnmsg = this;
            this.labels = labels;
        }

        openMessage(item) {
            //this.events__read(item)
            this.openConversation = item;
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "=",
                open: "=",
                openConversation: "="},
        template: template
    }

}

export default angular.module('won.owner.components.needConnectionMessageLine', [])
    .directive('wonConnectionMessageItemLine', genComponentConf)
    .name;

