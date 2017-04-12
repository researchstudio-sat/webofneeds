;

import angular from 'angular';
import squareImageModule from './square-image';
import feedbackGridModule from './feedback-grid';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import { labels, relativeTime, updateRelativeTimestamps } from '../won-label-utils';
import {
    selectAllByConnections,
    selectLastUpdateTime,
} from '../selectors';
import {
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils';

const serviceDependencies = ['$q', '$ngRedux', '$scope', '$interval'];
function genComponentConf() {
    let template = `
        <div class="mgi__description">
            <div class="mgi__description__post clickable">
                <div class="mgi__description__post__text">
                    <div class="mgi__description__post__text__topline">
                        <div class="mgi__description__post__text__topline__title">
                            {{ self.theirNeedContent.get('dc:title') }}
                        </div>
                        <div class="mgi__description__post__text__topline__date">
                            {{ self.theirNeedCreatedOn }}
                        </div>
                    </div>
                    <div class="mgi__description__post__text__subtitle">
                        <span
                            class="mgi__description__post__text__subtitle__group"
                            ng-show="self.connectionData.get('group')">
                                <img src="generated/icon-sprite.svg#ico36_group"
                                    class="mgi__description__post__text__subtitle__group__icon">
                                {{ self.connectionData.get('group') }}
                                <span class="mgi__description__post__text__subtitle__group__dash"> &ndash; </span>
                        </span>
                        <span class="mgi__description__post__text__subtitle__type">
                            {{ self.labels.type[ self.theirNeedType ] }}
                        </span>
                    </div>
                </div>
            </div>
            <div
                class="mgi__description__content"
                ng-show="self.theirNeedContent.getIn(['won:hasLocation', 's:name']) || self.theirNeed.get('deadline')">
                    <div class="mgi__description__content__location"
                        ng-show="self.theirNeedContent.getIn(['won:hasLocation', 's:name'])">
                            <img
                                class="mgi__description__content__indicator"
                                src="generated/icon-sprite.svg#ico16_indicator_location"/>
                            <span>{{ self.theirNeedContent.getIn(['won:hasLocation', 's:name']) }}</span>
                    </div>
                    <!--
                    <div class="mgi__description__content__datetime"
                        ng-show="self.theirNeed.get('deadline')">
                            <img
                                class="mgi__description__content__indicator"
                                src="generated/icon-sprite.svg#ico16_indicator_time"/>
                            <span>{{ self.theirNeed.get('deadline') }}</span>
                    </div>
                    -->
            </div>
        </div>
        <div
            class="mgi__match clickable"
            ng-if="!self.feedbackVisible"
            ng-click="self.showFeedback()">
                <div class="mgi__match__description">
                    <div class="mgi__match__description__title">
                        {{ self.ownNeedContent.get('dc:title') }}
                    </div>
                    <div class="mgi__match__description__type">
                        {{ self.labels.type[ self.ownNeedType ] }}
                    </div>
                </div>
                <won-square-image
                    src="self.ownNeedContent.get('titleImgSrc')"
                    title="self.ownNeedContent('dc:title')"
                    uri="self.ownNeed.getIn(['@id'])">
                </won-square-image>
        </div>
        <won-feedback-grid
            connection-uri="self.connectionUri"
            ng-click="self.hideFeedback()"
            ng-if="self.feedbackVisible"/>
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
            window.mgi4dbg = this;

            const selectFromState = (state) => {
                const connectionData = selectAllByConnections(state).get(this.connectionUri);
                const ownNeed = connectionData && connectionData.get('ownNeed');
                const theirNeed = connectionData && connectionData.get('remoteNeed');

                return {
                    connectionData,

                    ownNeed,
                    ownNeedType: ownNeed && inferLegacyNeedType(ownNeed),
                    ownNeedContent: ownNeed && seeksOrIs(ownNeed),

                    theirNeed,
                    theirNeedType: theirNeed && inferLegacyNeedType(theirNeed),
                    theirNeedContent: theirNeed && seeksOrIs(theirNeed),
                    theirNeedCreatedOn: theirNeed && relativeTime(
                        selectLastUpdateTime(state),
                        theirNeed.get('dct:created')
                    ),
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
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

