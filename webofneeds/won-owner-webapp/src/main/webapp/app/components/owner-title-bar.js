;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import { attach,mapToMatches } from '../utils';
import won from '../won-es6';
import { labels } from '../won-label-utils';
import { selectUnreadEventsByNeedAndType } from '../selectors';
import { actionCreators }  from '../actions/actions';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <nav class="need-tab-bar" ng-cloak ng-show="{{true}}">
            <div class="ntb__inner">
                <div class="ntb__inner__left">
                    <a ui-sref="overviewPosts">
                        <img src="generated/icon-sprite.svg#ico36_backarrow" class="ntb__icon">
                    </a>
                    <won-square-image src="self.item.titleImgSrc" title="self.item.title"></won-square-image>
                    <div class="ntb__inner__left__titles">
                        <h1 class="ntb__title">{{self.item.title}}</h1>
                        <div class="ntb__inner__left__titles__type">{{self.labels.type[self.item.basicNeedType]}}</div>
                    </div>
                </div>
                <div class="ntb__inner__right">
                    <img class="ntb__icon clickable" src="generated/icon-sprite.svg#ico_settings" ng-show="!self.settingsOpen" ng-click="self.settingsOpen = true" ng-mouseenter="self.settingsOpen = true">
                    <button class="won-button--filled thin red" ng-show="self.isActive && self.settingsOpen" ng-mouseleave="self.settingsOpen=false" ng-click="self.closePost()">Close Post</button>
                    <button class="won-button--filled thin red" ng-show="!self.isActive && self.settingsOpen" ng-mouseleave="self.settingsOpen=false" ng-click="self.reOpenPost()">Reopen Post</button>
                    <ul class="ntb__tabs">
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 4}">
                            <a ui-sref="postInfo({myUri: self.item.uri})">
                                Post Info
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 0}">
                            <a ui-sref="postConversations({myUri: self.item.uri})"
                                ng-class="{'disabled' : !self.hasMessages}">
                                Messages
                                <span class="ntb__tabs__unread">{{ self.unreadMessages }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 1}">
                            <a ui-sref="overviewMatches({myUri: self.item.uri})"
                                ng-class="{'disabled' : !self.hasMatches}">
                                Matches
                                <span class="ntb__tabs__unread">{{ self.unreadMatches }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 2}">
                            <a ui-sref="overviewIncomingRequests({myUri: self.item.uri})"
                                ng-class="{'disabled' : !self.hasIncomingRequests}">
                                Requests
                                <span class="ntb__tabs__unread">{{ self.unreadIncomingRequests }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 3}">
                            <a ui-sref="overviewSentRequests({myUri: self.item.uri})"
                                ng-class="{'disabled' : !self.hasSentRequests}">
                                Sent Requests
                                <span class="ntb__tabs__unread">{{ self.unreadSentRequests }}</span>
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    `;


    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            window.otb = this;
            this.labels = labels;
            this.settingsOpen = false;

            const selectFromState = (state)=>{
                const unreadCounts = selectUnreadEventsByNeedAndType(state);

                return {
                    hasIncomingRequests: Object.keys(state.getIn(['connections','connectionsDeprecated']).toJS())
                        .map(key=>state.getIn(['connections','connectionsDeprecated']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.RequestReceived && conn.ownNeed.uri === this.item.uri){
                                return true
                            }
                        }).length > 0,
                    hasSentRequests: Object.keys(state.getIn(['connections','connectionsDeprecated']).toJS())
                        .map(key=>state.getIn(['connections','connectionsDeprecated']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.RequestSent && conn.ownNeed.uri === this.item.uri){
                                return true
                            }
                        }).length > 0,
                    hasMatches: Object.keys(state.getIn(['connections','connectionsDeprecated']).toJS())
                        .map(key=>state.getIn(['connections','connectionsDeprecated']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.Suggested && conn.ownNeed.uri === this.item.uri){
                                return true
                            }
                        }).length > 0,
                    hasMessages: Object.keys(state.getIn(['connections','connectionsDeprecated']).toJS())
                        .map(key=>state.getIn(['connections','connectionsDeprecated']).toJS()[key])
                        .filter(conn=>{
                            if(conn.connection.hasConnectionState===won.WON.Connected && conn.ownNeed.uri === this.item.uri){
                                return true
                            }
                        }).length > 0,
                    unreadMessages: unreadCounts.getIn([this.item.uri, won.WON.Connected]), //TODO: NOT REALLY THE MESSAGE COUNT ONLY THE CONVERSATION COUNT
                    unreadIncomingRequests: unreadCounts.getIn([this.item.uri, won.WON.RequestReceived]),
                    unreadSentRequests: unreadCounts.getIn([this.item.uri, won.WON.RequestSent]),
                    unreadMatches: unreadCounts.getIn([this.item.uri, won.WON.Suggested]),
                    isActive: state.getIn(['needs','ownNeeds', this.item.uri, 'state']) === won.WON.Active
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        closePost() {
            console.log("CLOSING THE POST: "+this.item.uri);
            //this.needs__close(this.item);
        }

        reOpenPost() {
            console.log("RE-OPENING THE POST: "+this.item.uri);
            //this.needs__reopen(this.item);
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        template: template,
        scope: {selection: "=",
                item: "="}
    }
}

export default angular.module('won.owner.components.needTitleBar', [])
    .directive('wonOwnerTitleBar', genComponentConf)
    .name;
