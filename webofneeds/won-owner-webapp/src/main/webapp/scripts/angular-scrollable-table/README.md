angular-scrollable-table
========================

Yet another table directive for AngularJS.

This one features a fixed header that elegantly handles overly-long column header names.

Other features:
* Scroll to row
* Sortable header with custom comparator functions
* Resizable columns

Usage:
`angular.module('myApp', ['scrollable-table', ...]);`

Example:

```
<scrollable-table watch="visibleProjects">
  <table class="table table-striped table-bordered">
    <thead>
      <tr>
         <th sortable-header col="facility">Facility</th>
         ...
      </tr>
    </thead>
    <tbody>
      <tr ng-repeat="proj in visibleProjects" row-id="{{ proj.facility }}" 
          ng-class="{info: selected == proj.facility}" >
        <td>{{ proj.facility }}</td>
        ...
      </tr>
    </tbody>
  </table>
</scrollable-table>
```
    
where the controller contains

```
    $scope.visibleProjects = [{
      facility: "Atlanta",
      code: "C-RD34",
      cost: 540000,
      conditionRating: 52,
      extent: 100,
      planYear: 2014
    }, ...];
    
    $scope.$watch('selected', function(fac) {
       $scope.$broadcast("rowSelected", fac);
    });
})
```

Third-party dependencies: 
* jQuery
* Bootstrap 3 CSS (for styling, optional.  See the 'bootstrap2' branch also)

Demo here: http://jsfiddle.net/Gary_Li/T9Wek/

More infomation here: http://blog.boxelderweb.com/2013/12/19/angularjs-fixed-header-scrollable-table/

License: MIT

### Updates

8/22/14 : Created a 'bootstrap2' branch.  Merged a pull request into master that supports bootstrap 2 and resizable columns.
