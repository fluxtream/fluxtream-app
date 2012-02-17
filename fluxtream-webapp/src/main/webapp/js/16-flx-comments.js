
function editEventComment(comment) {
	var commentText = $(comment).find('.CommContent').html(),
		inputField = $(comment).parent().find(".commentList.placeholder");
	$(comment).parent().find(".listViewItemCommNew").show();
	$(comment).hide();
	inputField.focus();
	inputField.val(commentText);
}

function toggleCommentExpandCollapse() {
	if ($(".mainComment").hasClass("editing"))
		setTimeout("doToggleCommentExpandCollapse()", 200);
	doToggleCommentExpandCollapse();
}

function doToggleCommentExpandCollapse() {
	if ($('#commentMain').hasClass("collapsed"))
		expandComment();
	else
		collapseComment();
}

function updateToggleWidget() {
	if ($('#commentMain').hasClass("expanded"))
		$(".iconExpandCollapse").removeClass("collapsed").addClass("expanded");
	else
		$(".iconExpandCollapse").removeClass("expanded").addClass("collapsed");
}

function resetMainComment(expanded) {
	$(".mainComment").removeClass("editing");
	if($("#mainComment").hasClass("expanded")) return;
	$(".belowDate").removeClass("pointerCommFocus").addClass("pointerComm");
}

function handleComments() {
    $('#commentMain').html($('#commentMain').attr("title"));
    collapseComment();
}

function expandComment() {
	if ($(".mainComment").hasClass("editing")) return;
	$("#commentMain").unbind();
	$.ajax({url:"/diary/get/titleAndBody",
		success : function(comment) {
			$("#commentMain").empty();
			$("#commentMain").removeClass("collapsed").addClass("expanded");
			$("#commentMain").append("<div id=\"commentTitle\">" + comment.title + "</div>");
		    $('#commentTitle').editable("/diary/set/title", {
		    	loadtype: "POST",
		    	name : "commentTitle",
		    	style: "inherit",
		    	callback: function(){
		    		resetMainComment(true);
		            window.createGrowl(false);
		    	}
		    });
			$("#commentMain").append("<div id=\"commentContent\">" + comment.body + "</div>");
			new nicEditor({buttonList : ['italic','bold','underline','ol','ul'],
				iconsPath: "/static/images/nicEditorIcons.gif"}).panelInstance('commentContent');
			updateToggleWidget();
			$("#commentMain").append("<div class=\"commentButtons\">" +
					"<a class=\"btn primary\" href=\"javascript:saveCommentBody()\">Save</a>" +
					"</div>");
			$("#commentContent").focus(function() {
				$(".mainComment").addClass("editing");
				$(".belowDate").addClass("pointerCommFocus").removeClass("pointerComm");
			});
			$("#commentContent").blur(function() {
				$(".mainComment").removeClass("editing")
				$(".belowDate").addClass("pointerComm").removeClass("pointerCommFocus");
				saveCommentBody();
			});
		}
	});
}

function saveCommentBody() {
	$.ajax({
		url:"/diary/set/body",
		type: "POST",
		data: { commentBody : $("#commentContent").html()},
		success: function() {
            window.createGrowl(false);
		}
	});
}

function collapseComment() {
	if ($(".mainComment").hasClass("editing")) return;
	$.ajax({url:"/diary/get/title",
		success: function(comment) {
			$("#commentMain").css("min-height", "20px");
			$("#commentMain").empty();
			$("#commentMain").html(comment);
			$("#commentMain").removeClass("expanded").addClass("collapsed");
			updateToggleWidget();
			$("#commentMain").unbind();
		    $('#commentMain').editable("/diary/set/title", {
		    	loadtype: "POST",
		    	loadurl: "/diary/get/title",
		    	name : "commentTitle",
		    	style: "inherit",
		    	callback: function(){
		            window.createGrowl(false);
					$(".mainComment").removeClass("editing");
		    		resetMainComment();
		    	}
		    });
		}
	});
}

function setEventComment(input) {
	if ($(input).val()=="")
		$(input).val("Comment this event...");
	else {
		$.ajaxSetup({ 
	        scriptCharset: "utf-8" , 
		});
		var listViewItem = $(input).parent().parent().parent(),
			type = listViewItem.attr("type"),
			id = listViewItem.attr("id");
        $.ajax({
        	url:"/me/setEventComment.json",
        	type: "POST",
        	data: {
        		commentText : $(input).val(),
        		type : type,
        		id : id
        	}
        });
		$(input).parent().parent().find("article").html($(input).val());
		$(input).parent().parent().find("article").show();
		$(input).parent().hide();
	}
}


function handleMouseEvents() {
	$('.commentList.placeholder').live('focus', function () {
		if ($(this).hasClass("empty"))
			$(this).val("");
	});
	$('.commentList.placeholder').live('blur', function () {setEventComment(this);});
	$('#listView .listViewItem').live('mouseover',
		function () {
			$(this).find(".listViewAddCom").css({'display':'block'});
  	}
	);
	$('#listView .listViewItem').live('mouseleave',
  	function () {
    	$(this).find(".listViewAddCom").css({'display':'none'});
  	}
	);
	$('.listViewItem .iconComment').live('click',
  	function () {
    	$(this).parent().parent().find(".listViewItemComm").css({'display':'block'});
  	}
	);
	$('.listViewItem .iconCommDelete').live('click',
  	function () {
    	$(this).parent().parent().css({'display':'none'});
  	}
	);	
	
}
