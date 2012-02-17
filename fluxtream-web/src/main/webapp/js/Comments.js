
define([], function() {

	function editEventComment(comment) {
		var commentText = $(comment).find('.CommContent').html(),
			inputField = $(comment).parent().find(".commentList.placeholder");
		$(comment).parent().find(".listViewItemCommNew").show();
		$(comment).hide();
		inputField.focus();
		inputField.val(commentText);
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
	
	var Comments = {};
	return Comments;
	
})
