/**
 * Created by ksinger on 30.03.2017.
 */



import angular from 'angular';
import Immutable from 'immutable';
import squareImageModule from '../components/square-image';
import won from '../won-es6';
import { actionCreators }  from '../actions/actions';
import {
    attach,
    msStringToDate,
} from '../utils';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import {
    selectLastUpdateTime,
    //selectAllByConnections,
    selectUnreadCountsByNeedAndType,
    selectConnections,
    selectTheirNeeds,
    //selectLatestUpdateByConnection,
} from '../selectors';

import {
    seeksOrIs,
    inferLegacyNeedType,
    selectTimestamp,
    connectionLastUpdatedAt,
    connect2Redux,
} from '../won-utils';

const serviceDependencies = ['$scope', '$interval', '$ngRedux'];
function genComponentConf() {
    let template = `
        <won-square-image
            src="cnct.get('titleImg')"
            title="self.remoteNeedContent && self.remoteNeedContent.get('dc:title')"
            uri="self.remoteNeedUri">
        </won-square-image>
        <div class="fmil__item__description">
            <div class="fmil__item__description__topline">
                <div class="fmil__item__description__topline__title">
                    {{self.remoteNeedContent && self.remoteNeedContent.get('dc:title')}}
                </div>
                <div class="fmil__item__description__topline__date">
                    <!-- TODO only show this when this is a group's thread -->
                  {{ self.lastUpdated }}
                </div>
            </div>

            <div class="fmil__item__description__message">
                {{ self.connection && self.getTextForConnectionState(
                     self.connection.get('hasConnectionState')
                   )
                }}
            </div>
        </div>
        `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.seeksOrIs = seeksOrIs;
            this.inferLegacyNeedType = inferLegacyNeedType;

            window.fil4dbg = this;

            this.labels = labels;

            const self = this;
            const selectFromState = (state) => {
                const connection = self.connectionUri && selectConnections(state).get(self.connectionUri);
                const remoteNeedUri = connection && connection.get('hasRemoteNeed');
                const remoteNeed = remoteNeedUri && selectTheirNeeds(state).get(remoteNeedUri);
                const remoteNeedContent = remoteNeed && seeksOrIs(remoteNeed);

                //Problem: lastUpdated atm is only calculable, after the connection is viewed and the events loaded
                const lastUpdated = relativeTime(
                    state.get('lastUpdateTime'),
                    connectionLastUpdatedAt(state, connection)
                );

                // const unreadCounts = TODO

                return {
                    connection,
                    remoteNeedUri,
                    remoteNeed,
                    remoteNeedContent,
                    lastUpdated,
                }
            }
            connect2Redux(selectFromState, actionCreators, ['self.connectionUri'], this);
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

