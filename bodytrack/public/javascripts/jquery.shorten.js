{ // a dummy block, so I can collapse all the meta stuff in the editor

/*
 * Shorten, a jQuery plugin to automatically shorten text to fit in a block or a pre-set width and configure how the text ends.
 * Copyright (C) 2009-2011  Marc Diethelm
 * License: (GPL 3, http://www.gnu.org/licenses/gpl-3.0.txt) see license.txt
 */


/****************************************************************************
This jQuery plugin automatically shortens text to fit in a block or pre-set width while you can configure how the text ends. The default is an ellipsis  ("…", &hellip;, Unicode: 2026) but you can use anything you want, including markup.

This is achieved using either of two methods: First the the text width of the 'selected' element (eg. span or div) is measured using Canvas or by placing it inside a temporary table cell. If it's too big to big to fit in the element's parent block it is shortened and measured again until it (and the appended ellipsis or text) fits inside the block. A tooltip on the 'selected' element displays the full original text.

If the browser supports truncating text with CSS ('text-overflow:ellipsis') then that is used (but only if the text to append is the default ellipsis). http://www.w3.org/TR/2003/CR-css3-text-20030514/#text-overflow-props

If the text is truncated by the plugin any markup in the text will be stripped (eg: "<a" starts stripping, "< a" does not). This behaviour is dictated by the jQuery .text(val) method. The appended text may contain HTML however (a link or span for example).


Usage Example ('selecting' a div with an id of "element"):

<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js"></script>
<script type="text/javascript" src="jquery.shorten.js"></script>
<script type="text/javascript">
    $(function() {
        $("#element").shorten();
    });
</script>


By default the plugin will use the parent block's width as the maximum width and an ellipsis as appended text when the text is truncated.

There are three ways of configuring the plugin:

1) Passing a configuration hash as the plugin's argument, eg:

.shorten({
    width: 300,
    tail: ' <a href="#">more</a>',
    tooltip: false
});

2) Using two optional arguments (deprecated!):
width = the desired pixel width, integer
tail = text/html to append when truncating

3) By changing the plugin defaults, eg:


$.fn.shorten.defaults.tail = ' <a href="#">more</a>';


Notes:

There is no default width (unless you create one).

You may want to set the element's CSS to {visibility:hidden;} so it won't
initially flash at full width in slow browsers.

jQuery < 1.4.4: Shorten doesn't work for elements who's parents have display:none, because .width() is broken. (Returns negative values)
http://bugs.jquery.com/ticket/7225
Workarounds:
- Use jQuery 1.4.4+
- Supply a target width in options.
- Use better timing: Don't use display:none when shortening (maybe you can use visibility:hidden). Or shorten after changing display.

Only supports ltr text for now.

Tested with jQuery 1.3+


Based on a creation by M. David Green (www.mdavidgreen.com) in 2009.

Heavily modified/simplified/improved by Marc Diethelm (http://web5.me/).

****************************************************************************/
}


(function ($) {

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
			func = args.callee // dito; and shorter than $.fn.shorten

		if ( args.length ) {

			if ( args[0].constructor == Object ) {
				userOptions = args[0];
			} else if ( args[0] == "options" ) {
				return $(this).eq(0).data(options_identifier);
			} else {
				userOptions = {
					width: parseInt(args[0]),
					tail: args[1]
				}
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
				}

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
		// Get the current text style. This string uses the same syntax as the CSS font specifier. The order matters!
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
