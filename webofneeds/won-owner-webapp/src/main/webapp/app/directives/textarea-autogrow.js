// code adapted from https://stackoverflow.com/questions/17731083/how-to-autogrow-text-area-with-css3
import angular from 'angular';
import {
  clamp,
} from '../utils.js';

function genDirectiveConf() {
    return {
      restrict: 'A',
      link: (scope, element) => {

        let area = element[0];


        /* 
         * necessary styling 
         */

        //Height math depends on content box sizing
        area.style.boxSizing = 'content-box';
        // no reason to allow manual resizing, as it will automatically be resized on first input
        area.style.resize = 'none'; 



        /*
         * calculate line-height
         */

        const originalContent = area.value;
        const originalPlaceholder = area.placeholder;

        // reduce to one line in height, so lineHeight can be calculated correctly
        area.value = '';
        area.placeholder = '';
        area.style.height = '0px';
        
        const style = window.getComputedStyle(area, null);
        const offsets = parseFloat(style.paddingTop) + parseFloat(style.paddingBottom);

        const lineHeight = area.scrollHeight - offsets;
        
        area.value = originalContent;
        area.placeholder = originalPlaceholder;
        


        /*
         * update height
         */

        function updateHeight() {
          area.style.height = '0px';
          const height = area.scrollHeight - offsets;
          
          const lines = clamp(Math.floor(height/lineHeight), 1, 4);
          if(height > lineHeight*4) {
            area.style.overflowY = 'scroll';
          } else {
            area.style.overflowY = 'hidden';
          }
          area.style.height = lines * lineHeight + 'px';
        }

        element.on('input', updateHeight);
        // area.addEventListener('input', updateHeight);


        updateHeight();
      },
    };
  }

export default angular.module('won.owner.directives.textareaAutogrow', [])
    .directive('wonTextareaAutogrow', genDirectiveConf)
    .name;