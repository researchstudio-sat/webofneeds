;

import angular from 'angular';
import squareImageModule from './square-image';
import extendedGalleryModule from './extended-gallery';
import feedbackGridModule from './feedback-grid';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import { labels, relativeTime, updateRelativeTimestamps } from '../won-label-utils';
import { selectAllByConnections } from '../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope', '$interval'];
function genComponentConf() {
    let template = `
        <div ng-show="self.images" class="mfi__gallery">
            <won-extended-gallery max-thumbnails="self.maxThumbnails" items="self.images" class="horizontal"></won-extended-gallery>
        </div>
        <div class="mfi__description clickable">
            <div class="mfi__description__topline">
                <div class="mfi__description__topline__title clickable"><b>{{self.connectionData.getIn(['remoteNeed','title'])}}</b></div>
                <div class="mfi__description__topline__date">{{self.creationDate}}</div>
            </div>
            <div class="mfi__description__subtitle">
                <span class="mfi__description__subtitle__group" ng-show="self.connection.get('group')">
                    <img src="generated/icon-sprite.svg#ico36_group" class="mfi__description__subtitle__group__icon">{{self.connectionData.get('group')}}<span class="mfi__description__subtitle__group__dash"> &ndash; </span>
                </span>
                <span class="mfi__description__subtitle__type">{{self.labels.type[self.connectionData.getIn(['remoteNeed','basicNeedType'])]}}</span>
            </div>
            <div class="mfi__description__content">
                <div class="mfi__description__content__location">
                    <img class="mfi__description__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_location"/>
                    <span>Vienna area</span>
                </div>
                <div class="mfi__description__content__datetime">
                    <img class="mfi__description__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>o
                    <span>Available until 5th May</span>
                </div>
            </div>
        </div>
        <div class="mfi__match clickable" ng-if="!self.feedbackVisible" ng-click="self.showFeedback()" ng-mouseenter="self.showFeedback()" >
            <div class="mfi__match__description">
                <div class="mfi__match__description__title">{{self.connectionData.getIn(['ownNeed','title'])}}</div>
                <div class="mfi__match__description__type">{{self.labels.type[self.connectionData.getIn(['ownNeed','basicNeedType'])]}}</div>
            </div>
            <won-square-image src="self.getRandomImage()" title="self.connectionData.getIn(['ownNeed','title'])"></won-square-image>
        </div>
        <won-feedback-grid connection-uri="self.connectionUri" ng-mouseleave="self.hideFeedback()" ng-if="self.feedbackVisible"/>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            this.feedbackVisible = false;
            this.maxThumbnails = 4;
            this.images=[
                "images/furniture1.png",
                "images/furniture2.png",
                "images/furniture3.png",
                "images/furniture4.png",
            ];

            const selectFromState = (state) => {
                return {
                    connectionData: selectAllByConnections(state).get(this.connectionUri)
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            updateRelativeTimestamps(
                this.$scope,
                this.$interval,
                this.connectionData.getIn(["remoteNeed","creationDate"]),
                    t => this.creationDate = t);

        }

        showFeedback() {
            this.feedbackVisible = true;
        }

        hideFeedback() {
            this.feedbackVisible = false;
        }

        toggleFeedback(){
            this.feedbackVisible = !this.feedbackVisible;
        }

        getRandomImage(){
            let i = Math.floor((Math.random()*4));
            return this.images[2];
        }

    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            connectionUri: "="
        },
        template: template
    }
}

export default angular.module('won.owner.components.matchesFlowItem', [
    squareImageModule,
    extendedGalleryModule,
    feedbackGridModule
])
    .directive('wonMatchesFlowItem', genComponentConf)
    .name;

