/**
 * Extension of ui.bootstrap.popover that accepts generic html / a template as content for the popup
 */
angular.module( 'won.directives', [ 'ui.bootstrap.tooltip' ] )

.directive( 'htmlPopoverPopup', function () {
  return {
    restrict: 'EA',
    replace: true,
    scope: { title: '@', content: '@', placement: '@', animation: '&', isOpen: '&' },
    templateUrl: 'custom_components/html_popover/popover.html'
  };
})

.directive( 'htmlPopover', [ '$tooltip', function ( $tooltip ) {
  return $tooltip( 'popover', 'popover', 'click' );
}]);