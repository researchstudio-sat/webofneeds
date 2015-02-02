// set up explained: http://lostechies.com/gabrielschenker/2013/12/30/angularjspart-7-getting-ready-to-test/
describe('Test for being able to run a test file:', function() {

    beforeEach(module('won.owner'));
    //beforeEach(function() { module('won.owner'); });
    beforeEach(inject(function ($injector) {
        rootScope = $injector.get('$rootScope');
        injector = $injector;
        //$scope = $rootScope.$new();
        //$http = $injector.get('$http');
        $q = $injector.get('$q');
    }));

    describe('Test without app components', function() {
        it('sould test that Hello equals Hello',
            function() {
                expect("Hello").toEqual("Hello");
            });

        it('Test testing a promise', function(){

            $q = injector.get('$q');
            var deferred = $q.defer();
            var handler = jasmine.createSpy('success');
            deferred.promise.then(handler);
            deferred.resolve(10);
            rootScope.$digest();
            expect(handler).toHaveBeenCalledWith(10);

        });
    });



//    describe('Test with an app controller', function() {
//        // controller injection taken from http://www.ng-newsletter.com/advent2013/#!/day/19
//        beforeEach(inject(function($rootScope, $controller, _userService_) {
//            // Create a new scope that's a child of the $rootScope
//            scope = $rootScope.$new();
//            userService = _userService_;
//            userService.isAuth = function () { return true; };
//
//            // Create the controller
//            ctrl = $controller('CreateNeedCtrlNew', {
//                $scope: scope
//            });
//        }));
//
//
//        it('sould test that CreateNeedCtrlNew controller creates a clean need with specified in test title',
//            function() {
//                scope.title = "test-need-title";
//                var cleanNeed = scope.getCleanNeed();
//                expect(cleanNeed.title).toEqual("test-need-title");
//            });
//    });


    // several ways to inject a service: http://stackoverflow.com/questions/13013772/how-do-i-test-an-angularjs-service-with-jasmine
    describe('Test with an app service', function() {
        it('sould test that applicationStateService knows matcher search service ' +
                'uri as http://sat001.researchstudio.at:8080/matcher/search/'
            , inject(function(applicationStateService){

                expect(applicationStateService.getMatcherURI())
                    .toEqual("http://sat001.researchstudio.at:8080/matcher/search/");

            }));
    });


});