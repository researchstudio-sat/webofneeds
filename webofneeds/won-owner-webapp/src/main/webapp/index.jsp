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
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1"> <!-- see http://getbootstrap.com/css/#overview-mobile -->
    <title ng-bind="'Web Of Needs - '+$root.title">Web Of Needs</title>
    <link rel="stylesheet" href="./generated/won.css" />
</head>
<body>
<section ui-view></section>

<!-- <debug-navigation>
<div style="position: fixed;
                    z-index: 200;
                    bottom: 0px; left: 0px; right: 0px;
                    background-color: lightgray;
                    border-top: 1px solid black;">
    debug navigation: |
    <a ui-sref="landingpage">landingpage</a> |
    <a ui-sref="createNeed({draftId: 0})">create-need</a> |
    <a ui-sref="feed">feed</a> |
    <a ui-sref="settings.general">settings</a> |
    <a ui-sref="overviewMatches">matches</a> |
    <a ui-sref="overviewIncomingRequests">incoming-requests</a> |
    <a ui-sref="overviewPosts">posts</a> |
    <a ui-sref="postVisitor({myUri: 'http://example.org/121337345', theirUri: 'http://example.org/97172311'})">visitor-info</a> |
    <a ui-sref="postVisitorMsgs({myUri: 'http://example.org/121337345', theirUri: 'http://example.org/97172311'})">visitor-convo</a> |
    <a ui-sref="postInfo({myUri: 'http://example.org/121337345'})">owner-info</a> |
    <a ui-sref="postMatches({myUri: 'http://example.org/121337345'})">owner-matches</a> |
    <a ui-sref="postRequests({myUri: 'http://example.org/121337345'})">owner-requests</a> |
    <a ui-sref="postConversations({myUri: 'http://example.org/121337345'})">owner-convos</a>
</div>
<!-- </debug-navigation> -->

<!-- TODO make sure to only show the svgs once angular has finished rendering -->

<!-- start loading the svg so it's already in the cache once angular has finished loading its templates -->
<img src="./generated/icon-sprite.svg" style="display:none">


<script src="./scripts/jquery.10.2.js"></script>
<script src="./scripts/jquery.fs.scroller.min.js"></script>

<script src="./jspm_packages/system.js"></script>
<script src="./jspm_config.js"></script>

<!--
this line loads the bundled app. comment it out if you want
jspm to dynamically load and compile the sources at runtime.
this makes for easier debugging, as you get seperate
source files in the dev-tools' source explorer.-->

<script src="./generated/app_jspm.bundle.js"></script>

<script>
    System.import('./app/app_jspm')
            .catch(console.error.bind(console));
</script>
</body>
</html>

