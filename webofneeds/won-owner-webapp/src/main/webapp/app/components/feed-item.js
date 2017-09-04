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
    selectLastUpdateTime,
} from '../selectors';
import {
   connect2Redux,
} from '../won-utils';

import * as srefUtils from '../sref-utils';

import feedItemLineModule from './feed-item-line';

const serviceDependencies = ['$scope', '$interval', '$ngRedux', '$state'];
function genComponentConf() {
    let template = `

        <a
            class="fi clickable"
            href="{{ self.absHRef(
                self.$state,
                'post',
                { postUri: self.ownNeed.get('uri') }
            ) }}"
        >
            <won-square-image
                src="self.ownNeed.get('titleImg')"
                title="self.ownNeed.get('title')"
                uri="self.ownNeed.get('uri')">
            </won-square-image>
            <div class="fi__description">
                <div class="fi__description__topline">
                    <div class="fi__description__topline__title">
                        {{ self.ownNeed.get('title') }}
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
                        {{self.labels.type[self.ownNeed.get("type")]}}
                    </span>
                </div>
            </div>
        </a>
        <div class="fmil">

            <won-feed-item-line
                class="fmil__item clickable"
                ng-repeat="conn in self.connectionsArray track by $index"
                connection-uri="conn.get('uri')"
                need-uri="self.ownNeed.get('uri')"
                ng-show="$index < self.maxNrOfItemsShown"
                ng-click="self.router__stateGoAbs('post', {
                    postUri: self.ownNeed.get('uri'),
                    connectionUri: conn.get('uri'),
                    connectionType: conn.get('state')
                })">
            </won-feed-item-line>

            <div class="fmil__more clickable"
                 ng-show="self.connectionsSize === self.maxNrOfItemsShown + 1"
                 ng-click="self.showMore()">
                    1 more activity
            </div>
            <div class="fmil__more clickable"
                 ng-show="self.connectionsSize > self.maxNrOfItemsShown + 1"
                 ng-click="self.showMore()">
                    {{self.connectionsSize - self.maxNrOfItemsShown}} more activities
            </div>
        </div>

        <div class="fi__footer" ng-show="self.unreadMatchesCount || self.unreadRequestsCount">
            <div class="fi__footer__indicators">
                <a class="fi__footer__indicators__item clickable"
                    href="{{ self.absHRef(
                        self.$state,
                        'post',
                        { connectionType: self.WON.Suggested, postUri: self.needUri }
                    ) }}"
                   ng-show="self.unreadMatchesCount">
                    <img src="generated/icon-sprite.svg#ico36_match" class="fi__footer__indicators__item__icon"/>
                    <span class="fi__footer__indicators__item__caption">
                       {{ self.unreadMatchesCount }}
                       Match{{self.unreadMatchesCount > 1 ? 'es' : ''}}
                    </span>
                </a>
                <a class="fi__footer__indicators__item clickable"
                   ng-click="self.router__stateGoAbs('post', {connectionType: self.WON.RequestReceived, postUri: self.needUri})"
                   ng-show="self.unreadRequestsCount">
                    <img src="generated/icon-sprite.svg#ico36_incoming" class="fi__footer__indicators__item__icon"/>
                    <span class="fi__footer__indicators__item__caption">
                        {{self.unreadRequestsCount}}
                        Incoming Request{{ self.unreadRequestsCount > 1 ? 's' : ''}}
                    </span>
                </a>
            </div>
        </div>
    `;
    
    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            Object.assign(this, srefUtils); // bind srefUtils to scope
            this.labels = labels;
            window.lastfi4dbg = this;

            const self = this;
            this.maxNrOfItemsShown = 3;

            const selectFromState = (state) => {
                const lastUpdated = selectLastUpdateTime(state);

                const ownNeed = state.getIn(["needs", self.needUri]);
                const connections = ownNeed && ownNeed.get("connections");

                const unreadMatchesCount = connections && connections.filter(conn => conn.get("newConnection") && conn.get("state") === won.WON.Suggested).size;
                const unreadRequestsCount = connections && connections.filter(conn => conn.get("newConnection") && conn.get("state") === won.WON.RequestReceived).size;

                return {
                    ownNeed,
                    connectionsArray: connections && connections.toArray(),
                    connectionsSize: connections && connections.size,
                    WON: won.WON,
                    createdOn: ownNeed && relativeTime(lastUpdated, ownNeed.get('creationDate')),
                    unreadMatchesCount,
                    unreadRequestsCount,
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.needUri'], this);
        }
        showMore() {
            this.maxNrOfItemsShown += 6;
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

