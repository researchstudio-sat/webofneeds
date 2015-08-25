System.config({
  "baseURL": "/owner/#/",
  "transpiler": "babel",
  "babelOptions": {
    "optional": [
      "runtime"
    ]
  },
  "paths": {
    "*": "*.js",
    "github:*": "jspm_packages/github/*.js",
    "npm:*": "jspm_packages/npm/*.js",
    "bower:*": "jspm_packages/bower/*.js"
  }
});

System.config({
  "map": {
    "angular": "github:angular/bower-angular@1.4.2",
    "angular-bootstrap": "github:angular-ui/bootstrap-bower@0.13.0",
    "angular-mocks": "github:angular/bower-angular-mocks@1.4.2",
    "angular-new-router": "npm:angular-new-router@0.5.3",
    "angular-route": "github:angular/bower-angular-route@1.4.2",
    "angular-ui-utils": "github:angular-ui/ui-utils@2.0.0",
    "babel": "npm:babel-core@5.6.15",
    "babel-runtime": "npm:babel-runtime@5.6.15",
    "bootstrap": "github:twbs/bootstrap@3.3.5",
    "core-js": "npm:core-js@0.9.18",
    "font-awesome": "bower:font-awesome@4.3.0",
    "jquery": "github:components/jquery@2.1.4",
    "js-md5": "bower:js-md5@1.1.0",
    "ng-clip": "bower:ng-clip@0.2.6",
    "ng-scrollbar": "bower:ng-scrollbar@0.0.6",
    "ng-tags-input": "bower:ng-tags-input@2.3.0",
    "sockjs": "bower:sockjs@0.3.4",
    "bower:font-awesome@4.3.0": {
      "css": "github:systemjs/plugin-css@0.1.13"
    },
    "bower:ng-clip@0.2.6": {
      "angular": "bower:angular@1.4.2",
      "zeroclipboard": "bower:zeroclipboard@2.2.0"
    },
    "bower:ng-scrollbar@0.0.6": {
      "css": "github:systemjs/plugin-css@0.1.13"
    },
    "bower:ng-tags-input@2.3.0": {
      "angular": "bower:angular@1.4.2",
      "css": "github:systemjs/plugin-css@0.1.13"
    },
    "github:angular/bower-angular-mocks@1.4.2": {
      "angular": "github:angular/bower-angular@1.4.2"
    },
    "github:angular/bower-angular-route@1.4.2": {
      "angular": "github:angular/bower-angular@1.4.2"
    },
    "github:jspm/nodelibs-assert@0.1.0": {
      "assert": "npm:assert@1.3.0"
    },
    "github:jspm/nodelibs-buffer@0.1.0": {
      "buffer": "npm:buffer@3.4.3"
    },
    "github:jspm/nodelibs-events@0.1.1": {
      "events": "npm:events@1.0.2"
    },
    "github:jspm/nodelibs-path@0.1.0": {
      "path-browserify": "npm:path-browserify@0.0.0"
    },
    "github:jspm/nodelibs-process@0.1.1": {
      "process": "npm:process@0.10.1"
    },
    "github:jspm/nodelibs-util@0.1.0": {
      "util": "npm:util@0.10.3"
    },
    "github:jspm/nodelibs-vm@0.1.0": {
      "vm-browserify": "npm:vm-browserify@0.0.4"
    },
    "github:twbs/bootstrap@3.3.5": {
      "jquery": "github:components/jquery@2.1.4"
    },
    "npm:amdefine@1.0.0": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "module": "github:jspm/nodelibs-module@0.1.0",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:angular-new-router@0.5.3": {
      "child_process": "github:jspm/nodelibs-child_process@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "requirejs": "npm:requirejs@2.1.20",
      "route-recognizer": "github:btford/route-recognizer@0.1.1",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0",
      "traceur": "npm:traceur@0.0.72"
    },
    "npm:assert@1.3.0": {
      "util": "npm:util@0.10.3"
    },
    "npm:ast-types@0.5.7": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "depd": "npm:depd@1.0.1",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:babel-runtime@5.6.15": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:brace-expansion@1.1.0": {
      "balanced-match": "npm:balanced-match@0.2.0",
      "concat-map": "npm:concat-map@0.0.1"
    },
    "npm:buffer@3.4.3": {
      "base64-js": "npm:base64-js@0.0.8",
      "ieee754": "npm:ieee754@1.1.6",
      "is-array": "npm:is-array@1.0.1"
    },
    "npm:commander@2.8.1": {
      "child_process": "github:jspm/nodelibs-child_process@0.1.0",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "graceful-readlink": "npm:graceful-readlink@1.0.1",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:core-js@0.9.18": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:depd@1.0.1": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:esprima-fb@7001.1.0-dev-harmony-fb": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:glob@4.5.3": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "inflight": "npm:inflight@1.0.4",
      "inherits": "npm:inherits@2.0.1",
      "minimatch": "npm:minimatch@2.0.10",
      "once": "npm:once@1.3.2",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:graceful-readlink@1.0.1": {
      "fs": "github:jspm/nodelibs-fs@0.1.2"
    },
    "npm:inflight@1.0.4": {
      "once": "npm:once@1.3.2",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "wrappy": "npm:wrappy@1.0.1"
    },
    "npm:inherits@2.0.1": {
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:minimatch@2.0.10": {
      "brace-expansion": "npm:brace-expansion@1.1.0",
      "path": "github:jspm/nodelibs-path@0.1.0"
    },
    "npm:once@1.3.2": {
      "wrappy": "npm:wrappy@1.0.1"
    },
    "npm:path-browserify@0.0.0": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:recast@0.8.8": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "ast-types": "npm:ast-types@0.5.7",
      "cls": "npm:cls@0.1.5",
      "depd": "npm:depd@1.0.1",
      "esprima-fb": "npm:esprima-fb@7001.1.0-dev-harmony-fb",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "private": "npm:private@0.1.6",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "source-map": "npm:source-map@0.1.32"
    },
    "npm:regexpu@0.3.0": {
      "process": "github:jspm/nodelibs-process@0.1.1",
      "recast": "npm:recast@0.8.8",
      "regenerate": "npm:regenerate@1.2.1",
      "regjsgen": "npm:regjsgen@0.2.0",
      "regjsparser": "npm:regjsparser@0.1.5",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:regjsparser@0.1.5": {
      "jsesc": "npm:jsesc@0.5.0"
    },
    "npm:requirejs@2.1.20": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "vm": "github:jspm/nodelibs-vm@0.1.0"
    },
    "npm:rsvp@3.0.21": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:semver@2.3.2": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:source-map@0.1.32": {
      "amdefine": "npm:amdefine@1.0.0",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:traceur@0.0.72": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "commander": "npm:commander@2.8.1",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "glob": "npm:glob@4.5.3",
      "module": "github:jspm/nodelibs-module@0.1.0",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "regexpu": "npm:regexpu@0.3.0",
      "rsvp": "npm:rsvp@3.0.21",
      "semver": "npm:semver@2.3.2",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:util@0.10.3": {
      "inherits": "npm:inherits@2.0.1",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:vm-browserify@0.0.4": {
      "indexof": "npm:indexof@0.0.1"
    }
  }
});

