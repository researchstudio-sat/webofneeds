/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';

function genLoginConf() {
    let template = `
                    <div class="avatarimage__header">
                        <a class="avatarimage__header__button clickable" ng-click="self.open = false">
                            <img class="avatarimage__header__button__iconsmall" src="generated/icon-sprite.svg#ico36_person"/>
                            <img src="generated/icon-sprite.svg#ico16_arrow_up_hi" class="avatarimage__header__button__carret">
                        </a>
                    </div>
                    <div class="avatarimage__grid">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                        <img class="avatarimage__grid__item clickable" src="generated/icon-sprite.svg#ico36_person">
                    </div>
                    <div class="avatarimage__footer">
                        <a class="clickable">Upload user image</a>
                    </div>`;


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

export default angular.module('won.owner.components.avatarImageSelector', [])
    .directive('wonAvatarImageSelector', genLoginConf)
    .name;

