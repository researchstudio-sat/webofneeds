<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
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

		<script src="scripts/jquery.10.2.js"></script>

		<script src="bower_components/angular/angular.js"></script>
		<script src="bower_components/angular-bootstrap/ui-bootstrap-tpls.js"></script>

		<script type="text/javascript" src="bower_components/angular-ui-utils/modules/event/event.js "></script>
		<script type="text/javascript" src="bower_components/angular-ui-map/src/map.js"></script>
		<script type="text/javascript" src="<c:url value="/bower_components/js-md5/js/md5.js"/>"></script>

		<script type="text/javascript" src="scripts/upload/vendor/jquery.ui.widget.js"></script>
		<script type="text/javascript" src="scripts/upload/jquery.fileupload.js"></script>
		<script type="text/javascript" src="scripts/upload/jquery.iframe-transport.js"></script>
		<script type="text/javascript" src="scripts/upload/jquery.fileupload-process.js"></script>
		<script type="text/javascript" src="scripts/upload/jquery.fileupload-angular.js"></script>

		<script type="text/javascript" src="<c:url value="/app/app.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/service/user-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/map-service.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/service/need-service.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/service/connection-service.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/home/home.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/header/header.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/create-need/create-need.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/need-detail/need-detail.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/need-list/need-list.js"/>"></script>
	</head>
	<body>
		<span ng-init=""></span>
		<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false&callback=onGoogleReady"></script>
		<nav class="navbar navbar-default" role="navigation" ng-controller="HeaderCtrl">
			<div class="container" style="padding:0">
				<div class="collapse navbar-collapse navbar-ex1-collapse" style="padding-left:0;padding-right:15px">
					<ul class="nav navbar-nav">
						<li ng-class="isActive()"><a href="#/">
							<span class="glyphicon glyphicon-transfer"></span>&nbsp;Web Of Needs</a>
						</li>
						<li ng-show="!showPublic()" ng-class="isActive('create-need')" ng-cloak><a href="#/create-need/">
							<span class="glyphicon glyphicon-plus"></span>&nbsp;New	Need</a>
						</li>
						<li ng-show="!showPublic()" ng-class="isActive('need-list')" ng-cloak><a href="#/need-list/">
							<span class="glyphicon glyphicon-globe"></span>&nbsp;My Needs</a>
						</li>
					</ul>
					<ul class="nav navbar-nav navbar-right" ng-cloak>
                        <li><span ng-show="checkRegistered()">{{message}}</span></li>
						<li ng-show="showPublic()" ng-class="isActive('register')"><a href="#/register">Create account</a></li>
						<li ng-show="showPublic()" ng-class="isActive('signin')"><a href="#/signin">Sign in</a></li>
            <li ng-show="!showPublic()"><a href="#/need-list">{{userdata.username}}</a></li>
						<li ng-show="!showPublic()"><a href="" ng-click="onClickSignOut()">Sign out</a></li>
					</ul>
				</div>
			</div>
		</nav>

		<div ng-view id="content" class="container">
		</div>
		
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

