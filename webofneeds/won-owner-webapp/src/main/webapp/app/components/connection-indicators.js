/**
 * Component for rendering the connection state as an svg
 * Created by fsuda on 10.04.2017.
 */
import angular from 'angular';
import won from '../won-es6.js';
import 'ng-redux';
import { labels, } from '../won-label-utils.js';
import { actionCreators }  from '../actions/actions.js';

import {
    attach,
} from '../utils.js'
import {
    connect2Redux,
} from '../won-utils.js'

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <a
            class="indicators__item clickable"
            ng-show="self.hasConversations"
            ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.Connected})">
                <svg class="indicators__item__icon"
                    style="--local-primary:#F09F9F;"
                    ng-show="!self.unreadConversationsCount">
                        <use href="#ico36_message"></use>
                </svg>

                <svg style="--local-primary:var(--won-primary-color);"
                     ng-show="self.unreadConversationsCount"
                     class="indicators__item__icon">
                        <use href="#ico36_message"></use>
                </svg>

                <span class="indicators__item__caption" title="Number of chats with unread messages">
                    {{ self.unreadConversationsCount }}
                </span>
        </a>
        <div class="indicators__item" ng-show="!self.hasConversations" title="No chats in this post">
            <svg class="indicators__item__icon"
                style="--local-primary:#CCD2D2;">
                    <use href="#ico36_message"></use>
            </svg>
             <span class="indicators__item__caption"></span>
        </div>
        <a
            class="indicators__item clickable"
            ng-show="self.hasRequests"
            ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.Connected})"> <!-- TODO: set the connectionType to connected since we pulled these views together -->

                <svg class="indicators__item__icon"
                    style="--local-primary:#F09F9F;"
                    ng-show="!self.unreadRequestsCount">
                        <use href="#ico36_incoming"></use>
                </svg>
                <svg style="--local-primary:var(--won-primary-color);"
                    ng-show="self.unreadRequestsCount"
                    class="indicators__item__icon">
                        <use href="#ico36_incoming"></use>
                </svg>
                <span class="indicators__item__caption" title="Number of new requests">
                    {{ self.unreadRequestsCount }}
                </span>
        </a>
        <div class="indicators__item" ng-show="!self.hasRequests" title="No requests to this post">
            <svg class="indicators__item__icon"
                style="--local-primary:#CCD2D2;">
                    <use href="#ico36_incoming"></use>
            </svg>
             <span class="indicators__item__caption"></span>
        </div>
        <a
            class="indicators__item clickable"
            ng-show="self.hasMatches"
            ng-click="self.router__stateGoAbs('post', {postUri: self.needUri, connectionType: self.WON.Connected})">

                <svg class="indicators__item__icon"
                    style="--local-primary:#F09F9F;"
                    ng-show="!self.unreadMatchesCount">
                        <use href="#ico36_match"></use>
                </svg>

                <svg style="--local-primary:var(--won-primary-color);"
                    ng-show="self.unreadMatchesCount"
                    class="indicators__item__icon">
                        <use href="#ico36_match"></use>
                </svg>
                <span class="indicators__item__caption" title="Number of new matches">
                    {{ self.unreadMatchesCount }}
                </span>
        </a>
        <div class="indicators__item" ng-show="!self.hasMatches" title="No matches for this post">
            <svg class="indicators__item__icon"
                style="--local-primary:#CCD2D2;">
                    <use href="#ico36_match"></use>
            </svg>
            <span class="indicators__item__caption"></span>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.labels = labels;

            const selectFromState = (state) => {
                const need = state.get(this.needUri);

                return {
                    WON: won.WON,
                }
            };

            connect2Redux(
                selectFromState, actionCreators,
                ['self.needUri'],
                this
            );
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
        },
        template: template
    }
}

export default angular.module('won.owner.components.connectionIndicator', [
])
    .directive('wonConnectionIndicator', genComponentConf)
    .name;
