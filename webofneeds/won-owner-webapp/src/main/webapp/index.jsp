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
        <meta name="viewport" content="width=device-width, initial-scale=1"> <!-- see http://getbootstrap.com/css/#overview-mobile -->
		<title ng-bind="'Web Of Needs - '+$root.title">Web Of Needs</title>
		<link rel="stylesheet" href="style/bootstrap.min.css" />
		<%--<link rel="stylesheet" href="style/bootstrap.theme.cerulean.css"/>--%>

        <!--<link rel="stylesheet" href="bower_components/bootsketch/build/css/bootsketch.css"/>-->
        <!--<link rel="stylesheet" href="style/bootsketch-patches.css"/>-->
        <link rel="stylesheet" href="bower_components/bootstrap/dist/css/bootstrap-theme.css"/>

	    <link rel="stylesheet" href="style/jquery.fs.scroller.css"/>
	    <link rel="stylesheet" href="style/datepicker.css"/>
	    <link rel="stylesheet" href="bower_components/font-awesome/css/font-awesome.min.css">
	    <link rel="stylesheet" href="style/lightbox.css"/>
	    <link rel="stylesheet" href="style/bootstrap-tagsinput.css"/>
        <link rel="stylesheet" href="style/star-rating.css"/>
        <link rel="stylesheet" href="scripts/angular-scrollable-table/scrollable-table.css"/>
        <link rel="stylesheet" href="bower_components/ng-tags-input/ng-tags-input.css"/>
        <link rel="stylesheet" href="bower_components/ng-tags-input/ng-tags-input.bootstrap.css"/>


        <link rel="stylesheet" href="bower_components/ng-scrollbar/dist/ng-scrollbar.min.css"/>

        <!--leaflet.js provides us with a map-widget which can display map material from different sources-->
        <script type="text/javascript" src="resources/leaflet-0.7.3/leaflet.js"></script>
        <link rel="stylesheet" href="resources/leaflet-0.7.3/leaflet.css"/>

        <!--<link rel="stylesheet" href="style/ui-bootstrap-patches.css"/> <!-- disable after updating ui-bootstrap -->


        <!-- Our legacy CSS'. -->
        <link rel="stylesheet" href="style/main.css"/>
        <!-- The CSS generated from our ./style/won.scss
        Included last so it wins in clashes vs libraries. -->
        <link rel="stylesheet" href="style/generated/won.min.css" />



	</head>
	<body ng-controller="MainCtrl">
		<span ng-init=""></span>
		<nav class="navbar navbar-default" role="navigation" ng-controller="HeaderCtrl">
			<div class="container" style="padding:0">

		<div class="collapse navbar-collapse navbar-ex1-collapse" style="padding-left:0;padding-right:15px">

			<ul class="nav navbar-nav">
				<li ng-class="isActive()"><a href="" ng-click="clickOnWon()">
					<i class="fa fa-arrows-alt fa-lg"></i>&nbsp;WoN</a>
				</li>
                <!--<li class="testfoo">If you see me moving, SCSS is working!</li>-->
			</ul>
			<ul class="nav navbar-nav">
                <li class="dropdown"
                    ng-class="isActive('create-need')"
                    ng-cloak>
					<a href="" class="dropdown-toggle" data-toggle="dropdown">
						<i class="fa fa-plus-circle fa-lg"></i>&nbsp;New Post
					</a>
					<ul class="dropdown-menu">
						<li class="top-layer"><a href="#/create-need/1/0/"><!--<i class="fa fa-circle fa-lg"></i>--><img
                                src="/owner/images/type_posts/want.png"/>&nbsp;I want
							to <strong>have</strong> something</a></li>
						<li class="divider"></li>
						<li class="top-layer"><a href="#/create-need/1/1/"><!--<i class="fa fa-circle-o fa-lg"></i>--><img
                                src="/owner/images/type_posts/offer.png"/>&nbsp;I
							<strong>offer</strong> something</a></li>
						<li class="divider"></li>
						<li class="top-layer"><a href="#/create-need/1/2/"><!--<i class="fa fa-circle-o-notch fa-lg"></i>--><img
                                src="/owner/images/type_posts/todo.png"/>&nbsp;I
							want to do something <strong>together</strong></a></li>
						<li class="divider"></li>
						<li class="top-layer"><a href="#/create-need/1/3/"><!--<i class="fa fa-circle-thin fa-lg"></i>--><img
                                src="/owner/images/type_posts/change.png"/>&nbsp;I
							want to <strong>change</strong> something</a></li>
						<li class="divider" ng-show="!showPublic()"></li>
						<li class="dropdown-submenu top-layer"ng-show="!showPublic()">
							<a tabindex="-1" href="#"><i class="fa fa-file-text-o fa-lg"></i>&nbsp;Drafts:&nbsp;Unfinished Posts</a>
							<ul class="dropdown-menu" ng-controller="DraftCtrl"  >
								<li
                                        ng-repeat="draft in allDrafts | orderObjectBy:'meta.sentTimestamp':true | limitTo: recordsToDisplay">
                                    <a href ng-click="clickOn(draft.uri)">
                                    <span style="display: none;">{{draft.basicNeedType}}</span>
                                    <img ng-src="{{getTypePicURI(draft.basicNeedType)}}"/>
                                     &nbsp;
                                     <span ng-bind="draft.title"> &nbsp;</span>
                                    </a>
                                </li>
								<li class="divider"></li>
								<li><a href="#/postbox"><i class="fa fa-list fa-lg"></i>&nbsp;II Others (go to full list)</a></li>
							</ul>
						</li>
					</ul>
				</li>



				<li ng-show="showAccountUser()" ng-class="isActive('postbox')" ng-cloak><a href="#/postbox/">
					<i class="fa fa-clipboard fa-lg"></i>&nbsp;Post box</a>
				</li>

                <li ng-show="showAccountUser()" ng-cloak notif-dropdown
                    event-type = "message"
                    unread-events-by-type-by-need="unreadEventsByTypeByNeed"
                    unread-events-by-need-by-type="unreadEventsByNeedByType"
                    on-click="openNeedDetailView(needURI)"
                    get-type-pic-uri = "getTypePicURI(type)">
                </li>
                <li ng-show="showAccountUser()" ng-cloak notif-dropdown
                    event-type = "connect"
                    unread-events-by-type-by-need="unreadEventsByTypeByNeed"
                    unread-events-by-need-by-type="unreadEventsByNeedByType"
                    on-click="openNeedDetailView(needURI)"
                    get-type-pic-uri = "getTypePicURI(type)">
                </li>
                <li ng-show="showAccountUser()" ng-cloak notif-dropdown
                    event-type = "hint"
                    unread-events-by-type-by-need="unreadEventsByTypeByNeed"
                    unread-events-by-need-by-type="unreadEventsByNeedByType"
                    on-click="openNeedDetailView(needURI)"
                    get-type-pic-uri = "getTypePicURI(type)">
                </li>
			</ul>
					<ul class="nav navbar-nav navbar-right" ng-cloak>
                        <li><span ng-show="checkRegistered()"></span></li>

                        <li ng-show="showPublic()" ng-class="isActive('register')">
                            <a ng-click="displaySignUp()">
                                Sign Up&nbsp;<span class="caret"/>
                            </a>
                        </li>
                        <li ng-show="showPublic()" ng-class="isActive('signin')">
                            <a ng-click="displaySignIn()">
                                Sign In&nbsp;<span class="caret"/>
                            </a>
                        </li>
				<!--<li ng-show="!showPublic()"><a href="#/need-list">{{userdata.username}}</a></li>
										<li ng-show="!showPublic()"><a href="" ng-click="onClickSignOut()">Sign out</a></li>     -->

				<li class="dropdown" ng-show="!showPublic()" ng-cloak>
					<a class="dropdown-toggle" data-toggle="dropdown" href="">
						{{userdata.username}}&nbsp;<span class="caret"/>
					</a>
					<ul class="dropdown-menu">
                        <li><a href="" ng-click="goLandingPage()">What can I do with the Web of Needs?</a></li>
						<li><a href="" ng-click="onClickSignOut()">Sign out</a></li>
					</ul>
				</li>
			</ul>
				</div>
			</div>
		</nav>

		<div ng-view id="content" class="container">
		</div>

		<!--actually there's a footer html tag for this use-case-->
		<!--also we should get rid of these nested divs--->
<nav class="navbar navbar-default navbar-bottom" role="navigation">
    <div class="container" style="padding:0">
        <!--<div class="collapse navbar-collapse navbar-ex1-collapse" style="padding-left:0;padding-right:15px">-->
        <ul class="nav navbar-nav">
            <li><a href="#/why-use">&nbsp;Why use the Web of Needs</a></li>
            <li><a href="#/faq">&nbsp;FAQ</a></li>
            <li><a href="#/impressum">&nbsp;Impressum</a></li>
        </ul>
    </div>
</nav>


        <script src="scripts/jquery.10.2.js"></script>
        <script src="scripts/jquery.fs.scroller.min.js"></script>
        <script src="bower_components/angular/angular.js"></script>
        <script src="bower_components/bootstrap/dist/js/bootstrap.js"></script>
        <script src="bower_components/angular-route/angular-route.js"></script>
        <script src="bower_components/angular-mocks/angular-mocks.js"></script>

        <script src="bower_components/angular-bootstrap/ui-bootstrap-tpls.js"></script>

        <%--
        <script type="text/javascript" src="bower_components/angular-ui-utils/ui-utils.js"></script> <!-- to include all ui-utils TODO deleteme-->
        --%>
        <script type="text/javascript" src="bower_components/ng-tags-input/ng-tags-input.js"></script>
        <script type="text/javascript" src="bower_components/js-md5/js/md5.js"></script>
        <script type="text/javascript" src="bower_components/sockjs/sockjs.js"></script>
        <script type="text/javascript" src="bower_components/ng-scrollbar/src/ng-scrollbar.js"></script>
        <script type="text/javascript" src="bower_components/zeroclipboard/dist/ZeroClipboard.js"></script>
        <script type="text/javascript" src="bower_components/ng-clip/src/ngClip.js"></script>

        <script type="text/javascript" src="scripts/upload/vendor/jquery.ui.widget.js"></script>
        <script type="text/javascript" src="scripts/upload/jquery.fileupload.js"></script>
        <script type="text/javascript" src="scripts/upload/jquery.iframe-transport.js"></script>
        <script type="text/javascript" src="scripts/upload/jquery.fileupload-process.js"></script>
        <script type="text/javascript" src="scripts/upload/jquery.fileupload-angular.js"></script>
        <%--<script type="text/javascript" src="scripts/osm/angular-leaflet-directive.min.js"></script>--%>
        <script type="text/javascript" src="scripts/bootstrap-datepicker.js"></script>
        <script type="text/javascript" src="scripts/lightbox.min.js"></script>
        <script type="text/javascript" src="scripts/jquery.bootpag.min.js"></script>
        <script type="text/javascript" src="scripts/smart-table.min.js"></script>
        <script type="text/javascript" src="scripts/bootstrap-tagsinput.min.js"></script>
        <script type="text/javascript" src="scripts/jsonld.js"></script>
        <script type="text/javascript" src="scripts/rdfstore-js/rdf_store.js"></script>
        <script type="text/javascript" src="scripts/angular-scrollable-table/angular-scrollable-table.js"></script>

        <script type="text/javascript" src="scripts/star-rating.min.js"></script>


        <script src="jspm_packages/system.js"></script>
        <script src="jspm_config.js"></script>
        <!--
        <script>
            //System.import('app/app')
            System.import('app/jspm_test')
                    .catch(console.error.bind(console));
        </script>
        -->

        <!--
        TODO app.js should be included via jspm/system.js
        -->
        <script type="text/javascript" src="<c:url value="/app/app.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/won.js"/>"></script>

        <script type="text/javascript" src="<c:url value="/app/service/application-state-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/application-control-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/user-service.js"/>"></script>
        <%--<script type="text/javascript" src="<c:url value="/app/service/map-service.js"/>"></script>--%>
        <script type="text/javascript" src="<c:url value="/app/service/osm-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/need-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/util-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/message-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/search-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/won-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/linkeddata-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/service/message-factory-service.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/sign-in/sign-in.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/sign-in/sign-up.js"/>"></script>


        <script type="text/javascript" src="<c:url value="/app/home/home.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/header/header.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/header/notification-dropdown.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/faq/faq.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/impressum/impressum.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/main/main.js"/>"></script>

        <script type="text/javascript" src="<c:url value="/app/create-need/create-need.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/create-need/location-selector.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/create-need/image-uploader.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/conversation/conversation.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/draft/draft.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/need-list/need-list.js"/>"></script>

        <script type="text/javascript" src="<c:url value="/app/why-use/why-use.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/forgot-pwd/forgot-pwd.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/forgot-pwd/enter-new-pwd.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/postbox/postbox.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/private-link/private-link.js"/>"></script>

        <script type="text/javascript" src="<c:url value="/app/search/search.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/app/post-detail/post-detail.js"/>"></script>

        <script type="text/javascript">
            window.user = {
                <sec:authorize access="isAuthenticated()">
                username : '<sec:authentication property="principal.username" />',
                authorities : '<sec:authentication property="principal.authorities" />',
                isAuth : true
                </sec:authorize>
                <sec:authorize access="!isAuthenticated()">
                isAuth : false
                </sec:authorize>
            };

            angular.bootstrap(document.getElementsByTagName("html")[0], ['won.owner']);
            /*
             angular.bootstrap(document, ['won.owner'], {
             // make sure dependency injection works after minification
             // see https://docs.angularjs.org/guide/production
             // and https://docs.angularjs.org/guide/di#dependency-annotation
             //strictDi: true //TODO
             });*/
        </script>
        <!--<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false&callback=onGoogleReady"></script>-->
	</body>
</html>

