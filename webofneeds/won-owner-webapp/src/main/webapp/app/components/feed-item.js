import angular from 'angular';
import squareImageModule from '../components/square-image';
import won from '../won-es6';
import { actionCreators }  from '../actions/actions';
import { attach } from '../utils';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import {
    selectOwnNeeds,
    selectLastUpdateTime,
    selectConnectionsByNeed,
    selectUnreadCountsByNeedAndType,
} from '../selectors';

import {
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils';

const serviceDependencies = ['$scope', '$interval', '$ngRedux'];
function genComponentConf() {
    let template = `
            <div class="fi clickable" ui-sref="post({postUri: self.ownNeed.get('@id')})">
                <won-square-image
                    src="self.ownNeed.get('titleImg')"
                    title="self.ownNeedContent.get('dc:title')"
                    uri="self.ownNeed.get('@id')">
                </won-square-image>
                <div class="fi__description">
                    <div class="fi__description__topline">
                        <div class="fi__description__topline__title">
                            {{ self.ownNeedContent.get('dc:title') }}
                        </div>
                        <div class="fi__description__topline__date">
                            {{ self.createdOn }}
                        </div>
                    </div>
                    <div class="fi__description__subtitle">
                    <!--
                        <span class="fi__description__subtitle__group" ng-show="self.ownNeed.group">
                            <img src="generated/icon-sprite.svg#ico36_group" class="fi__description__subtitle__group__icon">
                            {{self.ownNeed.group}}
                            <span class="fi__description__subtitle__group__dash"> &ndash; </span>
                        </span>
                        -->
                        <span class="fi__description__subtitle__type">
                            {{
                                self.labels.type[
                                    self.inferLegacyNeedType(self.ownNeed)
                                ]
                            }}
                        </span>
                    </div>
                </div>
            </div>
            <div class="fmil">
                <div class="fmil__item clickable"
                ng-repeat="cnct in self.connections.toArray() track by $index"
                ng-show="$index < self.maxNrOfItemsShown"
                ui-sref="post({
                    postUri: self.ownNeed.get('@id'),
                    connectionUri: cnct.getIn(['connection', 'uri']),
                    connectionType:  cnct.getIn(['connection', 'hasConnectionState'])
                })">
                    <won-square-image
                        src="cnct.get('titleImg')"
                        title="self.seeksOrIs(cnct.getIn('remoteNeed')).get('dc:title')"
                        uri="cnct.getIn(['remoteNeed','@id'])">
                    </won-square-image>
                    <div class="fmil__item__description">
                        <div class="fmil__item__description__topline">
                            <div class="fmil__item__description__topline__title">
                                {{self.seeksOrIs(cnct.getIn('remoteNeed')).get('dc:title')}}
                            </div>
                            <div class="fmil__item__description__topline__date">
                                <!-- TODO only show this when this is a group's thread -->
                              Today, 15:03
                            </div>
                        </div>

                        <div class="fmil__item__description__message">{{ self.getTextForConnectionState(cnct.getIn(['connection', 'hasConnectionState'])) }}</div>
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
                       ui-sref="postMatches({myUri: self.ownNeed.get('@id')})"
                       ng-show="self.unreadMatchesCount()">
                        <img src="generated/icon-sprite.svg#ico36_match" class="fi__footer__indicators__item__icon"/>
                        <span class="fi__footer__indicators__item__caption">{{ self.unreadMatchesCount() }} Matches</span>
                    </a>
                    <a class="fi__footer__indicators__item clickable"
                       ui-sref="postRequests({myUri: self.ownNeed.get('@id')})"
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
            this.seeksOrIs = seeksOrIs;
            this.inferLegacyNeedType = inferLegacyNeedType;

            window.fi4dbg = this;

            this.labels = labels;

            const self = this;

            this.maxNrOfItemsShown = 3;
            const selectFromState = (state) => {
                const ownNeeds = selectOwnNeeds(state);
                const lastUpdated = selectLastUpdateTime(state);
                const connectionsByNeed = selectConnectionsByNeed(state);
                const unreadCountsByNeedAndType = selectUnreadCountsByNeedAndType(state);
                const ownNeed = ownNeeds && ownNeeds.get(self.needUri);
                const ownNeedContent = ownNeed && seeksOrIs(ownNeed);

                return {
                    ownNeed,
                    ownNeedContent,
                    createdOn: ownNeed && relativeTime(lastUpdated, ownNeed.get('dct:created')),
                    connections: connectionsByNeed && connectionsByNeed.get(self.needUri),
                    unreadCounts: unreadCountsByNeedAndType && unreadCountsByNeedAndType.get(self.needUri),



                }
            }
            const disconnect = this.$ngRedux.connect(selectFromState,actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
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
        getTextForConnectionState(state){
            let stateText = this.labels.connectionState[state];
            if (!stateText) {
                stateText = "unknown connection state";
            }
            return stateText;
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
            /*
             ownPost { title, type, creationdate, pic }
             theirPosts/counterparts [
                { } //need
             ]
            */
            //unreadCounts: '=',
            //connections: '=',
        },
        template: template
    }
}

export default angular.module('won.owner.components.feedItem', [
    squareImageModule
])
    .directive('wonFeedItem', genComponentConf)
    .name;

