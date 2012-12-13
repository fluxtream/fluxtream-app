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
            grapher.setRange(Calendar.start / 1000, Calendar.end / 1000);
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
                    // TODO: re-enable this once it's properly integrated with
                    // Calendar.dateChanged()
                    /*
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
                    */
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

    timelineTab.initialized = false;
    timelineTab.render = render;
    timelineTab.connectorToggled = connectorToggled;
    timelineTab.connectorDisplayable = connectorDisplayable;
    timelineTab.connectorsAlwaysEnabled = connectorsAlwaysEnabled;
    return timelineTab;
});