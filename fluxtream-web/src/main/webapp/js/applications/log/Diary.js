define(function() {

	function handleComments() {
		$(".diaryTitle").html($(".diaryTitle").attr("title"));
		$.ajax({
			url : "/diary/get/title",
			success : function(comment) {
				$(".diaryTitle").css("min-height", "20px");
				$(".diaryTitle").empty();
				$(".diaryTitle").val(comment);
				$(".diaryTitle").unbind();
				$(".diaryTitle").keypress(function(e) {
		            code = (e.keyCode ? e.keyCode : e.which);
		            if (code == 13) 
		            	setDiaryTitle($(".diaryTitle").val());
				});
			}
		});
	}
	
	function setDiaryTitle(title) {
		$.ajax({
			url: "/diary/set/title",
			data: {"title": title},
			type : "POST",
			statusCode: {
				200: function() {
					applause(false);
				}
			}
		});
	}

	function applause(persistent) {
		// Use the last visible jGrowl qtip as our positioning target
		var target = $('.qtip.jgrowl:visible:last');

		var praise = [ "Great Prose", "Good job!", "This is Great",
				"I love that", "Well said", "You bet!", "Absolutely right",
				"Bellissimo!" ];
		var randomnumber = Math.floor(Math.random() * 8);

		// Create your jGrowl qTip...
		$(window).qtip({
			// Any content config you want here really.... go wild!
			content : {
				text : praise[randomnumber],
			},
			position : {
				my : 'top right', // Not really important...
				at : (target.length ? 'bottom' : 'top') + ' right',
				target : target.length ? target : $(window),

				adjust : {
					y : 5,
					x : -5
				}
			// Add some vertical spacing
			},
			show : {
				event : false, // Don't show it on a regular event
				ready : true, // Show it when ready (rendered)
				effect : function() {
					$(this).stop(0, 1).fadeIn(400);
				}, // Matches the hide effect
				delay : 0, // Needed to prevent positioning issues

				// Custom option for use with the .get()/.set() API,
				// awesome!
				persistent : persistent
			},
			hide : {
				event : false, // Don't hide it on a regular event
				effect : function(api) {
					// Do a regular fadeOut, but add some spice!
					$(this).stop(0, 1).fadeOut(400).queue(function() {
						// Destroy this tooltip after fading
						// out
						api.destroy();

						// Update positions
						updateGrowls();
					})
				}
			},
			style : {
				classes : 'jgrowl ui-tooltip-dark ui-tooltip-rounded',
				tip : false
			},
			events : {
				render : function(event, api) {
					timer.call(api.elements.tooltip, event);
				}
			}
		}).removeData('qtip');
	};

	window.updateGrowls = function() {
		// Loop over each jGrowl qTip
		var each = $('.qtip.jgrowl:not(:animated)');
		each.each(function(i) {
			var api = $(this).data('qtip');

			// Set the target option directly to prevent reposition() from being
			// called twice.
			api.options.position.target = !i ? $(document.body) : each.eq(i - 1);
			api.set('position.at', (!i ? 'top' : 'bottom') + ' right');
		});
	};

	function timer(event) {
		var api = $(this).data('qtip'), lifespan = 3000; // 5 second lifespan

		// If persistent is set to true, don't do anything.
		if (api.get('show.persistent') === true) {
			return;
		}

		// Otherwise, start/clear the timer depending on event type
		clearTimeout(api.timer);
		if (event.type !== 'mouseover') {
			api.timer = setTimeout(api.hide, lifespan);
		}
	}
	
	var Diary = {};

	Diary.handleComments = handleComments;

	return Diary;
})
