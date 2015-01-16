/**
 *
 *
 * Created by ksinger on 16.01.2015.
 */



angular.module('won.owner').factory('osmService', function () {
    var osm = {}
    //TODO rewrite to use futures
    osm.getMostLikelyLocation = function (locationStr, handler) {
        this.getLocations(locationStr, function(response) {return handler(response[0]) })
    }
    /* If address is a string it will be taken as is and queried with.
    If it's an object of the form of

    {street="...",
     city="...",
     county="...",
     state="...",
     country="...",
     postalcode="..."}

     a more refined query will be used. Any of these fields can be undefined
     if you don't want to include them in the query

     */
    osm.matchingLocations = function (address, handler) {
        var req = new XMLHttpRequest();
        req.onload = function(){
            handler(JSON.parse(this.responseText))
        };
        //for the nominatim docu see here: http://wiki.openstreetmap.org/wiki/Nominatim
        //adding &polygon_svg=1 to the request would give us the bounding polygon
        if(typeof address === 'string') {
            req.open("GET", "http://nominatim.openstreetmap.org/search/" + address + "?format=json&addressdetails=1", true);
            req.send()
        } else {
            adrStr = ""

            if (addressObj.street != undefined)
                adrStr + "street=" + addressObj.street //<housenumber> <streetname>
            if (addressObj.city != undefined)
                adrStr + "city=" + addressObj.city //<city>
            if (addressObj.county != undefined)
                adrStr + "county=" + addressObj.county //<county>
            if (addressObj.state != undefined)
                adrStr + "state=" + addressObj.state //<state>
            if (addressObj.country != undefined)
                adrStr + "country=" + addressObj.country //<country>
            if (addressObj.postalcode != undefined)
                adrStr + "postalcode=" + addressObj.postalcode //<postalcode>

            if (adrStr != "") {
                req.open("GET", "http://nominatim.openstreetmap.org/search/" + adrStr + "?format=json&addressdetails=1", true);
                req.send();
            }
        }
    }
    return osm;
})
    /*osm.geocodeResponse = function () {
        var geocodeResponse = JSON.parse(this.responseText);
        if (geocodeResponse.length <= 0) {
            return;
        }
        geocodeResponse = geocodeResponse[0];

        var lctn = [parseFloat(geocodeResponse.lat),
            parseFloat(geocodeResponse.lon)];
        console.log(geocodeResponse);
        console.log(geocodeResponse.lat);
        console.log(geocodeResponse.lon);
        console.log(lctn);

        // create a map in the "map" div, set the view to a given place and zoom
        map.setView(lctn, 13);

        // add an OpenStreetMap tile layer
        L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        // add a marker in the given location, attach some popup content to it and open the popup
        L.marker(lctn).addTo(map)
            .bindPopup(geocodeResponse.display_name + "<br> and more fancy html")
            .openPopup();
    }

    osm.initializeMap = function () {
        map = L.map('map-canvas');
        this.getInitialLocation();

    }*/
