;(function ($) {

	//var $c = console;
	var
		_native = false,
		is_canvasTextSupported,
		measureContext, // canvas context or table cell
		measureText, // function that measures text width
		info_identifier = "shorten-info",
		options_identifier = "shorten-options";

	$.fn.shorten = function() {

		var userOptions = {},
			args = arguments, // for better minification
			func = args.callee; // dito; and shorter than $.fn.shorten

		if ( args.length ) {

			if ( args[0].constructor == Object ) {
				userOptions = args[0];
			} else if ( args[0] == "options" ) {
				return $(this).eq(0).data(options_identifier);
			} else {
				userOptions = {
					width: parseInt(args[0]),
					tail: args[1]
				};
			}
		}

		this.css("visibility","hidden"); // Hide the element(s) while manipulating them

		// apply options vs. defaults
		var options = $.extend({}, func.defaults, userOptions);


		/**
		 * HERE WE GO!
		 **/
		return this.each(function () {

			var
				$this = $(this),
				text = $this.text(),
				numChars = text.length,
				targetWidth,
				tailText = $("<span/>").html(options.tail).text(), // convert html to text
				tailWidth,
				info = {
					shortened: false,
					textOverflow: false
				};

			if ($this.css("float") != "none") {
				targetWidth = options.width || $this.width(); // this let's correctly shorten text in floats, but fucks up the rest
			} else {
				targetWidth = options.width || $this.parent().width();
			}

			if (targetWidth < 0) { // jQuery versions < 1.4.4 return negative values for .width() if display:none is used.
				//$c.log("nonsense target width ", targetWidth);
				return true;
			}

			$this.data(options_identifier, options);

			// for consistency with the text-overflow method (which requires these properties), but not actually neccessary.
			this.style.display = "block";
			//this.style.overflow = "hidden"; // firefox: a floated li will cause the ul to have a "bottom padding" if this is set.
			this.style.whiteSpace = "nowrap";

			// decide on a method for measuring text width
			if ( is_canvasTextSupported ) {
				//$c.log("canvas");
				measureContext = measureText_initCanvas.call( this );
				measureText = measureText_canvas;

			} else {
				//$c.log("table")
				measureContext = measureText_initTable.call( this );
				measureText = measureText_table;
			}

			var origLength = measureText.call( this, text, measureContext );

                        if ( options.tooltip ) {
               			this.setAttribute("title", text);
               		}

			if ( origLength < targetWidth ) {
				//$c.log("nothing to do");
				$this.text( text );
				this.style.visibility = "visible";

				$this.data(info_identifier, info);

				return true;
			}

			/**
			 * If browser implements text-overflow:ellipsis in CSS and tail is &hellip;/Unicode 8230/(…), use it!
			 * In this case we're doing the measurement above to determine if we need the tooltip.
			 **/
			if ( func._native && !userOptions.width ) {
				//$c.log("css ellipsis");
				var rendered_tail = $("<span>"+options.tail+"</span>").text(); // render tail to find out if it's the ellipsis character.

				if ( rendered_tail.length == 1 && rendered_tail.charCodeAt(0) == 8230 ) {

					$this.text( text );

					// the following three properties are needed for text-overflow to work (tested in Chrome).
					// for consistency now I need to set this everywhere... which probably interferes with users' layout...
					//this.style.whiteSpace = "nowrap";
					this.style.overflow = "hidden";
					//this.style.display = "block";

					this.style[func._native] = "ellipsis";
					this.style.visibility = "visible";

					info.shortened = true;
					info.textOverflow = "ellipsis";
					$this.data(info_identifier, info);

					return true;
				}
			}

			tailWidth = measureText.call( this, tailText, measureContext ); // convert html to text and measure it
			targetWidth = targetWidth - tailWidth;

				//$c.log(text +" + "+ tailText);

			/**
			 * Before we start removing characters one by one, let's try to be more intelligent about this:
			 * If the original string is longer than targetWidth by at least 15% (for safety), then shorten it
			 * to targetWidth + 15% (and re-measure for safety). If the resulting text still is too long (as expected),
			 * use that for further shortening. Else use the original text. This saves a lot of time for text that is
			 * much longer than the desired width.
			 */
			var safeGuess = targetWidth * 1.15; // add 15% to targetWidth for safety before making the cut.

			if ( origLength - safeGuess > 0 ) { // if it's safe to cut, do it.

				var cut_ratio = safeGuess / origLength,
					num_guessText_chars = Math.ceil( numChars * cut_ratio ),
					// looking good: shorten and measure
					guessText = text.substring(0, num_guessText_chars),
					guessTextLength = measureText.call( this, guessText, measureContext );

					//$c.info("safe guess: remove " + (numChars - num_guessText_chars) +" chars");

				if ( guessTextLength > targetWidth ) { // make sure it's not too short!
					text = guessText;
					numChars = text.length;
				}
			}

			// Remove characters one by one until text width <= targetWidth
				//var count = 0;
			do {
				numChars--;
				text = text.substring(0, numChars);
					//count++;
			} while ( measureText.call( this, text, measureContext ) >= targetWidth );

			$this.html( $.trim( $("<span/>").text(text).html() ) + options.tail );
			this.style.visibility = "visible";
				//$c.info(count + " normal truncating cycles...")
				//$c.log("----------------------------------------------------------------------");

			info.shortened = true;
			$this.data(info_identifier, info);

			return true;
		});

		return true;

	};



	var css = document.documentElement.style;

	if ( "textOverflow" in css ) {
		_native = "textOverflow";
	} else if ( "OTextOverflow" in css ) {
		_native = "OTextOverflow";
	}

		// test for canvas support

	if ( typeof Modernizr != 'undefined' && Modernizr.canvastext ) { // if Modernizr has tested for this already use that.
		is_canvasTextSupported = Modernizr.canvastext;
	} else {
		var canvas = document.createElement("canvas");
		is_canvasTextSupported = !!(canvas.getContext && canvas.getContext("2d") && (typeof canvas.getContext("2d").fillText === 'function'));
	}
	
	$.fn.shorten._is_canvasTextSupported = is_canvasTextSupported;
	$.fn.shorten._native = _native;



	function measureText_initCanvas()
	{
		var $this = $(this);
		var canvas = document.createElement("canvas");
			//scanvas.setAttribute("width", 500); canvas.setAttribute("height", 40);
		ctx = canvas.getContext("2d");
		$this.html( canvas );

		/* the rounding is experimental. it fixes a problem with a font size specified as 0.7em which resulted in a computed size of 11.2px.
		  without rounding the measured font was too small. even with rounding the result differs slightly from the table method's results. */
		// Get the current text style. This string uses the same syntax as the CSS font specifier. The ordering matters!
		ctx.font = $this.css("font-style") +" "+ $this.css("font-variant") +" "+ $this.css("font-weight") +" "+ Math.ceil(parseFloat($this.css("font-size"))) +"px "+ $this.css("font-family");

		return ctx;
	}

	// measurement using canvas
	function measureText_canvas( text, ctx )
	{
			//ctx.fillStyle = "red"; ctx.fillRect (0, 0, 500, 40);
			//ctx.fillStyle = "black"; ctx.fillText(text, 0, 12);

		return ctx.measureText(text).width; // crucial, fast but called too often
	};

	function measureText_initTable()
	{
		var css = "padding:0; margin:0; border:none; font:inherit;";
		var $table = $('<table style="'+ css +'width:auto;zoom:1;position:absolute;"><tr style="'+ css +'"><td style="'+ css +'white-space:nowrap;"></td></tr></table>');
		$td = $("td", $table);

		$(this).html( $table );

		return $td;
	};

	// measurement using table
	function measureText_table( text, $td )
	{
		$td.text( text );

		return $td.width(); // crucial but expensive
	};


	$.fn.shorten.defaults = {
		tail: "&hellip;",
		tooltip: true
	};

})(jQuery);
