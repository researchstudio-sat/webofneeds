/**
 * Created by ksinger on 22.03.2016.
 */
;

import won from '../won-es6';
import angular from 'angular';
import squareImageModule from './square-image';
import { labels } from '../won-label-utils';
import { attach } from '../utils.js';
import { actionCreators }  from '../actions/actions';
import { selectAllByConnections } from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div class="connectionSelectionItemLine"
                ng-repeat="(key,connectionUri) in self.connectionUris">
            <div class="conn">
                 <div
                 class="conn__item clickable"
                 ng-class="self.openUri === connectionUri? 'selected' : ''"
                 ng-click="self.setOpen(connectionUri)">
                     <!--TODO request.titleImgSrc isn't defined -->
                    <won-square-image
                        src="request.titleImgSrc"
                        title="self.allByConnections.getIn([connectionUri, 'remoteNeed', 'title'])">
                    </won-square-image>
                    <div class="conn__item__description">
                        <div class="conn__item__description__topline">
                            <div class="conn__item__description__topline__title">
                                {{ self.allByConnections.getIn([connectionUri, 'remoteNeed', 'title']) }}
                            </div>
                            <div class="conn__item__description__topline__date">
                                {{ self.allByConnections.getIn([connectionUri, 'connection', 'timestamp']) }}
                            </div>
                            <img
                                class="conn__item__description__topline__icon"
                                src="generated/icon-sprite.svg#ico_settings">
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
                                        self.allByConnections.getIn([connectionUri, 'remoteNeed', 'basicNeedType'])
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
            window.connSel4db = this;
            attach(this, serviceDependencies, arguments);
            this.labels = labels;

            const self = this;

            const selectFromState = (state)=>{
                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                const allByConnections = selectAllByConnections(state);
                const post = state.getIn(['needs','ownNeeds', postId]);
                const postJS = post? post.toJS() : {};

                /*
                * TODO this is a hack. For some reason passing directive parameters from post-owner-messages
                * doesn't work. Passing the type to filter for would be the preferred option.
                */
                const isMessagesView = state.getIn(['router', 'currentState', 'name']) === 'postConversations';
                if(isMessagesView) {
                    const connectionUris = allByConnections
                        .filter(conn =>
                            conn.getIn(['connection', 'hasConnectionState']) === self.connectionType &&
                            conn.getIn(['ownNeed', 'uri']) === postId
                        )
                        .map(conn => conn.getIn(['connection','uri']))
                        .toList().toJS();

                    return {
                        connectionUris,
                        allByConnections,
                        post: postJS,
                    };
                } else {
                    return {
                        connectionUris: [],
                        allByConnections,
                        post: postJS,
                    };
                }
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

        openMessage(item) {
            //this.events__read(item)
            this.openConversation = item;
            this.selectedConnectionUri = item.connection.uri;
            this.selectedConnection(item.connection.uri);
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            /*
             * TODO '@' is a hack here that requires using this with the full type url
             * for some reason I couldn't binding via '=' wasn't working though.
             */
            connectionType: "@",
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
