/**
 * Created by ksinger on 11.08.2016.
 */

import "leaflet";

import icon from "leaflet/dist/images/marker-icon.png";
import icon2x from "leaflet/dist/images/marker-icon-2x.png";
import shadow from "leaflet/dist/images/marker-shadow.png";

/* global L */
L.Icon.Default.prototype._getIconUrl = name => {
  if (L.Browser.retina && name === "icon") {
    name += "-2x";
  }
  if (name == "icon") {
    if (L.Browser.retina) {
      return icon2x;
    } else {
      return icon;
    }
  } else {
    return shadow;
  }
};

export default L;
