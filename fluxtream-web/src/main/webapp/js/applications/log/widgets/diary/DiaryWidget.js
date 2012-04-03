define(["applications/log/widgets/diary/Status",
        "applications/log/widgets/Widget",
        "applications/log/App"], function(status, Widget, Log) {
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/diary/" + timeUnit.toLowerCase() + "Diary.html"], function(template) {
			$("#widgets").append(template);
			status.handleComments();
			$('textarea.tinymce').tinymce({
				// Location of TinyMCE script
				script_url : '/static/tiny_mce/tiny_mce.js',

				// General options
				theme : "advanced",
				plugins : "style",
				width : "100%",
				height: "100%",

				// Theme options
				theme_advanced_buttons1 : "save,newdocument,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,styleselect,formatselect,fontselect,fontsizeselect",
				theme_advanced_buttons2 : "",
				theme_advanced_buttons3 : "",
				theme_advanced_buttons4 : "",
				theme_advanced_toolbar_location : "top",
				theme_advanced_toolbar_align : "left",
				theme_advanced_statusbar_location : "bottom",
				theme_advanced_resizing : true,

				// Example content CSS (should be your site CSS)
				content_css : "/static/tiny_mce/css/content.css",
			});
			
			Log.fullHeight();
		});
	}
	
	var diaryWidget = new Widget("clock", "Candide Kemmler", "icon-pencil");
	diaryWidget.render = render;
	return diaryWidget;
	
});
