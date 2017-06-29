/**
 * Created by ksinger on 16.09.2015.
 */

import angular from 'angular';

function genComponentConf() {
    function link(scope, element, attrs) {
        element.bind("keyup", function (event) {
            if(event.which === 13) {
                scope.$apply(function (){
                    scope.$eval(attrs.wonEnter);
                });

                event.preventDefault();
            }
        });
    }

    return {
        restrict: 'A',
        link,
    }
}
export default angular.module('won.owner.directives.enter', [
    ])
    .directive('wonEnter', genComponentConf)
    .name;
