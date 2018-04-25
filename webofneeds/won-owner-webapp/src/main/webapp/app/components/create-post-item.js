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
        <div class="cpi__item">
            <div >
                <svg class="cpi__icon clickable"
                    ng-click="self.selectCreate('search')"
                    title="Create a new search"
                    style="--local-primary:var(--won-primary-color);">
                        <use href="#ico36_search"></use>
                </svg>
                Create New Search
            </div>
            <div >
                <svg class="cpi__icon clickable"
                    ng-click="self.selectCreate('post')"
                    title="Create a new post"
                    style="--local-primary:var(--won-primary-color);">
                        <use href="#ico36_plus"></use>
                </svg>
                Create New Post
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

