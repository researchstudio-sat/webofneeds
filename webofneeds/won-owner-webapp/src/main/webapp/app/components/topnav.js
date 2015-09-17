/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';

function genTopnavConf() {
    let template = `
        <nav class="topnav">
            <div class="topnav__inner">
                <div class="topnav__inner__left">
                    <a href="#" class="topnav__button">
                        <img src="generated/icon-sprite.svg#WON_ico_header" class="topnav__button__icon">
                        <span class="topnav__page-title topnav__button__caption">Web of Needs</span>
                    </a>
                </div>
                <div class="topnav__inner__center">
                    <a ng-link="createNeed" class="topnav__button">
                        <img src="generated/icon-sprite.svg#ico36_plus" class="topnav__button__icon">
                        <span class="topnav__button__caption">New Need</span>
                    </a>
                </div>
                <div class="topnav__inner__right">
                    <ul class="topnav__list">
                        <li>
                            <a href="#" class="topnav__button">
                                <span class="topnav__button__caption">Username D...</span>
                                <img src="generated/icon-sprite.svg#ico16_arrow_down" class="topnav__carret">
                                <img src="generated/icon-sprite.svg#ico36_person" class="topnav__button__icon">
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    `


    return {
        restrict: 'E',
        //link: link,
        //controllerAs: 'ctrl',
        //controller: TopnavCtrl,
        template: template
    }
}

export default angular.module('won.owner.components.topnav', [])
    .directive('wonTopnav', genTopnavConf)
    .name;

