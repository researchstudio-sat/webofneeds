/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';
import loginComponent from 'app/components/login';

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
                <div class="topnav__inner__right">
                    <ul class="topnav__list">
                        <li>
                            <button class="topnav__button won-button--filled lighterblue" ng-show="!self.open">Sign up</button>
                        </li>
                        <li>
                            <a href="#" class="topnav__button" ng-click="self.open = !self.open" ng-class="self.open? 'open' : ''">
                                <span class="topnav__button__caption__always">Sign in</span>
                                <img src="generated/icon-sprite.svg#ico16_arrow_down" ng-show="!self.open" class="topnav__carret">
                                <img src="generated/icon-sprite.svg#ico16_arrow_up_hi" ng-show="self.open" class="topnav__carret">
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
        <nav class="loginOverlay" ng-show="self.open">
            <div class="lo__inner">
                <div class="lo__inner__right">
                    <won-login open="self.open"></won-login>
                </div>
            </div>
        </nav>
    `;


    return {
        restrict: 'E',
        //link: link,
        //controllerAs: 'ctrl',
        //controller: TopnavCtrl,
        template: template
    }
}

export default angular.module('won.owner.components.landingpagetopnav', [
    loginComponent
])
    .directive('wonLandingpageTopnav', genTopnavConf)
    .name;

