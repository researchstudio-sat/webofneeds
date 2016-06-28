import angular from 'angular';
import squareImageModule from '../components/square-image';
import won from '../won-es6';
import { attach } from '../utils';
import { labels, updateRelativeTimestamps } from '../won-label-utils';

const serviceDependencies = ['$scope', '$interval'];
function genComponentConf() {
    let template = `
            <div class="fi clickable" ui-sref="post({postUri: self.item.get('uri')})">
                <won-square-image 
                    src="self.item.get('titleImg')" 
                    title="self.item.get('title')"
                    uri="self.item.get('uri')">
                </won-square-image>
                <div class="fi__description">
                    <div class="fi__description__topline">
                        <div class="fi__description__topline__title">{{self.item.get('title')}}</div>
                        <div class="fi__description__topline__date">{{ self.creationDateLabel }}</div>
                    </div>
                    <div class="fi__description__subtitle">
                    <!--
                        <span class="fi__description__subtitle__group" ng-show="self.item.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="fi__description__subtitle__group__icon">{{self.item.group}}<span class="fi__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        -->
                        <span class="fi__description__subtitle__type">{{self.labels.type[self.item.get('basicNeedType')]}}</span>
                    </div>
                </div>
            </div>
            <div class="fmil">
                <div class="fmil__item clickable" 
                ng-repeat="cnct in self.connections.toArray() track by $index" 
                ng-show="$index < self.maxNrOfItemsShown" 
                ui-sref="post({
                            postUri: self.item.get('uri'), 
                            connectionUri: cnct.getIn(['connection', 'uri']), 
                            connectionType:  cnct.getIn(['connection', 'hasConnectionState'])
                })">
                    <won-square-image
                        src="cnct.get('titleImg')"
                        title="cnct.getIn(['remoteNeed','title'])"
                        uri="cnct.getIn(['remoteNeed','uri'])">
                    </won-square-image>
                    <div class="fmil__item__description">
                        <div class="fmil__item__description__topline">
                            <div class="fmil__item__description__topline__title">
                                {{cnct.getIn(['remoteNeed','title'])}}
                            </div>
                            <div class="fmil__item__description__topline__date">
                                <!-- TODO only show this when this is a group's thread -->
                              Today, 15:03
                            </div>
                        </div>

                        <div class="fmil__item__description__message">Placeholder. This is a {{ cnct.getIn(['connection', 'hasConnectionState']) }}</div>
                    </div>
                </div>
                <div class="fmil__more clickable"
                     ng-show="self.connections.size === self.maxNrOfItemsShown + 1"
                     ng-click="self.showMore()">
                        1 more activity
                </div>
                <div class="fmil__more clickable"
                     ng-show="self.connections.size > self.maxNrOfItemsShown + 1"
                     ng-click="self.showMore()">
                        {{self.connections.size - self.maxNrOfItemsShown}} more activities
                </div>
            </div>

            <div class="fi__footer" ng-show="self.unreadMatchesCount() || self.unreadRequestsCount()">
                <div class="fi__footer__indicators">
                    <a class="fi__footer__indicators__item clickable"
                       ui-sref="postMatches({myUri: self.item.get('uri')})"
                       ng-show="self.unreadMatchesCount()">
                        <img src="generated/icon-sprite.svg#ico36_match" class="fi__footer__indicators__item__icon"/>
                        <span class="fi__footer__indicators__item__caption">{{ self.unreadMatchesCount() }} Matches</span>
                    </a>
                    <a class="fi__footer__indicators__item clickable"
                       ui-sref="postRequests({myUri: self.item.get('uri')})"
                       ng-show="self.unreadRequestsCount()">
                        <img src="generated/icon-sprite.svg#ico36_incoming" class="fi__footer__indicators__item__icon"/>
                        <span class="fi__footer__indicators__item__caption">{{self.unreadRequestsCount()}} Incoming Requests</span>
                    </a>
                </div>
            </div>
            `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.fi4dbg = this;

            this.labels = labels;
            /*
            * TODO there's tick events now. use state.get('lastUpdateTime')
            * to calculate relative timestamps
            */
            updateRelativeTimestamps(
                this.$scope,
                this.$interval,
                this.item.get('creationDate'),
                    t => this.creationDateLabel = t);

            this.maxNrOfItemsShown = 3;
        }
        showMore() {
            this.maxNrOfItemsShown += 6;
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
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            item: '=',
            /*
             ownPost { title, type, creationdate, pic }
             theirPosts/counterparts [
                { } //need
             ]
            */
            unreadCounts: '=',
            connections: '=',
        },
        template: template
    }
}

export default angular.module('won.owner.components.feedItem', [
    squareImageModule
])
    .directive('wonFeedItem', genComponentConf)
    .name;

