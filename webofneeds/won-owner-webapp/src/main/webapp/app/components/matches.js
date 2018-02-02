;

import Immutable from 'immutable';
import won from '../won-es6.js';
import angular from 'angular';
import overviewTitleBarModule from './overview-title-bar.js';
import matchesFlowItemModule from './matches-flow-item.js';
import connectionsMapModule from './connections-map.js';
import sendRequestModule from './send-request.js';
import connectionsOverviewModule from './connections-overview.js';
import connectionSelectionModule from './connection-selection.js';

import {
    attach,
    decodeUriComponentProperly,
    getIn,
} from '../utils.js';
import {
    connect2Redux,
} from '../won-utils.js';
import { labels } from '../won-label-utils.js';
import { actionCreators }  from '../actions/actions.js';
import {
    selectOpenPostUri,
    displayingOverview,
    selectAllConnections,
    selectNeedByConnectionUri,
} from '../selectors.js';

const serviceDependencies = ['$ngRedux', '$scope'];
let template = `
    <a class="curtain" ng-if="self.connection"></a>
    <div class="omc__inner" ng-class="{'empty' : !self.hasMatches}">
        <div class="omc__empty" ng-if="!self.hasMatches">
            <div class="omc__empty__description">
                <svg class="omc__empty__description__icon"
                    style="--local-primary:#CCD2D2;">
                        <use xlink:href="#ico36_match"></use>
                </svg>
                <span class="omc__empty__description__text">The matches to all your needs will be listed here. 
                 You cannot influence the matching process. It might take some time, or maybe there is nothing to
                    be found for you, yet. Check back later or post more needs!</span>
            </div>
            <a ng-click="self.router__stateGoResetParams('createNeed')" class="omc__empty__link clickable">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="omc__empty__link__icon">
                        <use xlink:href="#ico36_plus"></use>
                </svg>
                <span class="omc__empty__link__caption">Create a Need</span>
            </a>
        </div>
        <div class="omc__header" ng-if="self.hasMatches">
            <div class="title">Matches to your post{{ self.isOverview? 's' : '' }}</div>
            <div class="omc__header__viewtype">
                <a ng-click="self.router__stateGoCurrent({layout: self.LAYOUT.TILES})"
                   class="clickable">
                    <svg class="omc__header__viewtype__icon clickable" 
                        style="--local-primary:{{self.layout === 'tiles' ? 'black' : 'var(--won-primary-color)'}};">
                            <use xlink:href="#ico-filter_tile"></use>
                    </svg>
                </a>
                <a ng-click="self.router__stateGoCurrent({layout: self.LAYOUT.LIST})"
                   class="clickable">
                    <svg class="omc__header__viewtype__icon clickable" 
                        style="--local-primary:{{self.layout === 'list' ? 'black' : 'var(--won-primary-color)'}};">
                            <use xlink:href="#ico-filter_list"></use>
                    </svg>
                </a>                
                <a ng-click="self.router__stateGoCurrent({layout: self.LAYOUT.MAP})"
                   class="clickable">
                    <svg class="omc__header__viewtype__icon clickable" 
                        style="--local-primary:{{self.layout === 'map' ? 'black' : 'var(--won-primary-color)'}};">
                            <use xlink:href="#ico-filter_map"></use>
                    </svg>
                </a>
            </div>
        </div>
        <div ng-if="self.hasMatches && self.layout === 'tiles'" class="omc__content__flow">
            <won-matches-flow-item
                    connection-uri="match.get('uri')"
                    ng-repeat="match in self.matchesArray">
            </won-matches-flow-item>
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
        <div ng-if="self.hasMatches && self.layout === 'map'" class="omc__content__map">
            <won-connections-map connection-type="::self.WON.Suggested" on-selected-connection="self.selectedConnection(connectionUri)"></won-connections-map>
        </div>
    </div>
    <div class="omc__sendrequest" ng-if="self.hasMatches && self.connection">
        <won-send-request></won-send-request>
    </div>
`;

const LAYOUT = Object.freeze({ TILES: 'tiles', LIST: 'list', MAP: 'map'});

class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.omc4dbg = this;

        this.WON = won.WON;
        this.LAYOUT = LAYOUT;
        this.labels = labels;

        const selectFromState = (state) => {
            let postUri = selectOpenPostUri(state);
            const connectionUri = decodeUriComponentProperly(getIn(state, ['router', 'currentParams', 'connectionUri']));
            const isWhatsAround= getIn(state, ["needs", postUri, "isWhatsAround"]);

            // either of 'tiles', 'list'
            let layout = getIn(state, ['router','currentParams','layout']);
            if(!layout) {
                layout = 'list';
            }

            const isOverview = displayingOverview(state);


            let matches;
            if(isOverview) { //overview
                const allConnections = selectAllConnections(state);
                matches = allConnections && allConnections.filter(conn => conn.get("state") === won.WON.Suggested);
            } else { // post-owner view
                const postConnections = state.getIn(["needs", postUri, "connections"]);
                matches = postConnections && postConnections.filter(conn => conn.get("state") === won.WON.Suggested);
            }

            if(!postUri && connectionUri){
                const needByConnection = selectNeedByConnectionUri(state, connectionUri);
                postUri = needByConnection && needByConnection.get("uri");
            }

            return {
                isOverview,
                layout,
                //LAYOUT,
                isWhatsAround: state.getIn(["needs", postUri, "isWhatsAround"]),
                connection: state.getIn(["needs", postUri, 'connections', connectionUri]),
                matchesArray: matches && matches.toArray(),
                hasMatches: matches && matches.size > 0,
            };
        };
        connect2Redux(selectFromState, actionCreators, [], this);
    }

    selectedConnection(connectionUri) {
        this.router__stateGoCurrent({connectionUri});
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
        sendRequestModule,
        connectionsOverviewModule,
        connectionSelectionModule,
        connectionsMapModule,
    ])
    .directive('wonMatches', genComponentConf)
    //.controller('OverviewMatchesController', [...serviceDependencies,OverviewMatchesController])
    .name;

