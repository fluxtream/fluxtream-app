define(["core/DashboardWidget", "core/widgetComponents/GrapherComponent", "core/grapher/BTCore",
        "core/widgetComponents/averageSteps", "core/widgetComponents/averageCaloriesBurned"],
    function(DashboardWidget,GrapherComponent,BodyTrack,AverageStepsComponent,AverageCaloriesBurnedComponent) {

        var GrapherWidget = new DashboardWidget();

        GrapherWidget.init = function() {
            require(["text!" + this.manifest.WidgetRepositoryURL + "/"
                         + this.manifest.WidgetName + "/grapherWidget.mustache"], function(template) {
                GrapherWidget.postLoad(template);
            });
        };

        GrapherWidget.postLoad = function(template) {
            this.setTitle(this.settings.title);
            if (template != null){
                var html = Hogan.compile(template);
                $("#grapherWidget-widget .flx-body").empty();
                $("#grapherWidget-widget .flx-body").append(
                    html.render({"manifest" : this.manifest})
                );
                this.grapher = null;
            }


            var channelName = this.settings.deviceName + "." + this.settings.channelName;

            if (this.grapher != null && this.grapher.channelName != channelName){
                $("#grapherWidgetWidget").empty();
                this.grapher = null;
            }
            else if (this.grapher != null){
                this.grapher.plot.setStyle(this.settings.style);
            }

            var tbounds = {start:this.digest.tbounds.start - 12 * 3600 * 1000, end: this.digest.tbounds.end + 12 * 3600 * 1000};

            if (this.grapher == null){
                $("#grapherWidgetWidget").empty();
                this.grapher = new GrapherComponent($("#grapherWidgetWidget"),channelName,tbounds,{
                    yAxisPosition:"right",
                    yAxisWidth:50,
                    xAxisHeight:0,
                    positiveOnly:true,
                    style:this.settings.style
                });

                this.grapher.yAxis.setPaint(function(axis){
                    var ctx = axis.canvas.getContext("2d");
                    ctx.clearRect(0,0,axis.canvas.width,axis.canvas.height);//clear drawing area

                    ctx.fillStyle="black";

                    var min = axis.getMin();
                    var max = axis.getMax();

                    var rounding = 1;
                    var changePerIncrement = (max - min) / 10;
                    if (changePerIncrement >= 10000)
                        rounding = 10;
                    else if (changePerIncrement >= 1000)
                        rounding = 1000;
                    else if (changePerIncrement >= 100)
                        rounding = 100;
                    else if (changePerIncrement >= 10)
                        rounding = 10;

                    for (var i = 0; i < 10; i++){//draw 10 numbers;
                        var value = min + (changePerIncrement * i);
                        var y = axis.canvas.height - axis.canvas.height * i / 10;
                        ctx.fillText(Math.round(value / rounding) * rounding,0,y);
                    }
                });
            }
            var tileURIs = GrapherWidget.getTileURIs(tbounds,channelName);
            var dataCount = 0;
            var that = this;
            var checkURIForData = function(offset){
                var uri = tileURIs[offset];
                $.ajax(uri,{
                    success: function(data){
                        data = data.data;
                        for (var i = 0; i < data.length; i++){
                            var ts = data[i][0]*1000;
                            if (ts >= tbounds.start && ts <= tbounds.end)
                                dataCount++;
                        }
                        offset++;
                        if (offset == tileURIs.length){//no data
                            if (dataCount == 1){
                                switch (channelName){
                                    case "Fitbit.steps":
                                        $("#grapherWidgetWidget").empty();
                                        that.grapher = null;
                                        new AverageStepsComponent(that,$("#grapherWidgetWidget"),that.digest.cachedData["fitbit-activity_summary"],"steps");
                                        return;
                                    case "BodyMedia.totalSteps":
                                        $("#grapherWidgetWidget").empty();
                                        that.grapher = null;
                                        new AverageStepsComponent(that,$("#grapherWidgetWidget"),that.digest.cachedData["bodymedia-steps"],"steps");
                                        return;
                                    case "Fitbit.caloriesOut":
                                        $("#grapherWidgetWidget").empty();
                                        that.grapher = null;
                                        new AverageCaloriesBurnedComponent(that,$("#grapherWidgetWidget"),that.digest.cachedData["fitbit-activity_summary"],"caloriesOut");
                                        return;
                                }
                            }
                            else if (dataCount == 0){
                                $("#grapherWidgetWidget").empty();
                                that.grapher = null;
                                App.loadMustacheTemplate("/widgets/grapherWidget/grapherWidgetTemplates.html","noData",function(template){
                                    $("#grapherWidgetWidget").append(template.render({}));
                                })
                            }
                        }
                        else{
                            checkURIForData(offset);
                        }
                    }
                });
            }
            checkURIForData(0);
        }

        GrapherWidget.getTileURIs = function(tbounds,channelName){
            var level = Math.floor(Math.log((tbounds.end - tbounds.start) / 1000 / 512)/Math.LN2);
            var tileWidth = Math.pow(2,9+level);
            var firstTile = Math.floor(tbounds.start/1000/tileWidth);
            var lastTile = Math.floor(tbounds.end/1000/tileWidth);
            var tileURIs = [];
            for (var i = firstTile; i <= lastTile; i++){
                tileURIs[tileURIs.length] = "/api/bodytrack/tiles/" + App.getUID() + "/" + channelName + "/" + level + "." + i + ".json";
            }
            return tileURIs;
        }

        GrapherWidget.validateSettings = function(){
            this.saveSettings({
                deviceName:$("#deviceSelector").val(),
                channelName:$("#channelSelector").val(),
                style:this.getStyleFromConfigurationControls(),
                title:$("#titleInput").val()
            });
        }

        GrapherWidget.bindWidgetSettings = function(widgetSettings){
            $("#titleInput").val(widgetSettings.title);
            bindStyleEditor(widgetSettings.style);
            BodyTrack.SOURCES.getAvailableList(function (sources){

                $("#deviceSelector").change(function(){
                    $("#channelSelector")[0] = 0;
                    $("#channelSelector").html("");
                    var i = $("#deviceSelector")[0].selectedIndex;
                    for (var j = 0; j < sources[i].channels.length; j++){
                        $("#channelSelector").append("<option>" + sources[i].channels[j].name + "</option>");
                        if (sources[i].channels[j].name == widgetSettings.channelName)
                            $("#channelSelector")[0].selectedIndex = j;
                    }
                });
                for (var i = 0; i < sources.length; i++){
                    $("#deviceSelector").append("<option>" + sources[i].name + "</option>");
                    if (sources[i].name == widgetSettings.deviceName){
                        $("#deviceSelector")[0].selectedIndex = i;
                        $("#deviceSelector").change();
                    }
                }
                $("#deviceSelector").enable();
                $("#channelSelector").enable();
            });
        }


        var defaultStyle = {styles:[{type:"lollipop",show:true,lineWidth:5,radius:0,color:"rgb(0, 102, 0)",fill:false},{type:"value",show:true,fillColor:"rgb(0, 102, 0)",marginWidth:5,verticalOffset:7,numberFormat:"###,##0"}],highlight:{styles:[{type:"value",show:true,fillColor:"rgb(0, 102, 0)",marginWidth:5,verticalOffset:7,numberFormat:"###,##0"}],lineWidth:5}};
        GrapherWidget.defaultSettings = function(widgetSettings){
            if (widgetSettings.deviceName == null)
                widgetSettings.deviceName = "Fitbit";
            if (widgetSettings.channelName == null)
                widgetSettings.channelName = "steps";
            if (widgetSettings.style == null)
                widgetSettings.style = defaultStyle;
            if (widgetSettings.title == null)
                widgetSettings.title = "Grapher";
            return widgetSettings;
        }

        function bindStyleEditor(style){

            if (style.comments == null)
                style.comments = {styles:[]};

            // get the next default color
            var defaultColor = "#" + jQuery.fn.colorPicker.getNextColor();

            // Load up the existing styles (if any) from the styles array
            var linesStyle = {"type" : "line", "show" : false, "lineWidth" : 1, "color" : defaultColor};
            var pointsStyle = {"type" : "point", "show" : false, "radius" : 2, "fill" : true, "color" : defaultColor, "fillColor" : defaultColor};
            var barsStyle = {"type" : "lollipop", "show" : false, "color" : defaultColor};
            var valuesStyle1 = {"type" : "value", "show" : false, "fillColor" : defaultColor};
            var valuesStyle2 = {"type" : "value", "show" : false, "fillColor" : defaultColor};
            var commentsStyle = {"type" : "point", "show" : true, "radius" : 3, "fill" : true, "color" : defaultColor, "fillColor" : defaultColor};

            for (var styleTypeIndex = 0; styleTypeIndex < style["styles"].length; styleTypeIndex++) {
                var theStyle = style["styles"][styleTypeIndex];
                if (typeof theStyle["type"] !== 'undefined') {
                    if (theStyle["type"] == "line") {
                        linesStyle = theStyle;
                    } else if (theStyle["type"] == "point" ||
                               theStyle["type"] == "square" ||
                               theStyle["type"] == "cross" ||
                               theStyle["type"] == "plus") {
                        // fill defaults to true if unspecified
                        if (typeof theStyle["fill"] === 'undefined') {
                            theStyle["fill"] = true;
                        }
                        pointsStyle = theStyle;
                    } else if (theStyle["type"] == "lollipop") {
                        // fill defaults to true if unspecified
                        if (typeof theStyle["fill"] === 'undefined') {
                            theStyle["fill"] = true;
                        }
                        barsStyle = theStyle;
                    } else if (theStyle["type"] == "value") {
                        valuesStyle1 = theStyle;
                    }

                    // show defaults to true if unspecified
                    if (typeof theStyle["show"] === 'undefined') {
                        theStyle["show"] = true;
                    }
                }
            }

            // build the type-ui field
            pointsStyle['type-ui'] = pointsStyle['type'];
            if (pointsStyle['fill'] && (pointsStyle['type'] == 'point' || pointsStyle['type'] == 'square')) {
                pointsStyle['type-ui'] += '-filled';
            }

            // Load up the existing styles (if any) from the highlight styles array--we currently only support the values style
            for (var highlightStyleTypeIndex = 0; highlightStyleTypeIndex < style["highlight"]["styles"].length; highlightStyleTypeIndex++) {
                var theHighlightStyle = style["highlight"]["styles"][highlightStyleTypeIndex];
                if (theHighlightStyle["type"] == "value") {
                    valuesStyle2 = theHighlightStyle;
                }

                // show defaults to true if unspecified
                if (typeof theHighlightStyle["show"] === 'undefined') {
                    theHighlightStyle["show"] = true;
                }
            }

            // now merge valuesStyle1 and valuesStyle2 (they should be identical, except for the "show" field)
            var valuesStyle = jQuery.extend(true, {}, valuesStyle1, valuesStyle2);
            valuesStyle["show"] = valuesStyle1["show"] || valuesStyle2["show"];

            // map the verticalOffset in valuesStyle to one of the three options we support.
            valuesStyle["verticalOffset"] = BodyTrack.TOOLS.parseInt(valuesStyle["verticalOffset"], 7);
            if (valuesStyle["verticalOffset"] > -3) {
                valuesStyle["verticalOffset"] = 7;
            } else if (valuesStyle["verticalOffset"] < -3) {
                valuesStyle["verticalOffset"] = -13;
            }

            // determine whether values should always be shown, or only on highlight (mouseover).  Note that the
            // concatenation here ensures that it's a string, which is required for when we set the selected index
            // of the select menu below.
            var showValuesOnlyOnHighlight = "" + (!valuesStyle1["show"] && valuesStyle2["show"]);

            // Load up the existing styles (if any) from the comments styles array--we currently only support a single point style
            for (var commentsStyleTypeIndex = 0; commentsStyleTypeIndex < style["comments"]["styles"].length; commentsStyleTypeIndex++) {
                var theCommentsStyle = style["comments"]["styles"][commentsStyleTypeIndex];
                if (theCommentsStyle["type"] == "point" ||
                    theCommentsStyle["type"] == "square" ||
                    theCommentsStyle["type"] == "cross" ||
                    theCommentsStyle["type"] == "plus") {
                    commentsStyle = theCommentsStyle;
                }

                // show defaults to true if unspecified
                if (typeof commentsStyle["show"] === 'undefined') {
                    commentsStyle["show"] = true;
                }
            }

            // build the type-ui field
            commentsStyle['type-ui'] = commentsStyle['type'];
            if (commentsStyle['fill'] && (commentsStyle['type'] == 'point' || commentsStyle['type'] == 'square')) {
                commentsStyle['type-ui'] += '-filled';
            }

            var channelElementId = "grapherWidgetStyleConfig";
            var isZeo = false;
            /* Configure the Zeo options ------------------------------------------------------------------------------ */
            $("#" + channelElementId + "-config-zeo-show").prop("checked", isZeo);

            /* Configure the Color Override options ------------------------------------------------------------------- */
            $("#" + channelElementId + "-config-color-override-color").colorPicker();
            $("#" + channelElementId + "-config-color-override-color").val("#000000"); // we always want this one to start out black
            $("#" + channelElementId + "-config-color-override-color").change();
            $("#" + channelElementId + "-config-color-override-color").change(function() {
                var overrideColor = $("#" + channelElementId + "-config-color-override-color").next(".color_picker").css("background-color");
                $("#" + channelElementId + "-config-lines-color").val(overrideColor).change();
                $("#" + channelElementId + "-config-points-color").val(overrideColor).change();
                $("#" + channelElementId + "-config-points-fillColor").val(overrideColor).change();
                $("#" + channelElementId + "-config-bars-color").val(overrideColor).change();
                $("#" + channelElementId + "-config-values-fillColor").val(overrideColor).change();
                $("#" + channelElementId + "-config-comments-color").val(overrideColor).change();
                $("#" + channelElementId + "-config-comments-fillColor").val(overrideColor).change();
            });

            /* Configure the Lines options ---------------------------------------------------------------------------- */

            // don't show this section if this is a Zeo plot
            $("#" + channelElementId + "-config-lines").toggle(!isZeo);

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-lines-show").prop("checked", linesStyle["show"] && !isZeo);
            //$("#" + channelElementId + "-config-lines-show").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the lineWidth select menu
            $("#" + channelElementId + "-config-lines-lineWidth").val(BodyTrack.TOOLS.parseInt(linesStyle["lineWidth"], 1));
            //$("#" + channelElementId + "-config-lines-lineWidth").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-lines-lineWidth").msDropDown();

            // Create the color colorpicker, and set its initial value
            $("#" + channelElementId + "-config-lines-color").colorPicker();
            $("#" + channelElementId + "-config-lines-color").val(typeof linesStyle["color"] === 'undefined' ? defaultColor : linesStyle["color"]);
            $("#" + channelElementId + "-config-lines-color").change();
            //$("#" + channelElementId + "-config-lines-color").change(updateDataSeriesPlotChannelConfig);

            /* Configure the Points options --------------------------------------------------------------------------- */

            // don't show this section if this is a Zeo plot
            $("#" + channelElementId + "-config-points").toggle(!isZeo);

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-points-show").prop("checked", pointsStyle["show"] && !isZeo);
            //$("#" + channelElementId + "-config-points-show").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the type select menu and the initial state of the fillColor color picker
            $("#" + channelElementId + "-config-points-type").val(pointsStyle['type-ui']);
            //$("#" + channelElementId + "-config-points-type").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-points-type").change(function() {
                var isFilledType = $("#" + channelElementId + "-config-points-type").val().match(/-filled$/) !== null;
                $("#" + channelElementId + "-config-points-fillColor-container").toggle(isFilledType);
            });
            $("#" + channelElementId + "-config-points-type").msDropDown();
            $("#" + channelElementId + "-config-points-fillColor-container").toggle(pointsStyle['fill']);

            // Set the initial value of the radius select menu
            $("#" + channelElementId + "-config-points-radius").val(BodyTrack.TOOLS.parseInt(pointsStyle["radius"], 2));
            //$("#" + channelElementId + "-config-points-radius").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-points-radius").msDropDown();

            // Create the color colorpicker, and set its initial value
            $("#" + channelElementId + "-config-points-color").colorPicker();
            $("#" + channelElementId + "-config-points-color").val(typeof pointsStyle["color"] === 'undefined' ? defaultColor : pointsStyle["color"]);
            $("#" + channelElementId + "-config-points-color").change();
            //$("#" + channelElementId + "-config-points-color").change(updateDataSeriesPlotChannelConfig);

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelElementId + "-config-points-fillColor").colorPicker();
            $("#" + channelElementId + "-config-points-fillColor").val(typeof pointsStyle["fillColor"] === 'undefined' ? defaultColor : pointsStyle["fillColor"]);
            $("#" + channelElementId + "-config-points-fillColor").change();
            //$("#" + channelElementId + "-config-points-fillColor").change(updateDataSeriesPlotChannelConfig);

            /* Configure the Bars options ----------------------------------------------------------------------------- */

            // don't show this section if this is a Zeo plot
            $("#" + channelElementId + "-config-bars").toggle(!isZeo);

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-bars-show").prop("checked", barsStyle["show"] && !isZeo);
            //$("#" + channelElementId + "-config-bars-show").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the lineWidth select menu
            $("#" + channelElementId + "-config-bars-lineWidth").val(BodyTrack.TOOLS.parseInt(barsStyle["lineWidth"], 1));
            //$("#" + channelElementId + "-config-bars-lineWidth").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-bars-lineWidth").msDropDown();

            // Create the color colorpicker, and set its initial value
            $("#" + channelElementId + "-config-bars-color").colorPicker();
            $("#" + channelElementId + "-config-bars-color").val(typeof barsStyle["color"] === 'undefined' ? defaultColor : barsStyle["color"]);
            $("#" + channelElementId + "-config-bars-color").change();
            //$("#" + channelElementId + "-config-bars-color").change(updateDataSeriesPlotChannelConfig);

            /* Configure the Values options --------------------------------------------------------------------------- */

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-values-show").prop("checked", valuesStyle["show"]);
            //$("#" + channelElementId + "-config-values-show").change(updateDataSeriesPlotChannelConfig);

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelElementId + "-config-values-fillColor").colorPicker();
            $("#" + channelElementId + "-config-values-fillColor").val(typeof valuesStyle["fillColor"] === 'undefined' ? defaultColor : valuesStyle["fillColor"]);
            $("#" + channelElementId + "-config-values-fillColor").change();
            //$("#" + channelElementId + "-config-values-fillColor").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the numberFormat select menu
            $("#" + channelElementId + "-config-values-numberFormat").val(typeof valuesStyle["numberFormat"] === 'undefined' ? "###,##0.0##" : valuesStyle["numberFormat"]);
            //$("#" + channelElementId + "-config-values-numberFormat").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-values-numberFormat").msDropDown();
            $("#" + channelElementId + "-config-values-numberFormat_msdd").css("width","47px")

            // Set the initial value of the verticalOffset select menu
            $("#" + channelElementId + "-config-values-verticalOffset").val(BodyTrack.TOOLS.parseInt(valuesStyle["verticalOffset"], 7));
            //$("#" + channelElementId + "-config-values-verticalOffset").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-values-verticalOffset").msDropDown();
            $("#" + channelElementId + "-config-values-verticalOffset_msdd").css("width","47px");

            // Set the initial value of the showOnlyOnHighlight select menu and the initial visibility of the marginWidth select menu
            $("#" + channelElementId + "-config-values-showOnlyOnHighlight").val(showValuesOnlyOnHighlight);
            //$("#" + channelElementId + "-config-values-showOnlyOnHighlight").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-values-showOnlyOnHighlight").change(function() {
                var shouldShowMarginMenu = $("#" + channelElementId + "-config-values-showOnlyOnHighlight").val() == 'false';
                $("#" + channelElementId + "-config-values-marginWidth-label-container").toggle(shouldShowMarginMenu);
                $("#" + channelElementId + "-config-values-marginWidth-container").toggle(shouldShowMarginMenu);
            });
            $("#" + channelElementId + "-config-values-showOnlyOnHighlight").msDropDown();
            $("#" + channelElementId + "-config-values-showOnlyOnHighlight_msdd").css("width","72px");
            var showValuesOnlyOnHighlightBoolean = showValuesOnlyOnHighlight == 'true';
            $("#" + channelElementId + "-config-values-marginWidth-label-container").toggle(!showValuesOnlyOnHighlightBoolean);
            $("#" + channelElementId + "-config-values-marginWidth-container").toggle(!showValuesOnlyOnHighlightBoolean);

            // Set the initial value of the marginWidth select menu
            $("#" + channelElementId + "-config-values-marginWidth").val(BodyTrack.TOOLS.parseInt(valuesStyle["marginWidth"], 5));
            //$("#" + channelElementId + "-config-values-marginWidth").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-values-marginWidth").msDropDown();

            /* Configure the Comments options ------------------------------------------------------------------------- */

            // Set the initial value of the show checkbox
            $("#" + channelElementId + "-config-comments-show").prop("checked", commentsStyle["show"]);
            //$("#" + channelElementId + "-config-comments-show").change(updateDataSeriesPlotChannelConfig);

            // Set the initial value of the type select menu and the initial state of the fillColor color picker
            $("#" + channelElementId + "-config-comments-type").val(commentsStyle['type-ui']);
            //$("#" + channelElementId + "-config-comments-type").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-comments-type").change(function() {
                var isFilledType = $("#" + channelElementId + "-config-comments-type").val().match(/-filled$/) !== null;
                $("#" + channelElementId + "-config-comments-fillColor-container").toggle(isFilledType);
            });
            $("#" + channelElementId + "-config-comments-type").msDropDown();
            $("#" + channelElementId + "-config-comments-fillColor-container").toggle(commentsStyle['fill']);

            // Set the initial value of the radius select menu
            $("#" + channelElementId + "-config-comments-radius").val(BodyTrack.TOOLS.parseInt(commentsStyle["radius"], 3));
            //$("#" + channelElementId + "-config-comments-radius").change(updateDataSeriesPlotChannelConfig);
            $("#" + channelElementId + "-config-comments-radius").msDropDown();

            // Create the color colorpicker, and set its initial value
            $("#" + channelElementId + "-config-comments-color").colorPicker();
            $("#" + channelElementId + "-config-comments-color").val(typeof commentsStyle["color"] === 'undefined' ? defaultColor : commentsStyle["color"]);
            $("#" + channelElementId + "-config-comments-color").change();
            //$("#" + channelElementId + "-config-comments-color").change(updateDataSeriesPlotChannelConfig);

            // Create the fillColor colorpicker, and set its initial value
            $("#" + channelElementId + "-config-comments-fillColor").colorPicker();
            $("#" + channelElementId + "-config-comments-fillColor").val(typeof commentsStyle["fillColor"] === 'undefined' ? defaultColor : commentsStyle["fillColor"]);
            $("#" + channelElementId + "-config-comments-fillColor").change();
            //$("#" + channelElementId + "-config-comments-fillColor").change(updateDataSeriesPlotChannelConfig);

            $("#color_selector").css("zIndex","20000");
        }

        GrapherWidget.getStyleFromConfigurationControls = function(){

            var newStyle = {};
            var channelElementId = "grapherWidgetStyleConfig";

            newStyle['styles'] = [];                // completely overwrite the existing styles array
            newStyle['highlight'] = {};             // completely overwrite the existing highlight object
            newStyle['highlight']['styles'] = [];   // initialize the highlight styles array

            var isZeo = $("#channelSelector").val() == "Sleep_Graph";
            var highlightLineWidth = 0;

            if (isZeo) {
                newStyle['styles'][newStyle['styles'].length] = {
                    "type" : "zeo",
                    "show" : true
                };
                highlightLineWidth = 1;
            } else {
                var linesStyle = {
                    "type"      : "line",
                    "show"      : $("#" + channelElementId + "-config-lines-show").is(':checked'),
                    "color"     : $("#" + channelElementId + "-config-lines-color").next(".color_picker").css("background-color"),
                    "lineWidth" : BodyTrack.TOOLS.parseInt($("#" + channelElementId + "-config-lines-lineWidth").val(), 1)
                };

                var pointsStyleType = $("#" + channelElementId + "-config-points-type").val();
                var pointsStyleFill = pointsStyleType.match(/-filled$/) !== null;
                var pointsStyle = {
                    "type"      : pointsStyleType.replace('-filled', ''),
                    "show"      : $("#" + channelElementId + "-config-points-show").is(':checked'),
                    "lineWidth" : 1,
                    "radius"    : BodyTrack.TOOLS.parseInt($("#" + channelElementId + "-config-points-radius").val(), 2),
                    "color"     : $("#" + channelElementId + "-config-points-color").next(".color_picker").css("background-color"),
                    "fill"      : pointsStyleFill,
                    "fillColor" : $("#" + channelElementId + "-config-points-fillColor").next(".color_picker").css("background-color")
                };

                var barsStyle = {
                    "type"      : "lollipop",
                    "show"      : $("#" + channelElementId + "-config-bars-show").is(':checked'),
                    "lineWidth" : BodyTrack.TOOLS.parseInt($("#" + channelElementId + "-config-bars-lineWidth").val(), 1),
                    "radius"    : 0,
                    "color"     : $("#" + channelElementId + "-config-bars-color").next(".color_picker").css("background-color"),
                    "fill"      : false
                };

                // Add the styles to the array--note that ordering here isn't arbitrary.  Styles are rendered in the order
                // they appear in the array.  Thus, we put points AFTER lines and bars so that the point gets rendered on top.
                // Value is placed last so that it's on top of everything.
                newStyle['styles'][newStyle['styles'].length] = linesStyle;
                newStyle['styles'][newStyle['styles'].length] = barsStyle;
                newStyle['styles'][newStyle['styles'].length] = pointsStyle;

                // Compute the lineWidth value for highlights by taking the max line width from the lines and bars
                // styles (but only if they're visible) and incrementing by 1.
                if (linesStyle['show']) {
                    highlightLineWidth = Math.max(highlightLineWidth, linesStyle['lineWidth']);
                }
                if (barsStyle['show']) {
                    highlightLineWidth = Math.max(highlightLineWidth, barsStyle['lineWidth']);
                }
                highlightLineWidth += 1;
            }

            // build the values style (we decide next where to stick it)
            var valuesStyle = {
                "type"           : "value",
                "show"           : $("#" + channelElementId + "-config-values-show").is(':checked'),
                "fillColor"      : $("#" + channelElementId + "-config-values-fillColor").next(".color_picker").css("background-color"),
                "marginWidth"    : BodyTrack.TOOLS.parseInt($("#" + channelElementId + "-config-values-marginWidth").val(), 5),
                "verticalOffset" : BodyTrack.TOOLS.parseInt($("#" + channelElementId + "-config-values-verticalOffset").val(), 7),
                "numberFormat"   : $("#" + channelElementId + "-config-values-numberFormat").val()
            };

            // We'll always put the values style in both the styles array AND the highlight styles array.  The "show"
            // field will be false for both if Values option is unchecked.  The "show" field will be true for both if the
            // Values option is checked and the showOnlyOnHighlight option is false.  If the showOnlyOnHighlight option is
            // true, then the instance in the styles array will have show set to false
            newStyle['highlight']['styles'][newStyle['highlight']['styles'].length] = valuesStyle;
            var onlyShowValuesOnHighlight = $("#" + channelElementId + "-config-values-showOnlyOnHighlight").val() === 'true';
            if (onlyShowValuesOnHighlight) {
                // clone the valuesStyle instance
                var valuesStyleCopy = BodyTrack.TOOLS.clone(valuesStyle);
                valuesStyleCopy["show"] = false;
                newStyle['styles'][newStyle['styles'].length] = valuesStyleCopy;
            } else {
                newStyle['styles'][newStyle['styles'].length] = valuesStyle;
            }

            // set the highlight line width
            newStyle['highlight']['lineWidth'] = highlightLineWidth;

            // Finally, build the comments style (this completely overwrites the existing comments object)
            var commentsStyleType = $("#" + channelElementId + "-config-comments-type").val();
            var commentsStyleFill = commentsStyleType.match(/-filled$/) !== null;
            newStyle['comments'] = {
                "show"           : $("#" + channelElementId + "-config-comments-show").is(':checked'),
                "styles"         : [{
                                        "type"      : commentsStyleType.replace('-filled', ''),
                                        "show"      : $("#" + channelElementId + "-config-comments-show").is(':checked'),
                                        "lineWidth" : 1,
                                        "radius"    : BodyTrack.TOOLS.parseInt($("#" + channelElementId + "-config-comments-radius").val(), 3),
                                        "color"     : $("#" + channelElementId + "-config-comments-color").next(".color_picker").css("background-color"),
                                        "fill"      : commentsStyleFill,
                                        "fillColor" : $("#" + channelElementId + "-config-comments-fillColor").next(".color_picker").css("background-color")
                                    }],
                "verticalMargin" : 4
            };

            return newStyle;

        }

        return GrapherWidget;
    })