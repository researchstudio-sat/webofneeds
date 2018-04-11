import angular from 'angular';
import ngAnimate from 'angular-animate';

import Immutable from 'immutable';
import squareImageModule from './square-image.js';
import connectionIndicatorsModule from './connection-indicators.js';
import won from '../won-es6.js';
import { actionCreators }  from '../actions/actions.js';
import { attach } from '../utils.js';
import {
    labels,
    relativeTime,
} from '../won-label-utils.js';
import {
    selectLastUpdateTime,
    selectNeedByConnectionUri,
} from '../selectors.js';
import {
   connect2Redux,
} from '../won-utils.js';

import * as srefUtils from '../sref-utils.js';

import feedItemLineModule from './feed-item-line.js';

const serviceDependencies = ['$scope', '$interval', '$ngRedux', '$state'];
function genComponentConf() {
    let template = `

        <a
            class="fi clickable"
            ng-class="{'fi--withconn' : self.connectionsSize > 0}"
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
                        <svg style="--local-primary:var(--won-primary-color);"
                            class="fi__description__subtitle__group__icon">
                                <use href="#ico36_group"></use>
                        </svg>
                        {{self.ownNeed.group}}
                        <span class="fi__description__subtitle__group__dash"> &ndash; </span>
                    </span>
                    -->
                    <span class="fi__description__subtitle__type">
                        {{self.labels.type[self.ownNeed.get("type")]}}{{self.ownNeed.get('matchingContexts')? ' in '+ self.ownNeed.get('matchingContexts').join(', ') : ' (no matching context specified)' }}
                    </span>
                </div>
            </div>
        </a>
        <div class="fmil" ng-if="self.connectionsSize > 0">
            <won-feed-item-line
                class="fmil__item clickable"
                ng-repeat="conn in self.connectionsArray track by $index"
                connection-uri="conn.get('uri')"
                need-uri="self.ownNeed.get('uri')"
                ng-if="$index < self.maxNrOfItemsShown"
                ng-click="self.selectConnection(conn.get('uri'))">
            </won-feed-item-line>

            <div class="fmil__more clickable"
                 ng-if="self.connectionsSize === self.maxNrOfItemsShown + 1"
                 ng-click="self.showMore()">
                    1 more activity
            </div>
            <div class="fmil__more clickable"
                 ng-if="self.connectionsSize > self.maxNrOfItemsShown + 1"
                 ng-click="self.showMore()">
                    {{self.connectionsSize - self.maxNrOfItemsShown}} more activities
            </div>
        </div>

        <div class="fi__footer">
            <won-connection-indicators need-uri="self.needUri" on-selected-connection="self.selectConnection(connectionUri)"></won-connection-indicators>
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
                const connectionsWithoutClosed = connections && connections.filter(conn => conn.get("state") !== won.WON.Closed);

                return {
                    ownNeed,
                    connectionsArray: connectionsWithoutClosed && connectionsWithoutClosed.toArray(),
                    connectionsSize: connectionsWithoutClosed && connectionsWithoutClosed.size,
                    WON: won.WON,
                    createdOn: ownNeed && relativeTime(lastUpdated, ownNeed.get('creationDate')),
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.needUri'], this);
        }

        selectConnection(connectionUri) {
            this.markAsRead(connectionUri);
            this.router__stateGoAbs('post', {connectionUri: connectionUri, postUri: this.ownNeed.get('uri'), connectionType: won.WON.Connected});
        }

        markAsRead(connectionUri){
            const connections = this.ownNeed && this.ownNeed.get("connections");
            const connection = connections && connections.get(connectionUri);

            if(connection && connection.get("unread") && connection.get("state") !== won.WON.Connected) {
                const payload = {
                    connectionUri: connectionUri,
                    needUri: this.ownNeed.get("uri"),
                };

                this.connections__markAsRead(payload);
            }
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
    connectionIndicatorsModule,
    ngAnimate,
])
    .directive('wonFeedItem', genComponentConf)
    .name;

