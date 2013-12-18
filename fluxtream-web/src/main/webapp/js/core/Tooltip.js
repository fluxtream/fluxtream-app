define(function(){
    var templates;
    App.loadAllMustacheTemplates("core/tooltipTemplates.html",function(t){
        templates = t;

    });

    var tooltips = [];

    App.addHideTooltipListener(function(){
        for (var i = 0, li = tooltips.length; i < li; i++){
            tooltips[i].remove();
        }
    });

    return {
        createTooltip: function(container,position,contents,color){
            color = $.xcolor.darken(color).getCSS();
            var orientation, tailOrientation;

            var containerWidth = container.width();
            var containerHeight = container.height();

            var angle = App.toPolar([containerWidth/2,containerHeight/2],position.x,position.y)[1];
            if (angle < 45 || angle > 315){
                orientation = "Right";
                tailOrientation = "left";
            }
            else if (angle > 135 && angle < 225){
                orientation = "Left";
                tailOrientation = "Right";
            }
            else if (position.y - containerHeight / 2 < 0){
                orientation = "Top";
                tailOrientation = "bottom";
            }
            else{
                orientation = "Bottom";
                tailOrientation = "top";
            }

            var tooltip = $(templates["tooltip-plain"].render({
                orientation:orientation,
                oppositeOrientation:tailOrientation,
                color:color
            }));
            var tail = tooltip.find(".flx-toolTipTail-" + orientation);
            tooltip.css("position","absolute");
            tooltip.find(".flx-toolTipContext").append(contents);
            container.append(tooltip);
            tooltips.push(tooltip);

            var tooltipWidth = tooltip.width();
            var tooltipHeight = tooltip.height();

            //positioning
            if (orientation == "Top" || orientation == "Bottom"){

                var tailOffset = 0;

                if (position.x < tooltipWidth / 2){
                    tooltip.css("left","0px");
                    tailOffset = position.x - tooltipWidth / 2;
                }
                else if (containerWidth - position.x < tooltipWidth / 2){
                    tooltip.css("left",containerWidth - tooltipWidth);
                    tailOffset = position.x - containerWidth + tooltipWidth / 2;
                }
                else{
                    tooltip.css("left",position.x - tooltipWidth / 2);
                }
                tail.css("left", (tooltipWidth - tail.outerWidth()) / 2 + tailOffset);

                if (orientation == "Top"){
                    tooltip.css("top",(position.y + tail.outerHeight()/ 2));
                }
                else{
                    tooltip.css("top",(position.y - tail.outerHeight()/ 2 - tooltipHeight));
                }
            }
            else{
                var tailOffset = 0;
                if (position.y < tooltipHeight / 2){
                    tooltip.css("top", "0px");
                    tailOffset = position.y - tooltipHeight / 2;
                }
                else if (containerHeight - position.y < tooltipHeight / 2){
                    tooltip.css("top",containerHeight - tooltipHeight);
                    tailOffset = position.y - containerHeight + tooltipHeight / 2;
                }
                else{
                    tooltip.css("top",position.y - tooltipHeight / 2);
                }

                tail.css("top", (tooltipHeight - tail.outerHeight()) / 2 + tailOffset);

                if (orientation == "Left"){
                    tooltip.css("left",position.x + tail.outerWidth() / 2);
                }
                else{
                    tooltip.css("left",position.x - tooltipWidth - tail.outerWidth() / 2);
                }
            }

            var oldRemove = tooltip.remove;

            tooltip.remove = function(){
                oldRemove.apply(this);
                var index = tooltips.indexOf(this);
                if (index >= 0)
                    tooltips.splice(index,1);
                if (tooltip.onRemove != null)
                    tooltip.onRemove();
            }
            tooltip.onRemove = null;

            return tooltip;

        }

    }
})