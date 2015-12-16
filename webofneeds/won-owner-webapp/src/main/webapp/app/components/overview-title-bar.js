/**
 * Created by ksinger on 20.08.2015.
 */
;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <nav ng-cloak ng-show="{{true}}" class="main-tab-bar">
            <div class="mtb__inner">
                <ul class="mtb__inner__center mtb__tabs">
                    <li ng-class="self.selection == 0? 'mtb__tabs__selected' : ''"><a ui-sref="feed">Feed</a></li>
                    <li ng-class="self.selection == 1? 'mtb__tabs__selected' : ''"><a ui-sref="overviewPosts">Posts
                        <span class="mtb__tabs__unread">5</span>
                    </a></li>
                    <li ng-class="self.selection == 2? 'mtb__tabs__selected' : ''"><a ui-sref="overviewIncomingRequests">Incoming Requests
                        <span class="mtb__tabs__unread">5</span>
                    </a></li>
                    <li ng-class="self.selection == 3? 'mtb__tabs__selected' : ''"><a ui-sref="overviewMatches">Matches
                        <span class="mtb__tabs__unread">18</span>
                    </a></li>
                </ul>
                <div class="mtb__inner__right">
                    <a href="#" class="mtb__searchbtn">
                        <img src="generated/icon-sprite.svg#ico36_search_nomargin" class="mtb__icon">
                    </a>
                </div>
            </div>
        </nav>
    `;

    class Controller {
        constructor() { }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {selection: "="},
        template: template
    }
}

export default angular.module('won.owner.components.overviewTitleBar', [])
    .directive('wonOverviewTitleBar', genComponentConf)
    .name;
