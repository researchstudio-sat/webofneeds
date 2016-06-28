;

import angular from 'angular';
import overviewTitleBarModule from './overview-title-bar';
import matchesFlowItemModule from './matches-flow-item';
import matchesGridItemModule from './matches-grid-item';
import matchesListItemModule from './matches-list-item';
import sendRequestModule from './send-request';

import { attach, mapToMatches, decodeUriComponentProperly} from '../utils';
import { labels } from '../won-label-utils';
import { actionCreators }  from '../actions/actions';
import { selectAllByConnections, selectOpenPostUri, displayingOverview } from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];
let template = `
    <a class="curtain" ng-if="self.connection"></a>
    <div class="omc__inner">
        <div class="omc__header">
            <div class="dummy"></div>
            <div class="title" ng-if="!self.post">Matches to your needs</div>
            <div class="omc__header__viewtype">
                <a ui-sref="{{ self.isOverview ? 'overviewMatches({layout: self.LAYOUT.TILES})' : 'post({layout : self.LAYOUT.TILES})' }}">
                    <img ng-src="{{self.layout === 'tiles' ? 'generated/icon-sprite.svg#ico-filter_tile_selected' : 'generated/icon-sprite.svg#ico-filter_tile'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
                <a ui-sref="{{ self.isOverview ? 'overviewMatches({layout: self.LAYOUT.GRID})' : 'post({layout : self.LAYOUT.GRID})' }}">
                    <img ng-src="{{self.layout === 'grid' ? 'generated/icon-sprite.svg#ico-filter_compact_selected' : 'generated/icon-sprite.svg#ico-filter_compact'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
                <a ui-sref="{{ self.isOverview ? 'overviewMatches({layout: self.LAYOUT.LIST})' : 'post({layout : self.LAYOUT.LIST})' }}">
                    <img ng-src="{{self.layout === 'list' ? 'generated/icon-sprite.svg#ico-filter_list_selected' : 'generated/icon-sprite.svg#ico-filter_list'}}"
                     class="omc__header__viewtype__icon clickable"/>
                </a>
            </div>
        </div>
        <div ng-if="self.layout === 'tiles'" class="omc__content__flow">
            <won-matches-flow-item
                    connection-uri="m.getIn(['connection','uri'])"
                    ng-repeat="m in self.matches">
            </won-matches-flow-item>
        </div>
        <div ng-if="self.layout === 'grid'" class="omc__content__grid">
            <won-matches-grid-item
                    connection-uri="m.getIn(['connection','uri'])"
                    ng-repeat="m in self.matches">
            </won-matches-grid-item>
        </div>
        <div ng-if="self.layout === 'list'" class="omc__content__list">
            <won-matches-list-item
                    item="item"
                    ng-repeat="(key,item) in self.matchesOfNeed">
            </won-matches-list-item>
        </div>
    </div>
    <div class="omc__sendrequest" ng-if="self.connection">
        <won-send-request></won-send-request>
    </div>
`

const LAYOUT = Object.freeze({ TILES: 'tiles', GRID: 'grid', LIST: 'list'});

class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.omc=this;

        this.labels = labels;

        const selectFromState = (state) => {
            const allMatchesByConnections = selectAllByConnections(state)
                    .filter(conn => conn.getIn(['connection', 'hasConnectionState']) === won.WON.Suggested);

            const postUri = selectOpenPostUri(state);
            const connectionUri = decodeUriComponentProperly(state.getIn(['router', 'currentParams', 'connectionUri']));


            // either of 'tiles', 'grid', 'list'
            let layout = state.getIn(['router','currentParams','layout']);
            if(!layout) {
                layout = 'tiles';
            }

            const isOverview = displayingOverview(state);
            let matchesByConnectionUri;
            if(isOverview) { //overview
                matchesByConnectionUri = allMatchesByConnections
                    .toList();
            } else { // post-owner view
                matchesByConnectionUri = allMatchesByConnections
                    .filter(conn => conn.getIn(['ownNeed', 'uri']) === postUri)
                    .toList();
            }

            return {
                isOverview,
                layout,
                LAYOUT,
                connection: state.getIn(['connections', connectionUri]),
                matches: matchesByConnectionUri.toArray(),
                matchesOfNeed: mapToMatches(matchesByConnectionUri.toJS()),//TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.
                post: state.getIn(['needs','ownNeeds', postUri]),
            };
        };
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
      //  this.loadMatches();
        this.$scope.$on('$destroy', disconnect);
    }

    loadMatches(){
        this.matches__load(
            this.$ngRedux.getState().getIn(['needs','ownNeeds']).toJS()
        )
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
        matchesListItemModule,
        sendRequestModule
    ])
    .directive('wonMatches', genComponentConf)
    //.controller('OverviewMatchesController', [...serviceDependencies,OverviewMatchesController])
    .name;

