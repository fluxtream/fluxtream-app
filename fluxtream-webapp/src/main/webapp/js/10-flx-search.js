function search() {
	setVisualizationType("SEARCH");
	$.get('/views/search', function(view) {
		$('#mainCont').html(view);
		$("#footer").show();
		updateVisualization();
	});
}

function getSearchResults(page) {
	var q = $("#searchBox").val();
	$.ajax({ url: "/me/search/"+page,
		data: {"q" : q},
		dataType: "html",
		success: function(html) {
			$("#facetsList").html(html);
		}
	});
}
