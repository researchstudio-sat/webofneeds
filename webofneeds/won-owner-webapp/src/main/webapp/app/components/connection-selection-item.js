/**
 * Created by ksinger on 10.04.2017.
 */

import won from '../won-es6.js';
import angular from 'angular';
import {
    labels,
} from '../won-label-utils.js';
import { attach } from '../utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
    selectAllTheirNeeds
} from '../selectors.js';

import postHeaderModule from './post-header.js';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div
      class="conn__inner"
      ng-class="self.isOpen() ? 'selected' : ''">
        <won-post-header
          need-uri="self.theirNeed.get('uri')"
          timestamp="self.lastUpdateTimestamp"
          ng-click="self.setOpen()"
          class="clickable">
        </won-post-header>

        <div class="conn__unreadCount">
          {{ self.unreadCount }}
        </div>
      </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            //this.settingsOpen = false;

            const self = this;

            const selectFromState = (state)=> {
                const ownNeed = selectNeedByConnectionUri(state, this.connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", this.connectionUri]);
                const theirNeed = connection && selectAllTheirNeeds(state).get(connection.get("remoteNeedUri"));

                return {
                    WON: won.WON,
                    ownNeed,
                    connection,
                    openConnectionUri: selectOpenConnectionUri(state),
                    lastUpdateTimestamp: connection && connection.get('lastUpdateDate'),
                    theirNeed,
                    unreadCount: undefined //TODO: WHAT SHOULD BE HERE?
                }
            };

            connect2Redux(selectFromState, actionCreators, ['self.connectionUri'], this);
        }
        isOpen() {
            return this.openConnectionUri === this.connectionUri;
        }

        setOpen() {
            this.markAsRead();
            this.onSelectedConnection({connectionUri: this.connectionUri}); //trigger callback with scope-object
            //TODO either publish a dom-event as well; or directly call the route-change
        }

        markAsRead(){
            if(this.connection && this.connection.get("unread") && this.connection.get("state") !== won.WON.Connected){
                const payload = {
                    connectionUri: this.connection.get("uri"),
                    needUri: this.ownNeed.get("uri")
                };

                this.connections__markAsRead(payload);
            }
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
            /*
             * Usage:
             *  on-selected-connection="myCallback(connectionUri)"
             */
            onSelectedConnection: "&"
        },
        template: template
    }
}
export default angular.module('won.owner.components.connectionSelectionItem', [
        postHeaderModule,
    ])
    .directive('wonConnectionSelectionItem', genComponentConf)
    .name;
