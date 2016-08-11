/**
 * Created by ksinger on 11.08.2016.
 */



export function initLeaflet(mapMount) {
    if(!L) {
        throw new Exception("Tried to initialize a leaflet widget while leaflet wasn't loaded.");
    }

    // Leaflet + JS-Bundling fix:
    L.Icon.Default.imagePath = 'images/map-images/';
    //TODO replace with own icons

    const secureOsmSource = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png' // secure osm.org
    const secureOsm = L.tileLayer(secureOsmSource, {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
    });

    const transportSource = 'http://{s}.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png';
    const transport = L.tileLayer(transportSource, {
        attribution: 'Maps &copy; <a href="http://www.thunderforest.com">Thunderforest</a>, Data &copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
    });

    const map = L.map(mapMount,{
        center: [37.44, -42.89], //centered on north-west africa
        zoom: 1, //world-map
        layers: [secureOsm], //initially visible layers

    }); //.setView([51.505, -0.09], 13);

    //map.fitWorld() // shows every continent twice :|
    map.fitBounds([[-80, -190],[80, 190]]); // fitWorld without repetition

    const baseMaps = {
        "Detailed default map": secureOsm,
        "Transport (Insecurely loaded!)": transport,
    };

    L.control.layers(baseMaps).addTo(map);

    // Force it to adapt to actual size
    // for some reason this doesn't happen by default
    // when the map is within a tag.
    // this.map.invalidateSize();
    // ^ doesn't work (needs to be done manually atm);

    return map;
}
