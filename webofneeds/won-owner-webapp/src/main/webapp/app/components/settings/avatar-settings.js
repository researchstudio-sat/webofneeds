;

import angular from 'angular';
import avatarImageSelectorModule from '../settings/avatarimage-selector.js';

function genComponentConf() {
    let template = `
            <div class="noavatars" ng-show="!self.items">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="avataricon">
                        <use xlink:href="#ico36_person" href="#ico36_person"></use>
                </svg>
                <div class="title">You don't use any avatars yet</div>
                <div class="description">While WON is anonymous, avatars are only used for communication with in groups. Introduction text thea explains the general concept. Mi, offici dolut quid maximaio dolupta dis intotatquam fuga. Ut acero venest as solore minctemporia cus est, volore ne im qui volorem ipiciuscium velibus ciendusto distibustrum asin repe re laborerum ent, sam con es eaque endempos apienimet fuga.</div>

                <a class="ac__button clickable">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="ac__button__icon">
                        <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
                </svg>
                    <span class="ac__button__caption">Create new Avatar</span>
                </a>
            </div>

            <div class="flexbuttons top" ng-show="self.items">
                <a class="ac__button clickable">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="ac__button__icon">
                        <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
                </svg>
                    <span class="ac__button__caption">Create new Avatar</span>
                </a>
            </div>
            <div class="avatargrid" ng-show="self.items">
                <div class="avatar" ng-repeat="item in self.items" ng-show="self.items">
                    <div>
                        <div class="avatar__header">
                            <div class="avatar__header__name">
                                <label class="label">Avatarname</label>
                                <div class="subtitle">Created on {{item.creationDate}}</div>
                            </div>
                            <a class="avatar__header__button clickable" ng-click="item.open = true">
                                <svg style="--local-primary:var(--won-primary-color);"
                                    class="avatar__header__button__iconsmall">
                                        <use xlink:href="#ico36_person" href="#ico36_person"></use>
                                </svg>
                                <svg style="--local-primary:var(--won-primary-color);"
                                    class="avatar__header__button__carret">
                                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                                </svg>
                            </a>
                        </div>
                        <div class="avatar__overlay">
                            <won-avatar-image-selector open="item.open" ng-show="item.open" ></won-avatar-image-selector>
                        </div>
                        <div class="inputflex">
                            <label class="label" for="screenname">Screen name</label>
                            <div class="inputside">
                                <input id="screenname" type="text" required="true" placeholder="Screen name" ng-model="item.screenName"/>
                            </div>
                        </div>
                        <div class="inputflex">
                            <label class="label" for="description">Avatar description</label>
                            <div class="inputside">
                                <input id="description" type="text" required="true" placeholder="Avatar Description" ng-model="item.description"/>
                                <div class="subtitle">This will be only visible for me</div>
                            </div>
                        </div>
                        <div class="inputflex">
                            <label class="label" for="aboutme">About me</label>
                            <div class="inputside">
                                <input id="aboutme" type="text" required="true" placeholder="Avatar Description" ng-model="item.aboutMe"/>
                                <div class="subtitle">visible if Avatar is used within groups groups</div>
                            </div>
                        </div>
                        <div class="inputflex">
                            <label class="label">Used in following groups</label>
                            <ul class="inputside" ng-show="item.usedInGroups">
                                <li ng-repeat="group in item.usedInGroups">{{group.name}}</li>
                            </ul>
                            <label class="label" ng-show="!item.usedInGroups">none yet</label>
                        </div>
                        <a class="ac__button clickable">
                            <svg style="--local-primary:var(--won-primary-color);"
                                class="ac__button__icon">
                                    <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
                            </svg>
                            <span class="ac__button__caption">Remove this avatar</span>
                        </a>
                    </div>
                </div>
            </div>
            <div class="flexbuttons bottom" ng-show="self.items">
                <button class="won-button--filled red" ng-click="settings.saveAccount()">Save Settings</button>
            </div>`;

    class Controller {
        constructor() {
            this.items = [{open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", usedInGroups: [{name: "group1"}, {name: "group2"}, {name: "group4"}], creationDate: "10.01.2015"},
                {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", creationDate: "10.01.2015"},
                {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", usedInGroups: [{name: "group1"}, {name: "group2"}, {name: "group4"}], creationDate: "10.01.2015"},
                {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", creationDate: "10.01.2015"},
                {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", usedInGroups: [{name: "group1"}, {name: "group2"}, {name: "group4"}], creationDate: "10.01.2015"},
                {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", creationDate: "10.01.2015"},
                {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", usedInGroups: [{name: "group1"}, {name: "group2"}, {name: "group4"}], creationDate: "10.01.2015"},
                {open: false, screenName: "", imageUrl: "", description: "", aboutMe: "", creationDate: "10.01.2015"}];

        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},//items: '='},
        template: template
    }
}
export default angular.module('won.owner.components.avatarSettings', [
    avatarImageSelectorModule
])
    .directive('wonAvatarSettings', genComponentConf)
    .name;
