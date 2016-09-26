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
                <div class="mgi__description__post__text">
                    <div class="mgi__description__post__text__topline">
                        <div class="mgi__description__post__text__topline__title">
                            {{ self.connectionData.getIn(['remoteNeed','won:hasContent','dc:title']) }}
                        </div>
                        <div class="mgi__description__post__text__topline__date">
                            {{ self.creationDate }}
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
                            {{
                                self.labels.type[
                                    self.connectionData.getIn(['remoteNeed','won:hasBasicNeedType','@id'])
                                ]
                            }}
                        </span>
                    </div>
                </div>
            </div>
            <div
                class="mgi__description__content"
                ng-show="self.theirNeed.get('location') || self.theirNeed.get('deadline')">
                    <div class="mgi__description__content__location"
                        ng-show="self.theirNeed.get('location')">
                            <img
                                class="mgi__description__content__indicator"
                                src="generated/icon-sprite.svg#ico16_indicator_location"/>
                            <span>{{ self.theirNeed.get('location') }}</span>
                    </div>
                    <div class="mgi__description__content__datetime"
                        ng-show="self.theirNeed.get('deadline')">
                            <img
                                class="mgi__description__content__indicator"
                                src="generated/icon-sprite.svg#ico16_indicator_time"/>
                            <span>{{ self.theirNeed.get('deadline') }}</span>
                    </div>
            </div>
        </div>
        <div
            class="mgi__match clickable"
            ng-if="!self.feedbackVisible"
            ng-click="self.showFeedback()">
                <div class="mgi__match__description">
                    <div class="mgi__match__description__title">
                        {{ self.ownNeed.getIn(['won:hasContent','dc:title']) }}
                    </div>
                    <div class="mgi__match__description__type">
                        {{
                            self.labels.type[
                                self.ownNeed.getIn(['won:hasBasicNeedType', '@id'])
                            ]
                        }}
                    </div>
                </div>
                <won-square-image
                    src="self.ownNeed.getIn(['won:hasContent','titleImgSrc'])"
                    title="self.ownNeed.getIn(['won:hasContent','dc:title'])"
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

            const selectFromState = (state) => {
                const connectionData = selectAllByConnections(state).get(this.connectionUri);
                return {
                    connectionData,
                    ownNeed: connectionData.get('ownNeed'),
                    theirNeed: connectionData.get('remoteNeed')
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            updateRelativeTimestamps(
                this.$scope,
                this.$interval,
                this.connectionData.getIn(["remoteNeed","dct:created"]),
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

