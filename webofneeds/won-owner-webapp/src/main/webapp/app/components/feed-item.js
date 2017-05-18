import angular from 'angular';
import Immutable from 'immutable';
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
    selectUnreadCountsByNeedAndType,
} from '../selectors';

import {
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils';

import feedItemLineModule from './feed-item-line';

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

            <won-feed-item-line
                class="fmil__item clickable"
                ng-repeat="cnct in self.connections track by $index"
                connection-uri="cnct.get('uri')"
                ng-show="$index < self.maxNrOfItemsShown"
                ui-sref="post({
                    postUri: self.ownNeed.get('@id'),
                    connectionUri: cnct.get('uri'),
                    connectionType: cnct.get('hasConnectionState')
                })">
            </won-feed-item-line>

            <div class="fmil__more clickable"
                 ng-show="self.nrOfConnections === self.maxNrOfItemsShown + 1"
                 ng-click="self.showMore()">
                    1 more activity
            </div>
            <div class="fmil__more clickable"
                 ng-show="self.nrOfConnections > self.maxNrOfItemsShown + 1"
                 ng-click="self.showMore()">
                    {{self.nrOfConnections - self.maxNrOfItemsShown}} more activities
            </div>
        </div>

        <div class="fi__footer" ng-show="self.unreadMatchesCount() || self.unreadRequestsCount()">
            <div class="fi__footer__indicators">
                <a class="fi__footer__indicators__item clickable"
                   ui-sref="postMatches({myUri: self.ownNeed.get('@id')})"
                   ng-show="self.unreadMatchesCount()">
                    <img src="generated/icon-sprite.svg#ico36_match" class="fi__footer__indicators__item__icon"/>
                    <span class="fi__footer__indicators__item__caption">
                       {{ self.unreadMatchesCount() }}
                       Match{{self.unreadMatchesCount() > 1 ? 'es' : ''}}
                    </span>
                </a>
                <a class="fi__footer__indicators__item clickable"
                   ui-sref="postRequests({myUri: self.ownNeed.get('@id')})"
                   ng-show="self.unreadRequestsCount()">
                    <img src="generated/icon-sprite.svg#ico36_incoming" class="fi__footer__indicators__item__icon"/>
                    <span class="fi__footer__indicators__item__caption">
                        {{self.unreadRequestsCount()}}
                        Incoming Request{{ self.unreadRequestsCount() > 1 ? 's' : ''}}
                    </span>
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

                const unreadCountsByNeedAndType = selectUnreadCountsByNeedAndType(state);
                const ownNeed = ownNeeds && ownNeeds.get(self.needUri);
                const ownNeedContent = ownNeed && seeksOrIs(ownNeed);

                const cnctUriCollection = ownNeed.getIn(['won:hasConnections', 'rdfs:member']);
                const connections = !cnctUriCollection?
                    [] : // if there's no cnctUriCollection, there's no connections
                    cnctUriCollection
                        .filter(c => c) // filter out `undefined`s
                        .map(c => state.getIn(['connections', c.get('@id')]))
                        .toArray();


                return {
                    ownNeed,
                    ownNeedContent,
                    connections,
                    nrOfConnections: connections? connections.size : 0,
                    createdOn: ownNeed && relativeTime(lastUpdated, ownNeed.get('dct:created')),
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
            return this.unreadXCount(won.WONMSG.hintMessage)
        }
        unreadRequestsCount() {
            return this.unreadXCount(won.WONMSG.connectMessage);
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
        },
        template: template
    }
}

export default angular.module('won.owner.components.feedItem', [
    squareImageModule,
    feedItemLineModule,
])
    .directive('wonFeedItem', genComponentConf)
    .name;

