;

import angular from 'angular';
import squareImageModule from './square-image';
import feedbackGridModule from './feedback-grid';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import { labels, relativeTime, updateRelativeTimestamps } from '../won-label-utils';
import { selectAllByConnections } from '../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope', '$interval'];
function genComponentConf() {
    let template = `
        <div class="mgi__description">
            <div class="mgi__description__post clickable">
                <!--won-square-image src="self.getRandomImage()" title="self.connectionData.get(.ownNeed.title"></won-square-image-->
                <div class="mgi__description__post__text">
                    <div class="mgi__description__post__text__topline">
                        <div class="mgi__description__post__text__topline__title">{{self.connectionData.getIn(['remoteNeed','title'])}}</div>
                        <div class="mgi__description__post__text__topline__date">{{self.creationDate}}</div>
                    </div>
                    <div class="mgi__description__post__text__subtitle">
                        <span class="mgi__description__post__text__subtitle__group" ng-show="self.connectionData.get('group')">
                            <img src="generated/icon-sprite.svg#ico36_group" class="mgi__description__post__text__subtitle__group__icon">{{self.connectionData.get('group')}}<span class="mgi__description__post__text__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="mgi__description__post__text__subtitle__type">{{self.labels.type[self.connectionData.getIn(['remoteNeed','basicNeedType'])]}}</span>
                    </div>
                </div>
            </div>
            <div class="mgi__description__content">
                <div class="mgi__description__content__location">
                    <img class="mgi__description__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_location"/>
                    <span>Vienna area</span>
                </div>
                <div class="mgi__description__content__datetime">
                    <img class="mgi__description__content__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>
                    <span>Available until 5th May</span>
                </div>
            </div>
        </div>
        <div class="mgi__match clickable" ng-if="!self.feedbackVisible" ng-click="self.showFeedback()" ng-mouseenter="self.showFeedback()">
            <div class="mgi__match__description">
                <div class="mgi__match__description__title">{{self.connectionData.getIn(['ownNeed','basicNeedType'])}}</div>
                <div class="mgi__match__description__type">{{self.labels.type[self.connectionData.getIn(['ownNeed','basicNeedType'])]}}</div>
            </div>
            <won-square-image 
                src="self.connectionData.getIn(['ownNeed','titleImgSrc'])" 
                title="self.connectionData.getIn(['ownNeed','title'])"
                uri="self.connectionData.getIn(['ownNeed','uri'])">
            </won-square-image>
        </div>
        <won-feedback-grid connection-uri="self.connectionUri" ng-mouseleave="self.hideFeedback()" ng-if="self.feedbackVisible"/>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.images=[
                "images/furniture1.png",
                "images/furniture2.png",
                "images/furniture3.png",
                "images/furniture4.png"
            ];
            this.feedbackVisible = false;
            this.labels = labels;

            const selectFromState = (state) => {
                return {
                    connectionData: selectAllByConnections(state).get(this.connectionUri),
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

export default angular.module('won.owner.components.matchesGridItem', [
    squareImageModule,
    feedbackGridModule
])
    .directive('wonMatchesGridItem', genComponentConf)
    .name;

