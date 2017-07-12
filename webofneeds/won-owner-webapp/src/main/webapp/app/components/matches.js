;

import Immutable from 'immutable';
import won from '../won-es6';
import angular from 'angular';
import overviewTitleBarModule from './overview-title-bar';
import matchesFlowItemModule from './matches-flow-item';
import matchesGridItemModule from './matches-grid-item';
import sendRequestModule from './send-request';
import connectionsOverviewModule from './connections-overview';
import connectionSelectionModule from './connection-selection';

import { attach, decodeUriComponentProperly} from '../utils';
import { labels } from '../won-label-utils';
import { actionCreators }  from '../actions/actions';
import {
    selectOpenPostUri,
    displayingOverview,
    selectAllConnections,
} from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];
let template = `
    <a class="curtain" ng-if="self.connection"></a>
    <div class="omc__inner" ng-class="{'empty' : !self.hasMatches}">
        <div class="omc__empty" ng-if="!self.hasMatches">
            <div class="omc__empty__description">
                <img src="generated/icon-sprite.svg#ico36_match_grey" class="omc__empty__description__icon">
                <span class="omc__empty__description__text">The matches to all your needs will be listed here. 
                 You cannot influence the matching process. It might take some time, or maybe there is nothing to
                    be found for you, yet. Check back later or post more needs!</span>
            </div>
            <a ui-sref="createNeed" class="omc__empty__link">
                <img src="generated/icon-sprite.svg#ico36_plus" class="omc__empty__link__icon">
                <span class="omc__empty__link__caption">Create a Need</span>
            </a>
        </div>
        <div class="omc__header" ng-if="self.hasMatches">
            <div class="title">Matches to your post{{ self.isOverview? 's' : '' }}</div>
            <div class="omc__header__viewtype">
                <a ui-sref="{layout: self.LAYOUT.TILES}">
                    <img ng-src="{{self.layout === 'tiles' ? 'generated/icon-sprite.svg#ico-filter_tile_selected' : 'generated/icon-sprite.svg#ico-filter_tile'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
                <a ui-sref="{layout: self.LAYOUT.GRID}">
                    <img ng-src="{{self.layout === 'grid' ? 'generated/icon-sprite.svg#ico-filter_compact_selected' : 'generated/icon-sprite.svg#ico-filter_compact'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
                <a ui-sref="{layout: self.LAYOUT.LIST}">
                    <img ng-src="{{self.layout === 'list' ? 'generated/icon-sprite.svg#ico-filter_list_selected' : 'generated/icon-sprite.svg#ico-filter_list'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
            </div>
        </div>
        <div ng-if="self.hasMatches && self.layout === 'tiles'" class="omc__content__flow">
            <won-matches-flow-item
                    connection-uri="match.get('uri')"
                    ng-repeat="match in self.matchesArray">
            </won-matches-flow-item>
        </div>
        <div ng-if="self.hasMatches && self.layout === 'grid'" class="omc__content__grid">
            <won-matches-grid-item
                    connection-uri="match.get('uri')"
                    ng-repeat="match in self.matchesArray">
            </won-matches-grid-item>
        </div>
        <div ng-if="self.hasMatches && self.layout === 'list'" class="omc__content__list">
            <won-connections-overview
                ng-show="self.isOverview"
                connection-type="::self.WON.Suggested"
                on-selected-connection="self.selectedConnection(connectionUri)">
            </won-connections-overview>

            <won-connection-selection
                ng-show="!self.isOverview"
                connection-type="::self.WON.Suggested"
                on-selected-connection="self.selectedConnection(connectionUri)">
            </won-connection-selection>
        </div>
    </div>
    <div class="omc__sendrequest" ng-if="self.hasMatches && self.connection">
        <won-send-request></won-send-request>
    </div>
`

const LAYOUT = Object.freeze({ TILES: 'tiles', GRID: 'grid', LIST: 'list'});

class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.omc4dbg = this;

        this.WON = won.WON;
        this.LAYOUT = LAYOUT;
        this.labels = labels;

        const selectFromState = (state) => {
            const postUri = selectOpenPostUri(state);
            const connectionUri = decodeUriComponentProperly(state.getIn(['router', 'currentParams', 'connectionUri']));

            // either of 'tiles', 'grid', 'list'
            let layout = state.getIn(['router','currentParams','layout']);
            if(!layout) {
                layout = 'tiles';
            }

            const isOverview = displayingOverview(state);
            let matches;
            if(isOverview) { //overview
                matches = selectAllConnections(state).filter(conn => conn.get("state") === won.WON.Suggested);
            } else { // post-owner view
                matches = state.getIn(["needs", postUri, "connections"]).filter(conn => conn.get("state") === won.WON.Suggested);
            }

            return {
                isOverview,
                layout,
                //LAYOUT,
                connection: state.getIn(["needs", postUri, 'connections', connectionUri]),
                matchesArray: matches.toArray(),
                hasMatches: matches.size > 0,
            };
        };
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }

    selectedConnection(connectionUri) {
        if(this.isOverview) {
            this.router__stateGo('overviewMatches', {connectionUri});
        } else {
            this.router__stateGo('post', {connectionUri});
        }
    }
}
Controller.$inject = serviceDependencies;

function genComponentConf() {
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.matches', [
        overviewTitleBarModule,
        matchesFlowItemModule,
        matchesGridItemModule,
        sendRequestModule,
        connectionsOverviewModule,
        connectionSelectionModule,
    ])
    .directive('wonMatches', genComponentConf)
    //.controller('OverviewMatchesController', [...serviceDependencies,OverviewMatchesController])
    .name;

