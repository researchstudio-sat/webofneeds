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
        <div class="cpi__item clickable"
            ng-click="self.selectCreate(self.SEARCH)"
            ng-class="{'selected': self.isSearch}">
            <svg class="cpi__item__icon"
                title="Create a new search"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="#ico36_search" href="#ico36_search"></use>
            </svg>
            <div class="cpi__item__text">
                Search
            </div>
        </div>
        <div class="cpi__item clickable"
            ng-click="self.selectCreate(self.POST)"
            ng-class="{'selected': self.isPost}">
            <svg class="cpi__item__icon"
                title="Create a new post"
                style="--local-primary:var(--won-primary-color);">
                    <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
            </svg>
            <div class="cpi__item__text">
                Post
            </div>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            this.SEARCH = "search";
            this.POST = "post";
            const self = this;

            const selectFromState = (state) => {
                const showCreateView = getIn(state, ['router', 'currentParams', 'showCreateView']);
                const isSearch = showCreateView === this.SEARCH;
                const isPost = showCreateView && !isSearch;

                return {
                    isSearch,
                    isPost,
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

