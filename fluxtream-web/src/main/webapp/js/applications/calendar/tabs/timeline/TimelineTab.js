define(["core/Tab", "core/FlxState", "core/grapher/Grapher",
        "applications/calendar/App"],
    function(Tab, FlxState, Grapher, Calendar) {

    var timelineTab = new Tab("calendar", "timeline", "Candide Kemmler", "icon-film", false);
    var digest;
    var grapher = null;
    var connectorEnabled;

    function connectorDisplayable(connector){
        return connector.channelNames.length != 0;
    }

    function connectorsAlwaysEnabled(){
        return true;
    }

    function render(params) {
        params.setTabParam(null);
        digest = params.digest;
        connectorEnabled = params.connectorEnabled;
        this.getTemplate("text!applications/calendar/tabs/timeline/template.html", "timeline", function() {
            setup(digest, params.timeUnit);
        });
    }



    function setup(digest, timeUnit) {
        if (grapher == null) {
            grapher = new Grapher($("#timelineTabContainer"),{onLoad:function(){
                grapher.setRange(digest.tbounds.start/1000, digest.tbounds.end/1000);
                for (var connectorName in connectorEnabled){
                    connectorToggled(connectorName,null,connectorEnabled[connectorName]);
                }
                var prevDateString = null;
                grapher.dateAxis.addAxisChangeListener(function() {
                    var timeUnit = grapher.getCurrentTimeUnit();
                    var center = (grapher.dateAxis.getMin() +grapher. dateAxis.getMax()) / 2.0;
                    var date = new Date(center * 1000);
                    var dateChangeBuffer = 24 * 3600 * 1000 / 12;
                    var dateEarly = new Date(center * 1000 - dateChangeBuffer);
                    var dateLater = new Date(center * 1000 + dateChangeBuffer);
                    var dateString = Calendar.toDateString(date,timeUnit);
                    var dateStringEarly = Calendar.toDateString(dateEarly,timeUnit);
                    var dateStringLater = Calendar.toDateString(dateLater,timeUnit);
                    if (dateString != prevDateString && dateStringEarly != prevDateString && dateStringLater != prevDateString) {
                        Calendar.dateChanged(dateString, timeUnit);
                        prevDateString = dateString;
                    }
                });
            }});
        }
        else{
            $(window).resize();
        }
    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            if (connectorName == digest.selectedConnectors[i].connectorName){
                var channels = digest.selectedConnectors[i].channelNames;
                for (var i = 0; i < channels.length; i++){
                    if (enabled){
                        grapher.addChannel(channels[i]);
                    }
                    else{
                        grapher.removeChannel(channels[i]);
                    }

                }
                return;
            }
        }
    }

    function timeNavigation(nav){
        var splits = nav.split("/");
        switch (splits[0]){
            case "prev":
                grapher.gotoTime("back");
                break;
            case "next":
                grapher.gotoTime("forward");
                break;
            case "DAY":
                if (grapher.getCurrentTimeUnit() != "DAY"){
                    grapher.setZoom(24*3600);
                }
                break;
            case "WEEK":
                if (grapher.getCurrentTimeUnit() != "WEEK"){
                    grapher.setZoom(7*24*3600);
                }
                break;
            case "MONTH":
                if (grapher.getCurrentTimeUnit() != "MONTH"){
                    grapher.setZoom(30*24*3600);
                }
                break;
            case "YEAR":
                if (grapher.getCurrentTimeUnit() != "YEAR"){
                    grapher.setZoom(365*24*3600);
                }
                break;
            case "today":
                var end = new Date().getTime() / 1000;
                var diff = end - grapher.dateAxis.getMax();
                grapher.setRange(grapher.dateAxis.getMin() + diff, grapher.dateAxis.getMax() + diff);
                break;
            case "set":
                switch (splits[1]){
                    case "date":
                        var dateParts = splits[2].split("-");
                        var year = dateParts[0];
                        var month = parseInt(dateParts[1]) - 1;
                        var day = dateParts[2];
                        var start = new Date(year,month,day,0,0,0,0).getTime()/1000;
                        var end = new Date(year,month,day,11,59,59,999).getTime()/1000;
                        grapher.setRange(start,end);
                        break;
                    case "week":
                        var datePartsStart = splits[2].split("-");
                        var datePartsEnd = splits[3].split("-");
                        var yearStart = datePartsStart[0];
                        var monthStart = parseInt(datePartsStart[1]) - 1;
                        var dayStart = datePartsStart[2];
                        var yearEnd = datePartsEnd[0];
                        var monthEnd = parseInt(datePartsEnd[1]) - 1;
                        var dayEnd = datePartsEnd[2];
                        var start = new Date(yearStart,monthStart,dayStart,0,0,0,0).getTime()/1000;
                        var end = new Date(yearEnd,monthEnd,dayEnd,11,59,59,999).getTime()/1000;
                        grapher.setRange(start,end);
                        break;
                    case "month":
                        var year = splits[2];
                        var month = splits[3] - 1;
                        var start = new Date(year,month,1,0,0,0,0).getTime()/1000;
                        var end = new Date(year,month,App.getLastDayOfMonth(year,month),11,59,59,999).getTime()/1000;
                        grapher.setRange(start,end);
                        break;
                    case "year":
                        var year = splits[2];
                        var start = new Date(year,0,1,0,0,0,0).getTime() /1000;
                        var end = new Date(year,11,31,11,59,59,999).getTime() /1000;
                        grapher.setRange(start,end);
                        break;
                    default:
                        return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }

    timelineTab.initialized = false;
    timelineTab.render = render;
    timelineTab.connectorToggled = connectorToggled;
    timelineTab.connectorDisplayable = connectorDisplayable;
    timelineTab.connectorsAlwaysEnabled = connectorsAlwaysEnabled;
    timelineTab.timeNavigation = timeNavigation;
    return timelineTab;
});