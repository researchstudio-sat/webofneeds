;

import angular from 'angular';
import squareImageModule from './square-image';
import extendedGalleryModule from './extended-gallery';
import feedbackGridModule from './feedback-grid';
import postHeaderModule from './post-header';
import postContentModule from './post-content';

import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import {
    labels,
    relativeTime,
} from '../won-label-utils';

import {
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils';

import {
    selectAllByConnections,
    selectLastUpdateTime,
} from '../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope', '$interval'];
function genComponentConf() {
    let template = `
        <div ng-show="self.images" class="mfi__gallery">
            <won-extended-gallery
                max-thumbnails="self.maxThumbnails"
                items="self.images"
                class="horizontal"
                ng-show="self.images.length > 0">
            </won-extended-gallery>
            <won-square-image 
                title="self.remoteNeedContent.get('dc:title')"
                uri="self.remoteNeed.get('@id')"
                ng-show="self.images.length == 0">
            </won-square-image>
        </div>

        <div class="mfi__description clickable"
              ng-click="self.toggleFeedback()">

            <won-post-header
              need-uri="self.remoteNeed.get('@id')"
              hide-image="true">
            </won-post-header>
            <hr/>
            <won-post-content
              need-uri="self.remoteNeed.get('@id')">
            </won-post-content>
        </div>

        <div
            class="mfi__match clickable"
            ng-if="!self.feedbackVisible"
            ng-click="self.showFeedback()">
                <div class="mfi__match__description">
                    <div class="mfi__match__description__title">
                        {{ self.ownNeedContent.get('dc:title') }}
                    </div>
                    <div class="mfi__match__description__type">
                        {{ self.labels.type[ self.ownNeedType ] }}
                    </div>
                </div>
                <won-square-image
                    src="self.ownNeedContent.get('titleImgSrc')"
                    title="self.ownNeedContent.get('dc:title')"
                    uri="self.ownNeed.get('@id')">
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

            window.mfi4dbg = this;


            const selectFromState = (state) => {
                const connectionData = selectAllByConnections(state).get(this.connectionUri);
                const ownNeed = connectionData && connectionData.get('ownNeed');
                const remoteNeed = connectionData && connectionData.get('remoteNeed');

                return {
                    connectionData,

                    ownNeed,
                    ownNeedType: ownNeed && inferLegacyNeedType(ownNeed),
                    ownNeedContent: ownNeed && seeksOrIs(ownNeed),

                    remoteNeed,
                    remoteNeedType: remoteNeed && inferLegacyNeedType(remoteNeed),
                    remoteNeedContent: remoteNeed && seeksOrIs(remoteNeed),
                    remoteCreatedOn: remoteNeed && relativeTime(
                        selectLastUpdateTime(state),
                        remoteNeed.get('dct:created')
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

export default angular.module('won.owner.components.matchesFlowItem', [
        squareImageModule,
        extendedGalleryModule,
        feedbackGridModule,
        postHeaderModule,
        postContentModule,
    ])
    .directive('wonMatchesFlowItem', genComponentConf)
    .name;

