<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%--
  ~ Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
  ~
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<!DOCTYPE HTML>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<title>Web Of Needs</title>
		<link rel="stylesheet" href="style/bootstrap.min.css" />
		<link rel="stylesheet" href="style/bootstrap.theme.cerulean.css"/>
		<link rel="stylesheet" href="style/main.css"/>
	<link rel="stylesheet" href="style/jquery.fs.scroller.css"/>
	<link rel="stylesheet" href="style/datepicker.css"/>
	<link rel="stylesheet" href="font-awesome-4.1.0/css/font-awesome.min.css">
	<link rel="stylesheet" href="style/lightbox.css"/>
	<link rel="stylesheet" href="style/bootstrap-tagsinput.css"/>
    <link rel="stylesheet" href="style/star-rating.css"/>

		<script src="scripts/jquery.10.2.js"></script>
	<script src="scripts/jquery.fs.scroller.min.js"></script>
		<script src="bower_components/angular/angular.js"></script>
        <script src="bower_components/angular-route/angular-route.js"></script>
        <script src="bower_components/angular-mocks/angular-mocks.js"></script>
		<script src="bower_components/angular-bootstrap/ui-bootstrap-tpls.js"></script>

		<script type="text/javascript" src="bower_components/angular-ui-utils/modules/event/event.js "></script>
		<script type="text/javascript" src="bower_components/angular-ui-map/src/map.js"></script>
		<script type="text/javascript" src="bower_components/js-md5/js/md5.js"></script>
        <script type="text/javascript" src="bower_components/sockjs/sockjs.js"></script>

		<script type="text/javascript" src="scripts/upload/vendor/jquery.ui.widget.js"></script>
		<script type="text/javascript" src="scripts/upload/jquery.fileupload.js"></script>
		<script type="text/javascript" src="scripts/upload/jquery.iframe-transport.js"></script>
		<script type="text/javascript" src="scripts/upload/jquery.fileupload-process.js"></script>
		<script type="text/javascript" src="scripts/upload/jquery.fileupload-angular.js"></script>
        <script type="text/javascript" src="scripts/bootstrap-datepicker.js"></script>
        <script type="text/javascript" src="scripts/lightbox.min.js"></script>
        <script type="text/javascript" src="scripts/jquery.bootpag.min.js"></script>
        <script type="text/javascript" src="scripts/smart-table.min.js"></script>
        <script type="text/javascript" src="scripts/bootstrap-tagsinput.min.js"></script>
        <script type="text/javascript" src="bower_components/jsonld.js/js/jsonld.js"></script>
        <script type="text/javascript" src="scripts/rdfstore-js/rdf_store.js"></script>

        <script type="text/javascript" src="scripts/star-rating.min.js"></script>


		<script type="text/javascript" src="<c:url value="/app/app.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/application-state-service.js"/>"></script>

		<script type="text/javascript" src="<c:url value="/app/service/user-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/map-service.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/service/need-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/util-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/message-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/won-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/linkeddata-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/message-factory-service.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/service/connection-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/builders.js"/>"></script>

		<script type="text/javascript" src="<c:url value="/app/home/home.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/header/header.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/faq/faq.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/impressum/impressum.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/main/main.js"/>"></script>

        <script type="text/javascript" src="<c:url value="/app/create-need/create-need.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/need-detail/need-detail.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/need-list/need-list.js"/>"></script>

	<script type="text/javascript" src="<c:url value="/app/why-use/why-use.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/app/forgot-pwd/forgot-pwd.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/app/forgot-pwd/enter-new-pwd.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/app/postbox/postbox.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/app/private-link/private-link.js"/>"></script>

	<script type="text/javascript" src="<c:url value="/app/search/search.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/app/post-detail/post-detail.js"/>"></script>

	</head>
	<body ng-controller="MainCtrl">
		<span ng-init=""></span>
		<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false&callback=onGoogleReady"></script>
		<nav class="navbar navbar-default" role="navigation" ng-controller="HeaderCtrl">
			<div class="container" style="padding:0">
		<!--<div class="collapse navbar-collapse navbar-ex1-collapse" style="padding-left:0;padding-right:15px">
							<ul class="nav navbar-nav">
								<li ng-class="isActive()"><a href="#">
									<i class="fa fa-arrows-alt fa-lg"></i>&nbsp;WoN</a>
								</li>
							</ul>
							<ul class="nav navbar-nav navbar-right" ng-cloak>
								<li><span ng-show="checkRegistered()">{{message}}</span></li>
								<li ng-show="showPublic()" ng-class="isActive('register')"><a href="#/register">Sign up&nbsp;<span class="caret"/></a></li>
								<li ng-show="showPublic()" ng-class="isActive('signin')"><a href="#/signin">Sign in&nbsp;<span class="caret"/></a></li>
								<!--<li ng-show="!showPublic()"><a href="#/need-list">{{userdata.username}}</a></li>
								<li ng-show="!showPublic()"><a href="" ng-click="onClickSignOut()">Sign out</a></li>     -->

		<!--          <li class="dropdown" ng-show="!showPublic()" ng-cloak>
						  <a class="dropdown-toggle" data-toggle="dropdown" href="">
							  {{userdata.username}}&nbsp;<span class="caret"/>
						  </a>
						  <ul class="dropdown-menu">
							  <li><a href="" ng-click="onClickSignOut()">Sign out</a></li>
						  </ul>
					  </li>
				  </ul>
			  </div>-->


		<!--<nav class="navbar navbar-default" role="navigation" ng-controller="HeaderCtrl">
							<div class="container" style="padding:0">
				<div class="collapse navbar-collapse navbar-ex1-collapse" style="padding-left:0;padding-right:15px">
					<ul class="nav navbar-nav">
						<li ng-class="isActive()"><a href="#/">
											<span class="glyphicon glyphicon-transfer"></span>&nbsp;WoN</a>
						</li>
									</ul>          -->

		<div class="collapse navbar-collapse navbar-ex1-collapse" style="padding-left:0;padding-right:15px">
			<!--<ul class="nav navbar-nav">
						<li ng-show="!showPublic()" ng-class="isActive('create-need')" ng-cloak><a
                                href="#/create-need/1/">
							<span class="glyphicon glyphicon-plus"></span>&nbsp;New	Post</a>
						</li>
						<li ng-show="!showPublic()" ng-class="isActive('need-list')" ng-cloak><a href="#/need-list/">
										<span class="glyphicon glyphicon-globe"></span>&nbsp;My Posts</a>
						</li>
								</ul>       -->
			<ul class="nav navbar-nav">
				<li ng-class="isActive()"><a href="#">
					<i class="fa fa-arrows-alt fa-lg"></i>&nbsp;WoN</a>
				</li>
					</ul>
			<ul class="nav navbar-nav">
				<!--<li ng-show="!showPublic()" ng-class="isActive('create-need')" ng-cloak><a
												href="#/create-need/1/">
											<span class="glyphicon glyphicon-plus-sign"></span>&nbsp;New Post</a>
										</li>    -->
				<li class="dropdown" ng-show="!showPublic()" ng-class="isActive('create-need')" ng-cloak>
					<a href="#" class="dropdown-toggle" data-toggle="dropdown">
						<i class="fa fa-plus-circle fa-lg"></i>&nbsp;New Post
					</a>
					<ul class="dropdown-menu">
						<li><a href="#/create-need/1/0/"><!--<i class="fa fa-circle fa-lg"></i>--><img src="/owner/images/type_posts/want.png"/>&nbsp;I want
							to <strong>have</strong> something</a></li>
						<li class="divider"></li>
						<li><a href="#/create-need/1/1/"><!--<i class="fa fa-circle-o fa-lg"></i>--><img src="/owner/images/type_posts/offer.png"/>&nbsp;I
							<strong>offer</strong> something</a></li>
						<li class="divider"></li>
						<li><a href="#/create-need/1/2/"><!--<i class="fa fa-circle-o-notch fa-lg"></i>--><img src="/owner/images/type_posts/todo.png"/>&nbsp;I
							want to do something <strong>together</strong></a></li>
						<li class="divider"></li>
						<li><a href="#/create-need/1/3/"><!--<i class="fa fa-circle-thin fa-lg"></i>--><img src="/owner/images/type_posts/change.png"/>&nbsp;I
							want to <strong>change</strong> something</a></li>
						<li class="divider"></li>
						<li class="dropdown-submenu">
							<a tabindex="-1" href="#"><i class="fa fa-file-text-o fa-lg"></i>&nbsp;Drafts:&nbsp;Unfinished Posts</a>
							<ul class="dropdown-menu" ng-controller="PostBoxCtrl">
								<li ng-repeat="draft in drafts | orderBy: '-datetime' | limitTo: recordsToDisplay"><a href="#/create-need/1"><i class="fa fa-file-o fa-lg">
									&nbsp;{{draft.title}}</i></a></li>
								<li class="divider"></li>
								<li><a href="#/postbox"><i class="fa fa-list fa-lg"></i>&nbsp;II Others (go to full list)</a></li>
							</ul>
						</li>
					</ul>
				</li>
				<li class="dropdown" ng-show="showPublic()" ng-class="isActive('create-need')" ng-cloak>
					<a href="#" class="dropdown-toggle" data-toggle="dropdown">
						<i class="fa fa-plus-circle fa-lg"></i>&nbsp;New Post
					</a>
					<ul class="dropdown-menu">
						<li><a href="#/create-need/1/0/"><!--<i class="fa fa-circle fa-lg"></i>--><img src="/owner/images/type_posts/want.png"/>&nbsp;I want
							to <strong>have</strong> something</a></li>
						<li class="divider"></li>
						<li><a href="#/create-need/1/1/"><!--<i class="fa fa-circle-o fa-lg"></i>--><img src="/owner/images/type_posts/offer.png"/>&nbsp;I
							<strong>offer</strong> something</a></li>
						<li class="divider"></li>
						<li><a href="#/create-need/1/2/"><!--<i class="fa fa-circle-o-notch fa-lg"></i>--><img src="/owner/images/type_posts/todo.png"/>&nbsp;I
							want to do something <strong>together</strong></a></li>
						<li class="divider"></li>
						<li><a href="#/create-need/1/3/"><!--<i class="fa fa-circle-thin fa-lg"></i>--><img src="/owner/images/type_posts/change.png"/>&nbsp;I
							want to <strong>change</strong> something</a></li>
					</ul>
				</li>
				<li ng-show="!showPublic()" ng-class="isActive('postbox')" ng-cloak><a href="#/postbox/">
					<i class="fa fa-clipboard fa-lg"></i>&nbsp;Post box</a>
				</li>
				<li ng-show="!showPublic()">
					<a href="" class="dropdown-toggle" data-toggle="dropdown" ng-controller="PostBoxCtrl">
						<!-- TODO provide here the total number of messages -->
						<i class="fa fa-comment-o fa-lg"></i>&nbsp;{{21}}
					</a>
					<ul class="dropdown-menu" ng-controller="PostBoxCtrl" style="width: 280px;">
						<li class="text-center grey-item">{{21}}&nbsp;new messages</li>
						<!-- TODO put real parameters into url -->
						<li ng-repeat="conv in conversations"><a href="#/private-link"><img src="{{getTypePicURI(conv.type)}}"/>&nbsp;{{conv.title}}&nbsp;<span
								class="badge pull-right">{{conv.msgs}}</span></a></li>
						<li><a href="#/postbox" class="text-center grey-item">See all&nbsp;<span class="glyphicon glyphicon-new-window"></span></a>
						</li>
					</ul>
				</li>
				<li ng-show="!showPublic()" ng-cloak>
					<a href="" class="dropdown-toggle" data-toggle="dropdown" ng-controller="PostBoxCtrl">
						<!-- TODO provide here the total number of messages -->
						<i class="fa fa-male fa-lg"></i>&nbsp;{{11}}
					</a>
					<ul class="dropdown-menu" ng-controller="PostBoxCtrl" style="width: 280px;">
						<li class="text-center grey-item">{{11}}&nbsp;new messages</li>
						<!-- TODO put real parameters into url -->
						<li ng-repeat="req in requests"><a href="#/private-link"><img
								src="{{getTypePicURI(req.type)}}"/>&nbsp;{{req.title}}&nbsp;<span class="badge pull-right">{{req.msgs}}</span></a>
						</li>
						<li><a href="#/postbox" class="text-center grey-item">See all&nbsp;<span class="glyphicon glyphicon-new-window"></span></a>
						</li>
					</ul>
				</li>
				<li ng-show="!showPublic()" ng-cloak>
					<a href="" class="dropdown-toggle" data-toggle="dropdown" ng-controller="PostBoxCtrl">
						<!-- TODO provide here the total number of messages -->
						<i class="fa fa-puzzle-piece fa-lg"></i>&nbsp;{{countOfAllUnreadMatchEvents}}
					</a>
					<ul class="dropdown-menu" style="width: 280px;">
						<li class="text-center grey-item">{{countOfAllUnreadMatchEvents}}&nbsp;new messages</li>
						<!-- TODO put real parameters into url -->
						<li ng-repeat="need in allNeedsWithUnreadNotifications"><a href="#/private-link"><img
								src="{{getTypePicURI(req.type)}}"/>&nbsp;{{need.title}}&nbsp;<span
                                class="badge pull-right">{{need.matches.length}}</span></a>
						</li>
						<li><a href="#/postbox" class="text-center grey-item">See all&nbsp;<span class="glyphicon glyphicon-new-window"></span></a>
						</li>
					</ul>
				</li>
			</ul>
					<ul class="nav navbar-nav navbar-right" ng-cloak>
                        <li><span ng-show="checkRegistered()">{{message}}</span></li>
				<li ng-show="showPublic()" ng-class="isActive('register')"><a href="#/register">Sign up&nbsp;<span class="caret"/></a></li>
				<li ng-show="showPublic()" ng-class="isActive('signin')"><a href="#/signin">Sign in&nbsp;<span class="caret"/></a></li>
				<!--<li ng-show="!showPublic()"><a href="#/need-list">{{userdata.username}}</a></li>
										<li ng-show="!showPublic()"><a href="" ng-click="onClickSignOut()">Sign out</a></li>     -->

				<li class="dropdown" ng-show="!showPublic()" ng-cloak>
					<a class="dropdown-toggle" data-toggle="dropdown" href="">
						{{userdata.username}}&nbsp;<span class="caret"/>
					</a>
					<ul class="dropdown-menu">
						<li><a href="" ng-click="onClickSignOut()">Sign out</a></li>
					</ul>
				</li>
			</ul>
				</div>
			</div>
		</nav>

		<div ng-view id="content" class="container">
		</div>
		
<nav class="navbar navbar-default" role="navigation">
	<div class="container" style="padding:0">
		<div class="collapse navbar-collapse navbar-ex1-collapse" style="padding-left:0;padding-right:15px">
			<ul class="nav navbar-nav">
				<li><a href="#/why-use">&nbsp;Why use the Web of Needs</a></li>
				<li><a href="#/faq">&nbsp;FAQ</a></li>
				<li><a href="#/impressum">&nbsp;Impressum</a></li>
			</ul>
		</div>
	</div>
</nav>

		<script type="text/javascript">
			window.user = {
				<sec:authorize access="isAuthenticated()">
				username : '<sec:authentication property="principal.username" />',
				isAuth : true
				</sec:authorize>
				<sec:authorize access="!isAuthenticated()">
				isAuth : false
				</sec:authorize>
			};

			function onGoogleReady() {
				angular.bootstrap(document.getElementsByTagName("html")[0], ['won.owner']);
			}
		</script>
	</body>
</html>

