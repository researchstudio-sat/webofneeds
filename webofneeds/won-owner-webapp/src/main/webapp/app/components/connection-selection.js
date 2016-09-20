/**
 * Created by ksinger on 22.03.2016.
 */
;

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
} from '../won-utils'

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div class="omc__empty" ng-if="self.connectionType === self.won.Connected && !self.hasConnections">
            <div class="omc__empty__description">
                <img src="generated/icon-sprite.svg#ico36_message_grey" class="omc__empty__description__icon">
                <span class="omc__empty__description__text">You will be able to communicate with others once there are accepted connections. Accept a request or send requests and wait until the counterpart accepts it.</span>
            </div>
            <a ui-sref="{connectionType: self.won.RequestReceived}" class="omc__empty__link">
                <img src="generated/icon-sprite.svg#ico36_incoming" class="omc__empty__link__icon">
                <span class="omc__empty__link__caption">Accept requests</span>
            </a>
        </div>
        <div class="omc__empty" ng-if="self.connectionType === self.won.RequestReceived && !self.hasConnections">
            <div class="omc__empty__description">
                <img src="generated/icon-sprite.svg#ico36_incoming_grey" class="omc__empty__description__icon">
                <span class="omc__empty__description__text">This view shows you all the incoming request for this specific need. Wait until someone tries to connect with you.</span>
            </div>
            <a ui-sref="{connectionType: self.won.Connected}" class="omc__empty__link">
                <img src="generated/icon-sprite.svg#ico36_message" class="omc__empty__link__icon">
                <span class="omc__empty__link__caption">Go to conversations</span>
            </a>
            <a ui-sref="{connectionType: self.won.Suggested}" class="omc__empty__link">
                <img src="generated/icon-sprite.svg#ico36_matches" class="omc__empty__link__icon">
                <span class="omc__empty__link__caption">Go to matches</span>
            </a>
        </div>
        <div class="omc__empty" ng-if="self.connectionType === self.won.RequestSent && !self.hasConnections">
            <div class="omc__empty__description">
                <img src="generated/icon-sprite.svg#ico36_outgoing_grey" class="omc__empty__description__icon">
                <span class="omc__empty__description__text">This view shows you all your sent requests for this specific need. Connect with a match to see it here.</span>
            </div>
            <a ui-sref="{connectionType: self.won.Connected}" class="omc__empty__link">
                <img src="generated/icon-sprite.svg#ico36_message" class="omc__empty__link__icon">
                <span class="omc__empty__link__caption">Go to conversations</span>
            </a>
            <a ui-sref="{connectionType: self.won.Suggested}" class="omc__empty__link">
                <img src="generated/icon-sprite.svg#ico36_matches" class="omc__empty__link__icon">
                <span class="omc__empty__link__caption">Go to matches</span>
            </a>
        </div>
        <div class="connectionSelectionItemLine"
                ng-repeat="(key,connectionUri) in self.connectionUris">
            <div class="conn">
                 <div
                 class="conn__item"
                 ng-class="self.openConversationUri === connectionUri? 'selected' : ''">
                     <!--TODO request.titleImgSrc isn't defined -->
                    <won-square-image
                        src="request.titleImgSrc"
                        class="clickable"
                        title="self.allByConnections.getIn([
                            connectionUri, 'remoteNeed',
                            'won:hasContent', 'dc:title'])"
                        uri="self.allByConnections.getIn([connectionUri, 'remoteNeed', '@id'])"
                        ng-click="self.setOpen(connectionUri)">
                    </won-square-image>
                    <div class="conn__item__description">
                        <div class="conn__item__description__topline">
                            <div class="conn__item__description__topline__title clickable"
                                        ng-click="self.setOpen(connectionUri)">
                                {{
                                  self.allByConnections.getIn([
                                    connectionUri, 'remoteNeed',
                                    'won:hasContent', 'dc:title'
                                  ])
                                }}
                            </div>
                            <div class="conn__item__description__topline__date">
                                {{ self.lastUpdated.get(connectionUri) }}
                            </div>
                            <img
                                class="conn__item__description__topline__icon"
                                src="generated/icon-sprite.svg#ico_settings"
                                ng-show="!self.settingsOpen && self.openConversationUri === connectionUri"
                                ng-mouseenter="self.settingsOpen = true">
                            <div class="conn__item__description__settings" ng-show="self.settingsOpen && self.openConversationUri === connectionUri" ng-mouseleave="self.settingsOpen=false">
                                <button class="won-button--filled thin red" ng-click="self.closeConnection(connectionUri)">Close Connection</button>
                            </div>
                        </div>
                        <div class="conn__item__description__subtitle">
                            <span class="conn__item__description__subtitle__group" ng-show="request.group">
                                <img
                                    src="generated/icon-sprite.svg#ico36_group"
                                    class="mil__item__description__subtitle__group__icon">
                                {{ self.allByConnections.getIn([connectionUri, 'group']) }}
                                <span class="mil__item__description__subtitle__group__dash"> &ndash; </span>
                            </span>
                            <span class="conn__item__description__subtitle__type">
                                {{
                                   self.labels.type[
                                        self.allByConnections.getIn([connectionUri, 'remoteNeed', 'won:hasBasicNeedType', '@id'])
                                   ]
                                }}
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
            </div>
        </div>
    `;

    class Controller {
        constructor() {
            window.connSel4dbg = this;
            attach(this, serviceDependencies, arguments);
            this.labels = labels;
            this.settingsOpen = false;

            const self = this;

            const selectFromState = (state)=>{
                const postUri = selectOpenPostUri(state);
                const openConnectionUri = selectOpenConnectionUri(state);
                const allByConnections = selectAllByConnections(state);
                const post = selectOpenPost(state);

                const connectionTypeInParams = decodeUriComponentProperly(
                        state.getIn(['router', 'currentParams', 'connectionType'])
                    ) ||
                    won.WON.Connected; // TODO old parameter
                const connectionType = connectionTypeInParams || self.connectionType;

                const connectionUris = allByConnections
                    .filter(conn =>
                        conn.getIn(['connection', 'hasConnectionState']) === connectionType &&
                        conn.getIn(['ownNeed', '@id']) === postUri
                    )
                    .map(conn => conn.getIn(['connection','uri']))
                    .toList().toJS();

                const lastStateUpdate = selectLastUpdateTime(state);

                if(connectionUris.size > 0){ //this is used to add a class to the toplevel element of the connection-selection, otherwise we would have to inject the class by asking the state in the toplevel
                    angular.element("won-connection-selection").removeClass("empty");
                }else{
                    angular.element("won-connection-selection").addClass("empty");
                }

                return {
                    lastUpdated:
                        selectLastUpdatedPerConnection(state)
                        .map(ts => relativeTime(lastStateUpdate, ts)),
                    connectionUris,
                    hasConnections: connectionUris.size > 0,
                    allByConnections,
                    openConversationUri: openConnectionUri,
                    won: won.WON,
                    post: post? post.toJS() : {},
                };
            }

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }
        setOpen(connectionUri) {
            this.openUri = connectionUri;
            this.selectedConnection({connectionUri}); //trigger callback with scope-object
        }
        getOpen() {
            return this.allByConnections.get(this.openUri);
        }

        closeConnection(connectionUri) {
            this.settingsOpen = false;
            this.connections__close(connectionUri);
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            connectionType: "=",
            /*
             * Usage:
             *  selected-connection="myCallback(connectionUri)"
             */
            selectedConnection: "&"
        },
        template: template
    }

}



export default angular.module('won.owner.components.connectionSelection', [])
    .directive('wonConnectionSelection', genComponentConf)
    .name;
