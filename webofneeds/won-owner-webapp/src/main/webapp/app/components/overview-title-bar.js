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
                    <li class=""><a href="#">Feed</a></li>
                    <li><a href="#">Posts
                        <span class="mtb__tabs__unread">5</span>
                    </a></li>
                    <li class="mtb__tabs__selected"><a href="#">Incoming Requests
                        <span class="mtb__tabs__unread">5</span>
                    </a></li>
                    <li><a href="#">Matches
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
        template: template
    }
}

export default angular.module('won.owner.components.overviewTitleBar', [])
    .directive('wonOverviewTitleBar', genComponentConf)
    .name;
