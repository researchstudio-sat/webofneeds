/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';

function genLoginConf() {
    let template = `
    <div class="avatarimage__header">
        <a class="avatarimage__header__button clickable" ng-click="self.open = false">
            <svg style="--local-primary:var(--won-primary-color);"
                class="avatarimage__header__button__iconsmall">
                    <use xlink:href="#ico36_person" href="#ico36_person"></use>
            </svg>
            <svg class="avatarimage__header__button__carret" style="--local-primary:black;">
                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
            </svg>

        </a>
    </div>
    <div class="avatarimage__grid">
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
        <svg style="--local-primary:var(--won-primary-color);"
            class="avatarimage__grid__item clickable">
                <use xlink:href="#ico36_person" href="#ico36_person"></use>
        </svg>
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

