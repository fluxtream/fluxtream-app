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

    /**
     * Updates Calendar state in response to a change in grapher.dateAxis. This
     * fires for both URL changes and user dateAxis dragging. onAxisChanged() is
     * idempotent in the former case - the URL change already went through
     * renderState(), which also updates Calendar state.
     */
    function onAxisChanged() {
        var timeUnit = grapher.getCurrentTimeUnit(),
            date = new Date(grapher.dateAxis.getMin() * 1000),
            obj = Calendar.toState("timeline", timeUnit, date);
        Calendar.changeTabState(obj);
    }

    function setup(digest, timeUnit) {
        if (grapher !== null) {
            $(window).resize();
            return;
        }
        grapher = new Grapher($("#timelineTabContainer"),{onLoad:function(){
            for (var connectorName in connectorEnabled){
                connectorToggled(connectorName,null,connectorEnabled[connectorName]);
            }
            var prevDateString = null;
            grapher.dateAxis.addAxisChangeListener(function() {
                // NOTE: we use $.doTimeout() here to avoid spamming onAxisChanged().
                // This will fire 100ms after the user stops dragging, since
                // $.doTimeout() cancels earlier timeouts with the same name.
                $.doTimeout('TimelineTabAxisChange', 100, function() {
                    onAxisChanged();
                });
            });
        }});
    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        $.each(digest.selectedConnectors, function(i, connector) {
            if (connectorName !== connector.connectorName) {
                return true;
            }
            var channels = connector.channelNames;
            $.each(connector.channelNames, function(j, channelName) {
                if (enabled) {
                    grapher.addChannel(channelName);
                } else {
                    grapher.removeChannel(channelName);
                }
            });
            return false;
        });
    }

    timelineTab.initialized = false;
    timelineTab.render = render;
    timelineTab.connectorToggled = connectorToggled;
    timelineTab.connectorDisplayable = connectorDisplayable;
    timelineTab.connectorsAlwaysEnabled = connectorsAlwaysEnabled;
    return timelineTab;
});