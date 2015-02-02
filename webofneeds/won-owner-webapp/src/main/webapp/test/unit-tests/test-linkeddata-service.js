describe('linkedDataService', function() {

    beforeEach(module('won.owner'));

    describe('somePromises', function() {

        beforeEach(inject(function ($injector) {
            $rootScope = $injector.get('$rootScope');
            //$scope = $rootScope.$new();
            //$http = $injector.get('$http');
            $q = $injector.get('$q');
        }));

        it('returns a promise that resolves if in the input promises array ' +
            'the 1st element resolves and the 2nd rejects', inject(function(linkedDataService) {

            // prepare test data
            var getTestPromise = function(withResult) {
                var deferred = $q.defer();
                if (withResult.success) {
                    deferred.resolve(withResult.value);
                } else {
                    deferred.reject(withResult.value);
                }
                return deferred.promise;
           }
            var promises = [getTestPromise({success: true, value: "OK"}), getTestPromise({success: false, value: "not OK"})];

            //test
            var handler = jasmine.createSpy('success');
            linkedDataService.somePromises(promises, function(key, reason) {
                //TODO how to test this? also spy on this function?
            }).then(handler);
            // this is necessary, so that ..
            $rootScope.$digest();
            expect(handler).toHaveBeenCalledWith(["OK", null]);

        }));

        it('returns a promise that resolves if in the input promises array ' +
            'the 1st element rejects and the 2nd resolves', inject(function(linkedDataService) {

            // prepare test data
            var getTestPromise = function(withResult) {
                var deferred = $q.defer();
                if (withResult.success) {
                    deferred.resolve(withResult.value);
                } else {
                    deferred.reject(withResult.value);
                }
                return deferred.promise;
            }
            var promises = [getTestPromise({success: false, value: "not OK"}), getTestPromise({success: true, value: "OK"})];

            //test
            var handler = jasmine.createSpy('success');
            linkedDataService.somePromises(promises, function(key, reason) {
                //TODO how to test this? also spy on this function?
            }).then(handler);
            // this is necessary, so that ..
            $rootScope.$digest();
            expect(handler).toHaveBeenCalledWith([null, "OK"]);

        }));

        it('returns a promise that rejects if in the input promises array ' +
            'all elements reject', inject(function(linkedDataService) {

            // prepare test data
            var getTestPromise = function(withResult) {
                var deferred = $q.defer();
                if (withResult.success) {
                    deferred.resolve(withResult.value);
                } else {
                    deferred.reject(withResult.value);
                }
                return deferred.promise;
            }
            var promises = [getTestPromise({success: false, value: "not OK"}), getTestPromise({success: false, value: "not OK"})];

            //test
           // var handler = jasmine.createSpy('success');
            var handler = jasmine.createSpy('error');
            linkedDataService.somePromises(promises, function(key, reason) {
                //TODO how to test this? also spy on this function?
            }).then(null, handler);
            // this is necessary, so that ..
            $rootScope.$digest();
            expect(handler).toHaveBeenCalledWith([null, null]);

        }));

        //TODO test with provided in input error handler

    });
});

