define(["applications/log/widgets/Widget"], function(Widget) {

    var listWidget = new Widget("list", "Candide Kemmler", "icon-list");

    function render(digest, timeUnit) {
		this.getUrl("/widgets/list/0", "list", bindPaginationElements, true);
	}
	
	function getFacets(page) {

		var checkedConnectors = getCheckedConnectors();

        listWidget.getUrl("/widgets/list/" + page +
            "?filter=" + checkedConnectors, "list", bindPaginationElements, true);
	}
	
	function bindPaginationElements() {
        console.log("binding pagination events");
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

    listWidget.render = render;
	return listWidget;
	
});
