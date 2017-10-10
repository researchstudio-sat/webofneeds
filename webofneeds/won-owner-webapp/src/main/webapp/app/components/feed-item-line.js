/**
 * Created by ksinger on 30.03.2017.
 */



import angular from 'angular';
import squareImageModule from '../components/square-image.js';
import { actionCreators }  from '../actions/actions.js';
import {
    attach,
} from '../utils.js';
import {
    labels,
    relativeTime,
} from '../won-label-utils.js';
import {
    selectLastUpdateTime,
    selectAllTheirNeeds,
} from '../selectors.js';

import {
    connect2Redux,
} from '../won-utils.js';

const serviceDependencies = ['$scope', '$interval', '$ngRedux'];
function genComponentConf() {
    let template = `
        <won-square-image
            src="self.remoteNeed && self.remoteNeed.get('titleImg')"
            title="self.remoteNeed && self.remoteNeed.get('title')"
            uri="self.remoteNeedUri">
        </won-square-image>
        <div class="fmil__item__description">
            <div class="fmil__item__description__topline">
                <div class="fmil__item__description__topline__title">
                    {{self.remoteNeed && self.remoteNeed.get('title')}}
                </div>
                <div class="fmil__item__description__topline__date">
                    <!-- TODO only show this when this is a group's thread -->
                  {{ self.lastUpdated }}
                </div>
            </div>

            <div class="fmil__item__description__message">
                {{ self.connection && self.getTextForConnectionState(
                     self.connection.get('state')
                   )
                }}
            </div>
        </div>
        `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.fil4dbg = this;

            this.labels = labels;

            const self = this;
            const selectFromState = (state) => {
                const lastUpdated = selectLastUpdateTime(state);
                const connection = state.getIn(["needs", this.needUri, "connections", this.connectionUri]);
                const remoteNeedUri = connection && connection.get('remoteNeedUri');
                const remoteNeed = remoteNeedUri && selectAllTheirNeeds(state).get(remoteNeedUri);

                return {
                    connection,
                    remoteNeedUri,
                    remoteNeed,
                    lastUpdated: connection && relativeTime(lastUpdated, connection.get('creationDate')),
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.needUri', 'self.connectionUri'], this);
        }

        getTextForConnectionState(state){
            let stateText = this.labels.connectionState[state];
            if (!stateText) {
                stateText = "unknown connection state";
            }
            return stateText;
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
            connectionUri: '=',
        },
        template: template
    }
}

export default angular.module('won.owner.components.feedItemLine', [
    squareImageModule,
])
    .directive('wonFeedItemLine', genComponentConf)
    .name;

