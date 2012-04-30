define(["applications/calendar/tabs/Tab"], function(Tab) {

    var listTab = new Tab("list", "Candide Kemmler", "icon-list", true);

    function render(digest, timeUnit) {
		this.getUrl("/tabs/list/0", "list", bindPaginationElements, true);
	}
	
	function getFacets(page) {
        if (page<0) return;

		var checkedConnectors = getCheckedConnectors();

        listTab.getUrl("/tabs/list/" + page +
            "?filter=" + checkedConnectors, "list", bindPaginationElements, true);
	}
	
	function bindPaginationElements() {
        $(".btnListChecked")
		$(".btnListChecked").click(function(e) {
			$(this).removeClass("btnListChecked");
			$(this).addClass("btn-inverse");
			getFacets(0);
		});
		$(".btn-inverse").click(function(e) {
			$(this).addClass("btnListChecked");
			$(this).removeClass("btn-inverse");
			getFacets(0);
		});
		$(".paginationLink").click(function(e) {
			var valueAttr = $(this).attr("pageNumber");
            var value = parseInt(valueAttr);
			getFacets(value);
		});
	}

	function getCheckedConnectors() {

		var connectors = $(".btnList"),
			uncheckedConnectorNames = "all",
			checkedConnectorNames = "all";

		if (connectors!=null&&connectors.length>0) {
			uncheckedConnectorNames = "";
			checkedConnectorNames = "";
			$.each(connectors, function(index) {
				var connectorName = connectors[index].getAttribute("value");
				if ($(connectors[index]).hasClass("btnListChecked")) {
					if (checkedConnectorNames!="") checkedConnectorNames += ",";
					checkedConnectorNames += connectorName;
				} else {
					if (uncheckedConnectorNames!="") uncheckedConnectorNames += ",";
					uncheckedConnectorNames += connectorName;
				}
			});
		}
		
		return uncheckedConnectorNames;
	}

    listTab.render = render;
	return listTab;
	
});
