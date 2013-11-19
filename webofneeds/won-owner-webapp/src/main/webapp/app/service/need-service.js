needServiceModule = angular.module('owner.service.need', []);

needServiceModule.factory('needService', function ($http, $q, connectionService) {

	var needService = {};

	needService.getNeedById = function (needId) {
		return $http.get('/owner/rest/need/' + needId);
	};

	needService.getIdFromUri = function(needUri) {
		return needUri.substr(needUri.lastIndexOf("/"), needUri.length);
	}

	needService.getNeedMatches = function (mainNeedId) {
		return $q.all([
			$http.get('/owner/rest/' + mainNeedId + '/matches'),
			this.getNeedConnections(mainNeedId)
		]).then(function(results) {
			var getIdFromUri = function (needUri) {
				return needUri.substr(needUri.lastIndexOf("/") + 1, needUri.length);
			}

			var matches = results[0].data;
			var connections = results[1].data;
			angular.forEach(matches, function(match) {
				angular.forEach(connections, function (connection) {
					var needIdFrom = getIdFromUri(connection.needURI);
					var needIdTo = getIdFromUri(connection.remoteNeedURI);
					if((match.needURI == connection.needURI || match.needURI == connection.remoteNeedURI) && (mainNeedId == needIdFrom || mainNeedId == needIdTo)) {
						match.hasConnection = true;
						match.connectionId = connection.id;
						connectionService.getConnectionState(connection.id).then(function(result) {
							match.connectionState = result;
						});
					}
				}, this);
			}, this);
			return matches;
		}).then(function(matches) {
			return matches;
		});
	};

	needService.getNeedConnections = function (needId) {
		return $http.get('/owner/rest/' + needId + '/listConnections');
	};

	needService.getAllNeeds = function() {
		return $http.get('/owner/rest/');
	}

	needService.save = function(need) {
		var needToSave = angular.copy(need);
		needToSave.tags = need.tags.join(",");
		if(needToSave.startTimeHour && needToSave.startTimeMinute) {
			needToSave.startTime = needToSave.startTime + " " + needToSave.startTimeHour + ":" + needToSave.startTimeMinute + ": 00";
		}
		delete needToSave.startTimeHour;
		delete needToSave.startTimeMinute;
		if (needToSave.endTimeHour && needToSave.endTimeMinute) {
			needToSave.endTime = needToSave.endTime + " " + needToSave.endTimeHour + ":" + needToSave.endTimeMinute + ": 00";
		}
		delete needToSave.endTimeHour;
		delete needToSave.endTimeMinute;
		delete needToSave.binaryFolder;
		return $http({
			method:'POST',
			url:'/owner/rest/create',
			data:needToSave,
			success:function (content) {
				console.log(content);
			}
		}).then(
				function () {
					// success
					return {status:"OK"};
				},
				function (response) {
					console.log("FATAL ERROR");
				}
		);;
	};

	return needService;
});

