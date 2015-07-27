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
    "github:jspm/nodelibs-process@0.1.1": {
      "process": "npm:process@0.10.1"
    },
    "github:twbs/bootstrap@3.3.5": {
      "jquery": "github:components/jquery@2.1.4"
    },
    "npm:babel-runtime@5.6.15": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:core-js@0.9.18": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    }
  }
});

