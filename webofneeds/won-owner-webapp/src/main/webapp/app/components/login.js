/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';

function genLoginConf() {
    let template = `<a href="#" class="wl__button" ng-click="self.open = !self.open">
                        <span class="wl__button__caption">Sign in</span>
                        <img src="generated/icon-sprite.svg#ico16_arrow_up_hi" class="wl__button__carret">
                    </a>
                    <input type="text" required="true" placeholder="Email address" ng-model="self.email" required type="email"/>
                    <input type="text" required="true" placeholder="Password" ng-model="self.password" required type="password"/>
                    <div class="wl__table">
                        <div class="wlt__left">
                            <input type="checkbox" ng-model="self.rememberMe" id="rememberMe"/><label for="rememberMe">Remember me</label>
                        </div>
                        <div class="wlt__right">
                            <a href="#">Forgot Password?</a>
                        </div>
                    </div>
                    <button class="won-button--filled lighterblue">Sign in</button>
                    <span class="wl__register">No Account yet? <a href="#">Register</a></span>`;


    class Controller {}

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {open: '='},
        template: template
    }
}

export default angular.module('won.owner.components.login', [])
    .directive('wonLogin', genLoginConf)
    .name;

