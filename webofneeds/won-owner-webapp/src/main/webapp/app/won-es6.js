/**
 * Created by ksinger on 03.12.2015.
 *
 * This is a simple reexport of won.js, so other es6-files can
 * import it via the module system. Once all scripts accessing
 * won.js directly have been removed from the codebase, it can
 * be merged into this script (the easiest by `
 * default export`ing the won-object)
 */

import won from './service/won';
import './service/need-builder';
import './service/message-builder';
import './service/linkeddata-service-won';
export default won;




