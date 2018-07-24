import angular from "angular";

function genComponentConf() {
  function link(scope, element, attrs) {
    const listener = element[0].addEventListener("input", () => {
      scope.$apply(() => {
        scope.$eval(attrs.wonInput);
      });
    });
    scope.$on("$destroy", () => {
      element[0].removeEventListener("input", listener);
    });
  }

  return {
    restrict: "A",
    link,
  };
}
export default angular
  .module("won.owner.directives.input", [])
  .directive("wonInput", genComponentConf).name;
