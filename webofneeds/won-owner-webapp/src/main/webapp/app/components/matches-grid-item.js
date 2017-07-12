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
import postHeaderModule from './post-header';
import postContentModule from './post-content';

const serviceDependencies = ['$ngRedux', '$scope', '$interval'];
function genComponentConf() {
    let template = `
        <div class="mgi__description clickable"
            ng-click="self.toggleFeedback()">

            <won-post-header
                need-uri="self.theirNeed.get('@id')">
            </won-post-header>
            <hr/>
            <won-post-content
                need-uri="self.theirNeed.get('@id')">
            </won-post-content>

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
                    title="self.ownNeedContent.get('dc:title')"
                    uri="self.ownNeed.getIn(['@id'])">
                </won-square-image>
        </div>
        <won-feedback-grid
            class="clickable"
            connection-uri="self.connectionUri"
            ng-click="self.hideFeedback()"
            ng-if="self.feedbackVisible"/>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
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
        feedbackGridModule,
        postHeaderModule,
        postContentModule,
    ])
    .directive('wonMatchesGridItem', genComponentConf)
    .name;

