;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <div class="rightside" ng-show="self.items">
                <div class="title withoutPadding">Manage Notification Settings</div>
                <div class="subtitle">can be set individually on create posts page</div>
                <div class="setting" ng-repeat="item in self.items">
                    <div class="inputflex">
                        <label class="label" for="name">Name</label>
                        <div class="inputside">
                            <input id="name" type="text" required="true" placeholder="Notification Name" ng-model="settings.name"/>
                            <div class="subtitle">This will be only visible for me</div>
                        </div>
                    </div>
                    <div class="inputflex">
                        <label class="label">Notify me by email in case of</label>

                        <div class="inputside">
                            <div class="inputside__flexrow">
                                <input id="newconversations" type="checkbox" required="true" ng-model="settings.newconversations"/>
                                <label class="label" for="newconversations">new conversation requests</label>
                            </div>
                            <div class="inputside__flexrow">
                                <input id="newmessages" type="checkbox" required="true" ng-model="settings.newmessages"/>
                                <label class="label" for="newmessages">new messages</label>
                            </div>
                            <div class="inputside__flexrow">
                                <input id="newmatches" type="checkbox" required="true" ng-model="settings.newmatches"/>
                                <label class="label" for="newmatches">new matches</label>
                            </div>
                        </div>
                    </div>
                    <div class="flexbuttons">
                        <a class="ac__button clickable">
                            <img src="generated/icon-sprite.svg#ico36_close_circle" class="ac__button__icon">
                            <span class="ac__button__caption">Remove</span>
                        </a>
                        <button class="won-button--filled red" ng-click="settings.saveAccount()">Save</button>
                    </div>
                </div>
                <a class="ac__button clickable">
                    <img src="generated/icon-sprite.svg#ico36_plus" class="ac__button__icon">
                    <span class="ac__button__caption">Add new notification settings</span>
                </a>
            </div>
            <div class="nonotifications" ng-show="!self.items">
                <img class="nonotifications__icon" src="generated/icon-sprite.svg#ico36_notification_circle_grey"/>
                <div class="title">You don't use Notification settings yet</div>
                <div class="description">You can specify your default Notifications to be at hand when posting needs. Notifications can be set individually on create posts page as well.</div>
                <a class="ac__button clickable">
                    <img src="generated/icon-sprite.svg#ico36_plus" class="ac__button__icon">
                    <span class="ac__button__caption">Add new notification settings</span>
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
export default angular.module('won.owner.components.notificationSettings', [])
    .directive('wonNotificationSettings', genComponentConf)
    .name;
