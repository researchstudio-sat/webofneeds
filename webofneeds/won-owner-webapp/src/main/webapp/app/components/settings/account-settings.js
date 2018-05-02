;

import angular from 'angular';

function genComponentConf() {
    let template = `
        <div class="title">Account</div>
        <div class="flexrow">
            <div class="leftside">
                <div class="inputflex">
                    <label class="label" for="email">Email</label>
                    <div class="inputside">
                        <input id="email" type="text" required="true" placeholder="Email address" ng-model="settings.email" required type="email"/>
                    </div>
                </div>
            </div>
            <div class="rightside">
                <div class="inputflex">
                    <label class="label" for="password">Password</label>
                    <div class="inputside">
                        <input id="password" type="text" required="true" placeholder="Password" ng-model="settings.password" required type="password"/>
                    </div>
                </div>
                <div class="inputflex">
                    <label class="label" for="password_repeat">Repeat Password</label>
                    <div class="inputside">
                        <input id="password_repeat" type="text" required="true" placeholder="Repeat Password" ng-model="settings.repeatPassword" required type="password"/>
                    </div>
                </div>
            </div>
        </div>
        <div class="flexbuttons">
            <a class="ac__button clickable">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="ac__button__icon">
                        <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
                </svg>
                <span class="ac__button__caption">Remove this account</span>
            </a>
            <button class="won-button--filled red" ng-click="settings.saveAccount()">Save Settings</button>
        </div>
    `;

    class Controller {
        constructor() { }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}
export default angular.module('won.owner.components.acountSettings', [])
    .directive('wonAccountSettings', genComponentConf)
    .name;
