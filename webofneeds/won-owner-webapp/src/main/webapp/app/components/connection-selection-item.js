/**
 * Created by ksinger on 10.04.2017.
 */

import won from '../won-es6';
import angular from 'angular';
import squareImageModule from './square-image';
import {
    labels,
    relativeTime,
} from '../won-label-utils';
import { attach, decodeUriComponentProperly } from '../utils.js';
import { actionCreators }  from '../actions/actions';
import {
    selectOpenConnectionUri,
    selectAllByConnections,
    selectOpenPost,
    selectOpenPostUri,
    selectLastUpdatedPerConnection,
    selectLastUpdateTime,
} from '../selectors';

import {
    selectTimestamp,
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils'

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div
      class="conn__item"
      ng-class="self.isOpen() ? 'selected' : ''">
         <!--TODO request.titleImgSrc isn't defined -->
        <won-square-image
          src="request.titleImgSrc"
          class="clickable"
          title="self.theirNeedContent.get('dc:title')"
          uri="self.theirNeed.get('@id')"
          ng-click="self.setOpen()">
        </won-square-image>
        <div class="conn__item__description">
          <div class="conn__item__description__topline">
            <div
              class="conn__item__description__topline__title clickable"
              ng-click="self.setOpen()">
              {{ self.theirNeedContent.get('dc:title') }}
            </div>
            <div class="conn__item__description__topline__date">
              {{ self.lastUpdated }}
            </div>
            <img
              class="conn__item__description__topline__icon clickable"
              src="generated/icon-sprite.svg#ico_settings"
              ng-show="!self.settingsOpen && self.isOpen()"
              ng-click="self.settingsOpen = true">
            <div class="ntb__contextmenu contextmenu"
              ng-show="self.settingsOpen && self.isOpen()">
              <div class="content">
                <div class="topline">
                  <img
                    class="contextmenu__icon clickable"
                    src="generated/icon-sprite.svg#ico_settings"
                    ng-click="self.settingsOpen = false">
                </div>
                <button
                  class="won-button--filled thin red"
                  ng-click="self.closeConnection()">
                    Close Connection
                </button>
              </div>
            </div>
          </div>
          <div class="conn__item__description__subtitle">
            <!--
            <span class="conn__item__description__subtitle__group" ng-show="request.group">
              <img
                src="generated/icon-sprite.svg#ico36_group"
                class="mil__item__description__subtitle__group__icon">
              {{ self.allByConnections.getIn([connectionUri, 'group']) }}
              <span class="mil__item__description__subtitle__group__dash"> &ndash; </span>
            </span>
            -->
            <span class="conn__item__description__subtitle__type">
              {{ self.labels.type[ self.theirNeedType ] }}
            </span>
          </div>
          <!--
          <div class="conn__item__description__message">
            <span
              class="conn__item__description__message__indicator"
              ng-click="self.setOpen(connectionUri)"
              ng-show="!self.read(connectionUri))"/>
              <!-- TODO self.read isn't defined
            {{ self.allByConnections.getIn([connectionUri, 'lastEvent', 'msg']) }}
          </div>
          -->
        </div>
      </div>
    `;

    class Controller {
        constructor() {
            window.connSelItm4dbg = this;
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            //this.settingsOpen = false;

            const self = this;

            const selectFromState = (state)=> {

                const connectionData = selectAllByConnections(state).get(this.connectionUri);
                const ownNeed = connectionData && connectionData.get('ownNeed');
                const theirNeed = connectionData && connectionData.get('remoteNeed');

                const lastStateUpdate = selectLastUpdateTime(state);
                const lastUpdatedPerConnection = selectLastUpdatedPerConnection(state);
                const lastUpdatedRel = lastUpdatedPerConnection &&
                    relativeTime(
                        lastStateUpdate,
                        lastUpdatedPerConnection.get(this.connectionUri)
                    );

                return {
                    openConnectionUri: selectOpenConnectionUri(state),

                    ownNeed,
                    ownNeedType: ownNeed && inferLegacyNeedType(ownNeed),
                    ownNeedContent: ownNeed && seeksOrIs(ownNeed),

                    theirNeed,
                    theirNeedType: theirNeed && inferLegacyNeedType(theirNeed),
                    theirNeedContent: theirNeed && seeksOrIs(theirNeed),
                    theirNeedCreatedOn: theirNeed && relativeTime(
                        lastStateUpdate,
                        theirNeed.get('dct:created')
                    ),
                    lastUpdated: lastUpdatedRel,
                }
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }
        isOpen() {
            return this.openConnectionUri === this.connectionUri;
        }

        setOpen() {
            this.selectedConnection({connectionUri: this.connectionUri}); //trigger callback with scope-object
            //TODO either publish a dom-event as well; or directly call the route-change
        }

        closeConnection() {
            this.settingsOpen = false;
            this.connections__close(this.connectionUri);
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
             *  selected-connection="myCallback(connectionUri)"
             */
            selectedConnection: "&"
        },
        template: template
    }
}
export default angular.module('won.owner.components.connectionSelectionItem', [])
    .directive('wonConnectionSelectionItem', genComponentConf)
    .name;
