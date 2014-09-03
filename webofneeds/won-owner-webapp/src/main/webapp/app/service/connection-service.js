/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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

