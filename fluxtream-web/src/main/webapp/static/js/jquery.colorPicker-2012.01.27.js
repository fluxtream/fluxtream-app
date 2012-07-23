/**
 * Really Simple Color Picker in jQuery
 *
 * Copyright (c) 2008 Lakshan Perera (www.laktek.com)
 * Licensed under the MIT (MIT-LICENSE.txt)  licenses.
 *
 * Chris Bartley <bartley@cmu.edu> made these modifications on 2012.01.27:
 * - closes when user hits ESC key
 * - tighter UI
 * - changed trigger graphic (now using a modified version of the one at http://www.eyecon.ro/colorpicker/)
 * - no longer display hex value or allow custom values
 * - the #color_selector div only gets created once and is shared amongst all pickers on the page
 * - fixed javascript syntax errors
 * - changed the default set of colors
 */

(function($) {
  $.fn.colorPicker = function() {
    if (this.length > 0) {
      buildSelector();
    }
    return this.each(function() {
      buildPicker(this)
    });
  };

  var selectorOwner;
  var selectorShowing = false;
  var nextColorIndex = 0;

  var buildPicker = function(element) {
    //build color picker
    var control = $("<div class='color_picker'>&nbsp;</div>");
    control.css('background-color', $(element).val());

    //bind click event to color picker
    control.bind("click", toggleSelector);

    //add the color picker section
    $(element).after(control);

    //add event listener to input box
    $(element).bind("change", function() {
      var selectedValue = toHex($(element).val());
      $(element).next(".color_picker").css("background-color", selectedValue);
    });

    //hide the input box
    $(element).hide();

  };

  var buildSelector = function() {
    // see whether we've already created a selector--if so, don't bother creating a new one
    var selector = $("#color_selector");
    if (typeof selector.get(0) === 'undefined') {
      selector = $("<div id='color_selector'></div>");

      //add color pallete
      $.each($.fn.colorPicker.defaultColors, function() {
        var swatch = $("<div class='color_swatch'>&nbsp;</div>");
        swatch.css("background-color", "#" + this);
        swatch.bind("click", function() {
          changeColor($(this).css("background-color"))
        });
        swatch.bind("mouseover", function() {
          $(this).css("border-color", "#666666");
        });
        swatch.bind("mouseout", function() {
          $(this).css("border-color", "#000000");
        });

        swatch.appendTo(selector);
      });

      $("body").append(selector);
    }
  };

  var checkMouse = function(event) {
    //check the click was on selector itself or on selectorOwner
    var selector = "div#color_selector";
    var selectorParent = $(event.target).parents(selector).length;
    if (event.target == $(selector)[0] || event.target == selectorOwner || selectorParent > 0) {
      return
    }

    hideSelector();
  };

  var hideSelector = function() {
    $(document).unbind('keyup', keyUpEventHandler);
    var selector = $("div#color_selector");

    $(document).unbind("mousedown", checkMouse);
    selector.fadeOut(100);
    selectorShowing = false
  };

  var keyUpEventHandler = function(e) {
    if (e.keyCode == 27) {
      hideSelector();
    }
  };

  var showSelector = function() {
    $(document).bind('keyup', keyUpEventHandler);
    var selector = $("div#color_selector");

    selector.css({
                   top  : $(selectorOwner).offset().top + ($(selectorOwner).outerHeight()),
                   left : $(selectorOwner).offset().left
                 });
    selector.fadeIn(100);

    //bind close event handler
    $(document).bind("mousedown", checkMouse);
    selectorShowing = true
  };

  var toggleSelector = function() {
    selectorOwner = this;
    selectorShowing ? hideSelector() : showSelector();
  };

  var changeColor = function(value) {
    var selectedValue = toHex(value);
    if (selectedValue) {
      $(selectorOwner).css("background-color", selectedValue);
      $(selectorOwner).prev("input").val(selectedValue).change();

      //close the selector
      hideSelector();
    }
  };

  //converts RGB string to HEX - inspired by http://code.google.com/p/jquery-color-utils
  var toHex = function(color) {
    //valid HEX code is entered
    if (color.match(/[0-9a-fA-F]{3}$/) || color.match(/[0-9a-fA-F]{6}$/)) {
      color = (color.charAt(0) == "#") ? color : ("#" + color);
    }
    //rgb color value is entered (by selecting a swatch)
    else if (color.match(/^rgb\(([0-9]|[1-9][0-9]|[1][0-9]{2}|[2][0-4][0-9]|[2][5][0-5]),[ ]{0,1}([0-9]|[1-9][0-9]|[1][0-9]{2}|[2][0-4][0-9]|[2][5][0-5]),[ ]{0,1}([0-9]|[1-9][0-9]|[1][0-9]{2}|[2][0-4][0-9]|[2][5][0-5])\)$/)) {
      var c = ([parseInt(RegExp.$1), parseInt(RegExp.$2), parseInt(RegExp.$3)]);

      var pad = function(str) {
        if (str.length < 2) {
          for (var i = 0, len = 2 - str.length; i < len; i++) {
            str = '0' + str;
          }
        }
        return str;
      };

      if (c.length == 3) {
        var r = pad(c[0].toString(16)), g = pad(c[1].toString(16)), b = pad(c[2].toString(16));
        color = '#' + r + g + b;
      }
    }
    else {
      color = false;
    }

    return color
  };

  //public methods
  $.fn.colorPicker.addColors = function(colorArray) {
    $.fn.colorPicker.defaultColors = $.fn.colorPicker.defaultColors.concat(colorArray);
  };

  $.fn.colorPicker.getNextColor = function() {
    if (nextColorIndex >= $.fn.colorPicker.cycleColors.length) {
      nextColorIndex = 0;
    }
    return $.fn.colorPicker.cycleColors[nextColorIndex++];
  };

  $.fn.colorPicker.resetGetNextColor = function() {
    nextColorIndex = 0;
  };

  $.fn.colorPicker.cycleColors =
  [
    'ff0000', '00dd00', '0000ff',                     'ff00ff', '0099ff', '0066ff',
    '990000', '009900', '000099', '990099', '999900', '9900ff', '009999', '6600ff',
    '660000', '006600', '000066', '666600', '006666', '660066', 'ff0099', 'ff0066',
    'ab2671', '9643a5', '4585a3', '41a587', 'd1bc36', 'ff9900', 'ff6600',
    '58b442', '7ec225', 'a7b828', 'cf9911', 'd47f1e', 'b56414', '914d14', '777777',
    'c7561e', 'b5515d', 'c244ab', '603f99', '536ca6', '3640ad', '3c995b', '444444',
    'd96666', 'e67399', 'b373b3', '8c66d9', '668cb3', '4a86e8', '59bfb3', '000000'
  ];

  $.fn.colorPicker.defaultColors =
  [
    'ff0000', '00dd00', '0000ff', 'dddd00', '00dddd', 'ff00ff', '0099ff', '0066ff',
    '990000', '009900', '000099', '999900', '009999', '990099', '9900ff', '6600ff',
    '660000', '006600', '000066', '666600', '006666', '660066', 'ff0099', 'ff0066',
    'ab2671', '9643a5', '4585a3', '41a587', 'd1bc36', 'ff9900', 'ff6600', 'aaaaaa',
    '58b442', '7ec225', 'a7b828', 'cf9911', 'd47f1e', 'b56414', '914d14', '777777',
    'c7561e', 'b5515d', 'c244ab', '603f99', '536ca6', '3640ad', '3c995b', '444444',
    'd96666', 'e67399', 'b373b3', '8c66d9', '668cb3', '4a86e8', '59bfb3', '000000'
  ];
})(jQuery);


