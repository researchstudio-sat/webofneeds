/**
 * Created by ksinger on 28.08.2015.
 */

;
import angular from 'angular';

function genComponentConf() {
    let template = `
        <div class="won-gallery__selected">
            <img src="images/furniture_big.jpg" alt="a table"/>
        </div>
        <div class="won-gallery__thumbs">
            <div class="won-gallery__thumbs__frame">
                <img src="images/furniture2.png" alt="a combination of shelfs"/>
            </div>
            <div class="won-gallery__thumbs__frame">
                <img src="images/furniture4.png" alt="a white, modern chair"/>
            </div>
            <div class="won-gallery__thumbs__frame">
                <img src="images/furniture1.png" alt="a white, modern chair"/>
            </div>
            <div class="won-gallery__thumbs__frame">
                <img src="images/furniture3.png" alt="a white, modern chair"/>
            </div>
        </div>
        <div class="won-gallery__controls">
            <svg style="--local-primary:var(--won-primary-color);"
                class="won-gallery__back">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
            </svg>
            <svg style="--local-primary:var(--won-primary-color);"
                class="won-gallery__forward">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
            </svg>
        </div>`

    return {
        restrict: 'E',
        template: template
    }
}

export default angular.module('won.owner.components.gallery', [])
    .directive('wonGallery', genComponentConf)
    .name;
