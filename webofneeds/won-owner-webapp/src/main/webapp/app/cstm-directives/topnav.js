 /**
 * Created by moru on 04/09/14.
 */

 angular.module('won.owner').directive('wonTopNav', function factory(userService) {
     return {
         restrict:'AE',
         templateUrl:"app/cstm-directives/test-template.partial.html",

         ////glitches hard atm (topnav gets duplicated over and over again)
         ////how does the topnav work atm?
         //templateUrl:"app/cstm-directives/topnav.partial.html",

         //TODO http://getbootstrap.com/javascript/#popovers for signin/up

         scope:{}
     }
 });
