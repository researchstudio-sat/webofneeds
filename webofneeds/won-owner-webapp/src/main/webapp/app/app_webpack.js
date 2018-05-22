import "babel-polyfill";

import "./app_jspm";

/* global require */
require.context("../images/won-icons", false, /\.svg$/);

import "../style/won.scss";
