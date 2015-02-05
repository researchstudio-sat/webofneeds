/**
 * Created by ksinger on 05.02.2015.
 */
;

angular.module('won.owner')
    .directive('locationSelector', function factory($log, osmService) {
        return {
            restrict: 'AE',
            //template: 'hello location selector',
            templateUrl : 'app/create-need/location-selector.html',
            scope : {
                /*chosenMessage: '=',
                clickOnPostLink: '&'*/
            },

            //link: function(scope, element, attrs){
            link: function(scope, element, attrs, $event){
                // TODO for some reason link get's called twice, the first time without html (the query-selector below is empty)
                $log.debug("Found " + $('#leaflet-canvas').length + " canvas(es)");
                var canvas = $("#leaflet-canvas");
                if(canvas.length == 0) {
                    return; // no canvas, no fun (and no point for the directive)
                } else {
                    canvas = canvas[0];
                }

                var map;

                // -------- snippet from leafletjs.com ----------
                // create a map in the "leaflet-canvas" div, set the view to a given place and zoom
                map = L.map(canvas);
                //map = L.map('leaflet-canvas');
                map.fitWorld().zoomIn(); // zoom=0 isn't rectangular (-> gray letterboxing) -> zoomIn to fix this

                // add an OpenStreetMap tile layer with attributions
                L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
                    attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                }).addTo(map);

                map.removeCstmMarker = function() {
                    if(map.cstmMarker != undefined) {
                        map.removeLayer(map.cstmMarker); // remove the previous marker //TODO does this delete the popup as well?
                    }
                };

                scope.submitAddressQuery = function (address) {
                    //clear old results
                    scope.addressSearchResults = undefined;
                    scope.selectedAddress = {}
                    map.removeCstmMarker();
                    //map.fitWorld().zoomIn(); // TODO a good idea ux-wise? or just stay at the previous location?

                    if(address && address !== "" && address !== {}) {
                        $("#locationForm").addClass('open'); //show drop-down //TODO hackish
                        osmService.matchingLocations(address).then(function(resp){
                            scope.addressSearchResults = resp;
                        }, function failed(){
                            $log.error("Address resolution failed.");
                        });
                    }
                }

                scope.selectedAddress = {};
                scope.selectAddress = function (address) {
                    $("#locationForm").removeClass('open'); //hide drop-down //TODO hackish
                    scope.selectedAddress = address;
                    scope.setMapLocation(address.lat, address.lon, address.display_name);
                    scope.addressText = address.display_name;
                };
                scope.setMapLocation = function (lat, lon, adr) { //TODO not in $scope but only usable here in link?
                    map.removeCstmMarker();

                    map.cstmMarker = L.marker([lat, lon]);
                    map.cstmMarker.addTo(map).bindPopup(adr);

                    //TODO base zoomlevel (L.latLng(lat, lon, alt(!))) on size of the selected area
                    map.setView([lat, lon], 13);

                    map.cstmMarker.openPopup();
                };
                //TODO start searching as soon as the user pauses/presses down, (followed by: select choice, press enter)
                //TODO enter selects the first entry? shows an error popup and asks to select a correct location (same on focus loss)? enter jumps to first line of dropdown?
                scope.onArrowDownInSearchField = function (event) { //TODO DELETEME
                    // TODO only go into list if there are search results
                    console.log("In onArrowDownInSearchField. Event: " + JSON.stringify(event));
                    scope.locationResultsVisible = true; //doesn't work(?)
                    //$('#locationDropDownToggle').dropdown();
                    console.log($('#locationDropDownToggle'));
                    console.log($('.dropDownToggle'));
                    $("#locationForm").addClass('open'); //TODO not very stable (e.g. if class name changes)
                };
                scope.onArrowUpInSearchField = function (event) { //TODO DELETEME
                    console.log("In onArrowUpInSearchField. Event: " + JSON.stringify(event));
                    $("#locationForm").removeClass('open'); //TODO not very stable (e.g. if class name changes)
                };
                scope.isopen = true;
                scope.toggleDropdown = function($event) {
                    console.log("in toggleDropdown. isopen = " + scope.isopen);
                    $event.preventDefault();
                    $event.stopPropagation();
                    scope.isopen = !scope.isopen;
                };
                scope.onLocationDropdownToggle = function() {
                    console.log("Toggled dropdown.");
                };

                (function() { // <anonymous wrapper>
                    //setup before functions
                    var typingTimer;                //timer identifier
                    var doneTypingInterval = 1300;  //time in ms

                    //on keyup, start the countdown
                    $('#addressTextField').keyup(function(){
                        clearTimeout(typingTimer);
                        typingTimer = setTimeout(doneTyping, doneTypingInterval);
                    });

                    //on keydown, clear the countdown
                    $('#addressTextField').keydown(function(){
                        clearTimeout(typingTimer);
                    });

                    //user is "finished typing," do something
                    function doneTyping () {
                        var address = $('#addressTextField').val();
                        //alert(encodeURIComponent(address));
                        scope.submitAddressQuery(address);
                    }
                })(); // </anonymous wrapper>
            }
        };
    });
