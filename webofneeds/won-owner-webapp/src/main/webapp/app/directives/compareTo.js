/**
 * Attribute Directive for password repeat validation
 */

import angular from "angular";

function genComponentConf() {
  function link(scope, element, attrs, ngModel) {
    ngModel.$validators.compareTo = function(modelValue) {
      return modelValue == scope.otherModelValue;
    };

    scope.$watch("otherModelValue", function() {
      ngModel.$validate();
    });
  }

  return {
    require: "ngModel",
    scope: {
      otherModelValue: "=compareTo",
    },
    restrict: "A",
    link,
  };
}
export default angular
  .module("won.owner.directives.compareTo", [])
  .directive("compareTo", genComponentConf).name;
