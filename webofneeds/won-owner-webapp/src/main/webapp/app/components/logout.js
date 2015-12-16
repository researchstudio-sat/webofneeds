/**
 * Created by ksinger on 20.08.2015.
 */
;
import angular from 'angular';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';

function genLogoutConf() {
    let template = `<a href="#" class="wl__button" ng-click="self.open = !self.open">
                        <span class="wl__button__caption">{{self.email}}</span>
                        <img src="generated/icon-sprite.svg#ico16_arrow_up" class="wl__button__carret">
                        <img src="generated/icon-sprite.svg#ico36_person" class="wl__button__icon">
                    </a>
                    <button class="won-button--filled lighterblue" ng-click="::self.logout()">Sign out</button>`;

    const serviceDependencies = ['$q', '$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */){
            attach(this, serviceDependencies, arguments);

            this.email = "";
            this.password = "";

            const logout = (state) => ({
                loggedIn: state.get('user').toJS().loggedIn,
                email: state.get('user').toJS().email
            });

            const disconnect = this.$ngRedux.connect(logout, actionCreators)(this);
            this.$scope.$on('$destroy',disconnect);
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {open: '='},
        template: template
    }
}

export default angular.module('won.owner.components.logout', [])
    .directive('wonLogout', genLogoutConf)
    .name;

