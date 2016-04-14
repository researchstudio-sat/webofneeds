;

import angular from 'angular';
import squareImageModule from '../components/square-image';
import { attach,mapToMatches } from '../utils';
import won from '../won-es6';
import { labels } from '../won-label-utils';
import { selectUnreadEventsByNeedAndType, selectAllByConnections } from '../selectors';
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
                            <a ui-sref="postInfo({myUri: self.myUri})">
                                Post Info
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 0}">
                            <a ui-sref="postConversations({myUri: self.myUri})"
                                ng-class="{'disabled' : !self.hasMessages}">
                                Messages
                                <span class="ntb__tabs__unread">{{ self.unreadMessages }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 1}">
                            <a ui-sref="overviewMatches({viewType: 0, myUri: self.myUri})"
                                ng-class="{'disabled' : !self.hasMatches}">
                                Matches
                                <span class="ntb__tabs__unread">{{ self.unreadMatches }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 2}">
                            <a ui-sref="overviewIncomingRequests({myUri: self.myUri})"
                                ng-class="{'disabled' : !self.hasIncomingRequests}">
                                Requests
                                <span class="ntb__tabs__unread">{{ self.unreadIncomingRequests }}</span>
                            </a>
                        </li>
                        <li ng-class="{'ntb__tabs__selected' : self.selection == 3}">
                            <a ui-sref="overviewSentRequests({myUri: self.myUri})"
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
                const connectionsDeprecated = selectAllByConnections(state).toJS(); //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.

                const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
                const post = state.getIn(['needs','ownNeeds', postId]);

                const hasIncomingRequests = state.getIn(['connections'])
                        .filter(conn =>
                        conn.get('hasConnectionState') === won.WON.RequestReceived
                        && conn.get('belongsToNeed') === postId
                    ).size > 0;

                const hasSentRequests = Object.keys(connectionsDeprecated) //TODO immutable maps have a `.filter(...)` https://facebook.github.io/immutable-js/docs/
                    .map(key => connectionsDeprecated[key])
                    .filter(conn=>{
                        if(conn.connection.hasConnectionState===won.WON.RequestSent && conn.ownNeed.uri === postId){
                            return true
                        }
                    }).length > 0;

                const hasMatches = Object.keys(connectionsDeprecated) //TODO immutable maps have a `.filter(...)` https://facebook.github.io/immutable-js/docs/
                    .map(key => connectionsDeprecated[key])
                    .filter(conn=>{
                        if(conn.connection.hasConnectionState===won.WON.Suggested && conn.ownNeed.uri === postId){
                            return true
                        }
                    }).length > 0;

                const hasMessages =  Object.keys(connectionsDeprecated) //TODO immutable maps have a `.filter(...)` https://facebook.github.io/immutable-js/docs/
                    .map(key => connectionsDeprecated[key])
                    .filter(conn=>{
                        return conn.connection.hasConnectionState===won.WON.Connected && conn.ownNeed.uri === postId
                    }).length > 0;

                return {
                    myUri: postId,
                    item: post? post.toJS() : {},
                    hasIncomingRequests,
                    hasSentRequests,
                    hasMatches,
                    hasMessages,
                    unreadMessages: unreadCounts.getIn([postId, won.WON.Connected]), //TODO: NOT REALLY THE MESSAGE COUNT ONLY THE CONVERSATION COUNT
                    unreadIncomingRequests: unreadCounts.getIn([postId, won.WON.RequestReceived]),
                    unreadSentRequests: unreadCounts.getIn([postId, won.WON.RequestSent]),
                    unreadMatches: unreadCounts.getIn([postId, won.WON.Suggested]),
                    isActive: state.getIn(['needs','ownNeeds', postId, 'state']) === won.WON.Active
                };
            };

            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        closePost() {
            console.log("CLOSING THE POST: "+this.item.uri);
            this.needs__close(this.item.uri);
        }

        reOpenPost() {
            console.log("RE-OPENING THE POST: "+this.item.uri);
            this.needs__reopen(this.item.uri);
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        template: template,
        scope: {
            selection: "=",
        }
    }
}

export default angular.module('won.owner.components.needTitleBar', [])
    .directive('wonOwnerTitleBar', genComponentConf)
    .name;
