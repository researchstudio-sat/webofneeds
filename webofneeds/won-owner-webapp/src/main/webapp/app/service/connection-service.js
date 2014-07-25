angular.module('won.owner').factory('connectionService', function ($http) {

	var connectionService = {};

	connectionService.STATE_REQUEST = 'manageConnection'
	connectionService.STATE_CLOSED = 'showMessageClosed'
	connectionService.STATE_CHAT = 'listMessages'
	connectionService.STATE_PENDING = 'showMessagePending'

	connectionService.getConnectionState = function (connectionId) {
		return $http.get('/owner/rest/connection/' + connectionId + '/state').then(function(result) {
			if(result.data.indexOf(connectionService.STATE_CLOSED) > -1) {
				return {
					state :connectionService.STATE_CLOSED,
					message :result.data.split('-')[1]
				}
			} else if(result.data.indexOf(connectionService.STATE_PENDING) > -1) {
				return {
					state:connectionService.STATE_PENDING,
					message:result.data.split('-')[1]
				}
			} else if (result.data.indexOf(connectionService.STATE_REQUEST) > -1) {
				return {
					state:connectionService.STATE_REQUEST
				}
			} else if (result.data.indexOf(connectionService.STATE_CHAT) > -1) {
				return {
					state:connectionService.STATE_CHAT
				}
			} else {
				console.log(result)
			}
		});
	};

	connectionService.getConnectionMessages = function (connectionId) {
		return $http.get('/owner/rest/connection/' + connectionId + '/body');
	};

	connectionService.sendConnectionAccept= function (connectionId) {
		return $http.get('/owner/rest/connection/' + connectionId + '/accept');
	};

	connectionService.sendConnectionDeny = function (connectionId) {
		return $http.get('/owner/rest/connection/' + connectionId + '/deny');
	};

	connectionService.sendConnectionClose = function (connectionId) {
		return $http.get('/owner/rest/connection/' + connectionId + '/close');
	};

	connectionService.sendConnectionMessage = function (connectionId, textMessage) {
		return $http({
			method:'POST',
			url:'http://localhost:8080/owner/rest/connection/' + connectionId + '/send',
			data:textMessage
		})
	};

	return connectionService;
});

