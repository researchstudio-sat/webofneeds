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
                            <svg style="--local-primary:var(--won-primary-color);"
                                class="ac__button__icon" >
                                    <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
                            </svg>
                            <span class="ac__button__caption">Remove</span>
                        </a>
                        <button class="won-button--filled red" ng-click="settings.saveAccount()">Save</button>
                    </div>
                </div>
                <a class="ac__button clickable">
                    <svg style="--local-primary:var(--won-primary-color);"
                        class="ac__button__icon">
                            <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
                    </svg>
                    <span class="ac__button__caption">Add new notification settings</span>
                </a>
            </div>
            <div class="nonotifications" ng-show="!self.items">
                <svg class="nonotifications__icon"
                    style="--local-primary:#CCD2D2;">
                        <use xlink:href="#ico36_notification_circle" href="#ico36_notification_circle"></use>
                </svg>
                <div class="title">You don't use Notification settings yet</div>
                <div class="description">You can specify your default Notifications to be at hand when posting needs. Notifications can be set individually on create posts page as well.</div>
                <a class="ac__button clickable">
                    <svg style="--local-primary:var(--won-primary-color);"
                        class="ac__button__icon">
                            <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
                    </svg>
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
