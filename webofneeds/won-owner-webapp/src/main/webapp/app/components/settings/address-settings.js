;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <div class="leftside" ng-show="self.items">
                <div class="title withoutPadding">Manage Addresses</div>
                <div class="subtitle">can be set individually on create posts page</div>
                <div class="setting" ng-repeat="item in self.items">
                    <div class="inputflex">
                        <label class="label" for="addressname">Name</label>
                        <div class="inputside">
                            <input id="addressname" type="text" required="true" placeholder="Address Name" ng-model="settings.addressname"/>
                            <div class="subtitle">This will be only visible for me</div>
                        </div>
                    </div>
                    <div class="inputflex">
                        <label class="label" for="address1">Address Line 1</label>
                        <div class="inputside">
                            <input id="address1" type="text" required="true" placeholder="Address Line 1" ng-model="settings.addressline1"/>
                        </div>
                    </div>
                    <div class="inputflex">
                        <label class="label" for="address2">Address Line 2</label>
                        <div class="inputside">
                            <input id="address2" type="text" required="true" placeholder="Address Line 2" ng-model="settings.addressline2"/>
                        </div>
                    </div>
                    <div class="inputflex">
                        <label class="label" for="address3">Address Line 3</label>
                        <div class="inputside">
                            <input id="address3" type="text" required="true" placeholder="Address Line 3" ng-model="settings.addressline3"/>
                        </div>
                    </div>
                    <div class="inputflex">
                        <label class="label" for="country">Country</label>
                        <div class="inputside">
                            <select id="country" required="true"/>
                        </div>
                    </div>
                    <div class="flexbuttons">
                        <a class="ac__button clickable">
                            <img src="generated/icon-sprite.svg#ico36_close_circle" class="ac__button__icon">
                            <span class="ac__button__caption">Remove</span>
                        </a>
                        <button class="won-button--filled red" ng-click="settings.saveAddress()">Save</button>
                    </div>
                </div>
                <a class="ac__button clickable">
                    <img src="generated/icon-sprite.svg#ico36_plus" class="ac__button__icon">
                    <span class="ac__button__caption">Add new default address</span>
                </a>
            </div>
            <div class="noaddresses" ng-show="!self.items">
                <img class="noaddresses__icon" src="generated/icon-sprite.svg#ico36_location_circle_grey"/>
                <div class="title">You don't use any addresses yet</div>
                <div class="description">You can specify your default adress to be at hand when posting needs. Adresses can be set individually on create posts page as well.</div>
                <a class="ac__button clickable">
                    <img src="generated/icon-sprite.svg#ico36_plus" class="ac__button__icon">
                    <span class="ac__button__caption">Add new default address</span>
                </a>
            <div>
    `;

    class Controller {
        constructor() { }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {items : "="},
        template: template
    }
}
export default angular.module('won.owner.components.addressSettings', [])
    .directive('wonAddressSettings', genComponentConf)
    .name;
