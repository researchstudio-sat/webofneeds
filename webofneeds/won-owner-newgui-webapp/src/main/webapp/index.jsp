<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>

<!DOCTYPE HTML>
<html>
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<title>Web Of Needs</title>
		<link rel="stylesheet" href="style/bootstrap.min.css" />
<%--
		<link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap-theme.min.css" />
--%>
		<link rel="stylesheet" href="style/bootstrap.theme.cerulean.css"/>

		<script src="jquery/jquery..10.2.js"></script>

		<script src="bower_components/angular/angular.js"></script>
		<script src="bower_components/angular-bootstrap/ui-bootstrap-tpls.js"></script>

		<script type="text/javascript" src="bower_components/angular-ui-utils/modules/event/event.js "></script>
		<script type="text/javascript" src="bower_components/angular-ui-map/src/map.js"></script>

		<script type="text/javascript" src="<c:url value="/app/service/need-service.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/service/connection-service.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/home/home.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/header/header.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/create-need/create-need.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/need-detail/need-detail.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/need-list/need-list.js"/>"></script>
		<script type="text/javascript" src="<c:url value="/app/app.js"/>"></script>


		<style type="text/css">
			.navbar {
				border-radius: 0;
				margin-bottom: 0;
			}
			#content {
				background-color: #F5F5F5;
			}

			.map-canvas {
				width: 405px;
				height: 235px;
			}

			.btnTag:hover {
				background-color: black;
			}

			.need-panel-body:hover {
				cursor: pointer;
				background-color: #F1F1F1;
			}

			.value-panel {
				padding-top:9px;
			}
		</style>
		<script type="text/javascript">
			function onGoogleReady() {
				angular.bootstrap(document.getElementsByTagName("html")[0], ['owner']);
			}
		</script>
	</head>
	<body>
		<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false&callback=onGoogleReady"></script>
		<nav class="navbar navbar-default" role="navigation" ng-controller="HeaderCtrl">
			<div class="container">
				<div class="collapse navbar-collapse navbar-ex1-collapse">
					<ul class="nav navbar-nav">
						<li ng-class="isActive()"><a href="#/">
							<span class="glyphicon glyphicon-transfer"></span>&nbsp;Web Of Needs</a>
						</li>
						<li	ng-class="isActive('create-need')"><a href="#/create-need/">
							<span class="glyphicon glyphicon-plus"></span>&nbsp;New	Need</a>
						</li>
						<li ng-class="isActive('need-list')"><a href="#/need-list/">
							<span class="glyphicon glyphicon-globe"></span>&nbsp;All Needs</a>
						</li>
					</ul>
				</div>
			</div>
		</nav>

		<div ng-view id="content" class="container">
		</div>
	</body>
</html>

