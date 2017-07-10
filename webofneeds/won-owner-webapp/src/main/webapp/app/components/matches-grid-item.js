;

import angular from 'angular';
import squareImageModule from './square-image';
import feedbackGridModule from './feedback-grid';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import { labels, } from '../won-label-utils';
import {
    selectNeedByConnectionUri,
} from '../selectors';
import postHeaderModule from './post-header';
import postContentModule from './post-content';

const serviceDependencies = ['$q', '$ngRedux', '$scope', '$interval'];
function genComponentConf() {
    let template = `
        <div class="mgi__description clickable"
            ng-click="self.toggleFeedback()">

            <won-post-header
                need-uri="self.theirNeed.get('uri')">
            </won-post-header>
            <hr/>
            <won-post-content
                need-uri="self.theirNeed.get('uri')">
            </won-post-content>

        </div>
        <div
            class="mgi__match clickable"
            ng-if="!self.feedbackVisible"
            ng-click="self.showFeedback()">
                <div class="mgi__match__description">
                    <div class="mgi__match__description__title">
                        {{ self.ownNeed.get('title') }}
                    </div>
                    <div class="mgi__match__description__type">
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
            const self = this;
            const selectFromState = (state) => {
                const ownNeed = selectNeedByConnectionUri(state, self.connectionUri);
                const connectionData = state.getIn(["needs", "allNeeds", ownNeed.get("uri"), "connections", self.connectionUri]);
                const theirNeed = state.getIn(["needs", "allNeeds", connectionData.get("remoteNeedUri")]);

                return {
                    ownNeed,
                    theirNeed,
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
            connectionUri: "=",
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

