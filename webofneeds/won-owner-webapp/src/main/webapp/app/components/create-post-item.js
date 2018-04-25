import angular from 'angular';
import ngAnimate from 'angular-animate';
import squareImageModule from '../components/square-image.js';
import { actionCreators }  from '../actions/actions.js';
import { 
    attach,
    getIn,
} from '../utils.js';
import { connect2Redux } from '../won-utils.js';

const serviceDependencies = ['$scope', '$ngRedux'];
function genComponentConf() {
    let template = `
        <div class="cpi__card">
            <div class="cpi__item clickable"
                ng-click="self.selectCreate('search')">
                <svg class="cpi__item__icon"
                    title="Create a new search"
                    style="--local-primary:var(--won-primary-color);">
                        <use href="#ico36_search"></use>
                </svg>
                <div class="cpi__item__text">
                    Search
                </div>
            </div>
            <div class="cpi__item clickable"
                ng-click="self.selectCreate('post')">
                <svg class="cpi__item__icon"
                    title="Create a new post"
                    style="--local-primary:var(--won-primary-color);">
                        <use href="#ico36_plus"></use>
                </svg>
                <div class="cpi__item__text">
                    Post
                </div>
            </div>
        </div>
        `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const self = this;
            const selectFromState = (state) => {
                const showCreateView = getIn(state, ['router', 'currentParams', 'showCreateView']);

                return {
                    showCreateView,
                }
            };
            connect2Redux(selectFromState, actionCreators, [], this);
        }
    
        selectCreate(type) {
            this.router__stateGoCurrent({connectionUri: undefined, postUri: undefined, showCreateView: type});
        }
    
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.createPostItem', [
    squareImageModule,
    ngAnimate,
])
    .directive('wonCreatePostItem', genComponentConf)
    .name;

