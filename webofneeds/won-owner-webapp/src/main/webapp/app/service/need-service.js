NeeLD = function (needURI, needLD) {
	this.needURI = needURI.replace('data', 'resource');
	this.title = '';
	this.textDescription = '';
	this.tags = []
	this.type = '';

	if (needLD['@graph']) {
		var contentId;
		angular.forEach(needLD['@graph'], function (node) {
			if (node['@id'] == this.needURI) {
				if (node['hasBasicNeedType']) {
					this.type = node.hasBasicNeedType;
				}
				if (node['hasContent']) {
					contentId = node.hasContent;
				}
			}
		}, this);
		if (contentId) {
			angular.forEach(needLD['@graph'], function (node) {
				if (node['@id'] == contentId) {
					this.title = node['dc:title']
					if (node['won:hasTag']) {
						if(angular.isArray(node['won:hasTag'])) {
							this.tags = node['won:hasTag'];
						} else {
							this.tags = [node['won:hasTag']];
						}
					}
					if (node['won:hasTextDescription']) {
						this.textDescription = node['won:hasTextDescription'];
					}
				}
			}, this);
		}
	}
}

CategorizedNeeds = function() {
	this.suggestions = [];
	this.requests = {
		received : [],
		sent : []
	}
	this.conversations = [];
	this.closed = [];

	this.addNeed = function(ldConnectionType, need) {
		switch(ldConnectionType) {
			case 'won:Suggested':
				this.suggestions.push(need);
			break;
			case 'won:RequestSent':
				this.requests.sent.push(need);
			break;
			case 'won:RequestReceived':
				this.requests.received.push(need);
			break;
			case 'won:Connected':
				this.conversations.push(need);
			break;
			case 'won:Closed':
				this.closed.push(need);
			break;
			default:
				throw Exception("FATAL ERROR: unrecognized state");
		}
	}

	this.merge = function(needsCategorized) {
		this.suggestions = this.suggestions.concat(needsCategorized.suggestions);
		this.conversations = this.conversations.concat(needsCategorized.conversations);
		this.requests.received = this.requests.received.concat(needsCategorized.requests.received);
		this.requests.sent = this.requests.sent.concat(needsCategorized.requests.sent);
		this.closed = this.closed.concat(needsCategorized.closed);
	}
}

angular.module('won.owner').factory('needService', function ($http, $q, connectionService) {

	var needService = {};

	needService.getNeedById = function (needId) {
		return $http.get('/owner/rest/needs/' + needId);
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

	var findNeedsToConnections = function (response) {
		var graph = response.data['@graph'];
		if(graph) {
			var remoteNeedsList = [];
			angular.forEach(graph, function (connection) {
				if (connection.hasRemoteNeed) {
					remoteNeedsList.push(needService.getNeedByUri(connection.hasRemoteNeed));
				}
				//connection.hasConnectionState
			});
			var needThrough = function(responses) {
				var needs = {};
				angular.forEach(responses, function (response) {
					if(response.status == 200) {
						var need = new NeeLD(response.config.url, response.data);
						needs[need.uri] = need;
					}
				});
				return needs;
			}
			return $q.allSettled(remoteNeedsList).then(function(response) {
				return needThrough(response);
			},function(response) {
				return needThrough(response);
			}).then(function(needs) {
				var result = new CategorizedNeeds();
				angular.forEach(graph, function (connection) {
					if (connection.hasRemoteNeed && needs[connection.hasRemoteNeed]) {
						result.addNeed(connection.hasConnectionState, needs[connection.hasRemoteNeed]);
					}
				});
				return result;
			});
		}
	}

	needService.getNeedConnections = function (needUri) {
		var dataNeedUri = needUri.replace('resource', 'data');
		return $http({
			method : 'GET',
			url : dataNeedUri + '/connections/',
			params : {
				'deep' : 'true'
			},
			headers : {
				'Accept' : 'application/ld+json'
			}
		}).then(
			findNeedsToConnections,
			function() {
				console.log("FATAL ERROR")
			}
		);
	};

	needService.getNeedByUri = function(needUri) {
		var dataNeedUri = needUri.replace('resource', 'data');
		return $http({
			method : 'GET',
			url : dataNeedUri,
			headers : {
				'Accept':'application/ld+json'
			}
		});
	}

	needService.getAllNeeds = function() {
		return $http.get('/owner/rest/needs/').then(
			function(response) {
				return response.data;
			},
			function() {
				return [];
			}
		);
	}

    needService.saveDraft = function(draft){
        var draftToSave = angular.copy(draft);
        return $http({
            method:'POST',
            url:'/owner/rest/needs/drafts',
            data:JSON.stringify(draftToSave),
            success:function(content){
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
        );
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
			url:'/owner/rest/needs/',
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
		);
	};

	return needService;
});

