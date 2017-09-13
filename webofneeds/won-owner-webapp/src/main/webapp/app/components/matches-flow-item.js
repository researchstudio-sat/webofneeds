;

import angular from 'angular';
import squareImageModule from './square-image.js';
import needMapModule from './need-map.js';
import extendedGalleryModule from './extended-gallery.js';
import feedbackGridModule from './feedback-grid.js';
import postHeaderModule from './post-header.js';
import postContentModule from './post-content.js';

import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    labels,
} from '../won-label-utils.js';
import {
    connect2Redux,
} from '../won-utils.js';

import {
    selectNeedByConnectionUri
} from '../selectors.js';

const serviceDependencies = ['$ngRedux', '$scope', '$interval'];
function genComponentConf() {
    let template = `
        <div ng-show="self.images" class="mfi__gallery">
            <won-extended-gallery
                max-thumbnails="self.maxThumbnails"
                items="self.images"
                class="horizontal"
                ng-show="!self.remoteNeed.get('location') && self.images.length > 0">
            </won-extended-gallery>
            <won-square-image 
                title="self.remoteNeed.get('title')"
                uri="self.remoteNeed.get('uri')"
                ng-show="!self.remoteNeed.get('location') && self.images.length == 0">
            </won-square-image>
            <won-need-map
                uri="self.remoteNeed.get('uri')"
                ng-if="self.remoteNeed.get('location') && self.images.length == 0">
            </won-need-map>
        </div>

        <div class="mfi__description clickable"
              ng-click="self.toggleFeedback()">
            <won-post-header
              need-uri="self.remoteNeed.get('uri')"
              hide-image="true">
            </won-post-header>
            <hr/>
            <won-post-content
              need-uri="self.remoteNeed.get('uri')">
            </won-post-content>
        </div>

        <div
            class="mfi__match clickable"
            ng-if="!self.feedbackVisible"
            ng-click="self.showFeedback()">
                <div class="mfi__match__description">
                    <div class="mfi__match__description__title">
                        {{ self.ownNeed.get('title') }}
                    </div>
                    <div class="mfi__match__description__type">
                        {{ self.labels.type[ self.ownNeed.get("type") ] }}
                    </div>
                </div>
                <won-square-image
                    src="self.ownNeed.get('titleImgSrc')"
                    title="self.ownNeed.get('title')"
                    uri="self.ownNeed.get('uri')">
                </won-square-image>
        </div>
        <won-feedback-grid
            connection-uri="self.connectionUri"
            ng-if="self.feedbackVisible"/>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            this.feedbackVisible = false;
            this.maxThumbnails = 4;
            this.images=[];
            const self = this;
            const selectFromState = (state) => {
                const ownNeed = selectNeedByConnectionUri(state, self.connectionUri);
                const connectionData = ownNeed && state.getIn(["needs", ownNeed.get("uri"), "connections", self.connectionUri]);
                const remoteNeed = connectionData && state.getIn(["needs", connectionData.get("remoteNeedUri")]);

                return {
                    ownNeed,
                    remoteNeed,
                };
            };

            connect2Redux(selectFromState, actionCreators, ['self.connectionUri'], this);
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
            connectionUri: "=",
        },
        template: template
    }
}

export default angular.module('won.owner.components.matchesFlowItem', [
        squareImageModule,
        extendedGalleryModule,
        feedbackGridModule,
        postHeaderModule,
        postContentModule,
        needMapModule
    ])
    .directive('wonMatchesFlowItem', genComponentConf)
    .name;

