define(["applications/calendar/tabs/diary/Status",
        "core/Tab",
        "applications/calendar/App"], function(status, Tab, Calendar) {
	
	function render(params) {
        params.setTabParam(null);
        this.getTemplate("text!applications/calendar/tabs/diary/" + params.timeUnit.toLowerCase() + "Diary.html", "diary-" + params.timeUnit, function() {
			setup(params.digest, params.timeUnit);
		});
	}
	
	function setup(digest, cEn) {
		status.handleComments();
		App.fullHeight();
		$('textarea.tinymce').tinymce({
			// Location of TinyMCE script
			script_url : '/static/tiny_mce-3.5b1/tiny_mce.js',

			// General options
			theme : "advanced",
			plugins : "style",
			width : "100%",
			height: "95%",

			// Theme options
			theme_advanced_buttons1 : "bold,italic,underline,|,justifyleft,justifycenter,justifyright,justifyfull",
			theme_advanced_buttons2 : "",
			theme_advanced_buttons3 : "",
			theme_advanced_buttons4 : "",
			theme_advanced_toolbar_location : "top",
			theme_advanced_toolbar_align : "left",
			theme_advanced_resizing : false,

			// Example content CSS (should be your site CSS)
			content_css : "/static/tiny_mce-3.5b1/css/content.css"
		});
	}
    var connectorEnabled;

    function connectorToggled(connectorName,objectTypeNames, enabled){
        connectorEnabled[connectorName] = enabled;
        setup(digest,connectorEnabled);
    }

    function connectorDisplayable(connector){
        return false;
    }

    var diaryTab = new Tab("calendar", "diary", "Candide Kemmler", "icon-pencil", true);
	diaryTab.render = render;
    diaryTab.connectorToggled = connectorToggled;
    diaryTab.connectorDisplayable = connectorDisplayable;
    return diaryTab;
	
});
