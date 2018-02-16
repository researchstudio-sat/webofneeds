// code adapted from https://stackoverflow.com/questions/17731083/how-to-autogrow-text-area-with-css3
import angular from 'angular';

function genDirectiveConf() {
    return {
      restrict: 'A',
      link: (scope, element) => {

        const updateHeight = () => {
          const text = element[0].value;
          const lines = 1 + (text.match(/\n/g) || []).length;
          element[0].rows = lines;
        }
        updateHeight(); // update instantly, in case there's already content

        element.on('input change', updateHeight);
      },
    };
  }

export default angular.module('won.owner.directives.textareaAutogrow', [])
    .directive('wonTextareaAutogrow', genDirectiveConf)
    .name;

