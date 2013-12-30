define(["core/grapher/BTCore", "core/grapher/Grapher"],function(BTCore, Grapher){

    var PREFS 		= BTCore.PREFS;
    var TOOLS 		= BTCore.TOOLS;
    var TAG_MANAGER = BTCore.TAG_MANAGER;
    var carouselTemplate;
    var photoCache;

    App.loadMustacheTemplate("applications/calendar/tabs/photos/photosTemplate.html","carousel",function(template){
        carouselTemplate = template;
    });

    App.loadMustacheTemplate("core/grapher/timelineTemplates.html",
        "_timeline_photo_dialog_template",
        function(template) {
            var html = template.render({"photoUrl" : ""});
            $("body").remove("#photoDialog");
            $("body").prepend("<div id='photoDialog'>" + html + "</div>");
            $("#photoDialog")['dialog'](
                {
                    autoOpen  : false,
                    modal     : true,
                    width     : 'auto',
                    height    : 'auto',
                    minWidth  : 340,
                    resizable : false
                }
            );
        }
    );

    var handleResize = function(){

    };

    var handleKeyEvent = handleResize;

    var orientationStyles = ["none",//1
                             "scale(-1,1)",//2
                             "rotate(180deg)",//3
                             "rotate(180deg) scale(-1,1)",//4
                             "rotate(90deg) scale(1,-1)",//5
                             "rotate(90deg)",//6
                             "rotate(270deg) scale(1,-1)",//7
                             "rotate(270deg)"//8
    ];

    function getCarouselHTML(digest,connectorNames){  //TODO: make this work without a digest
        if (connectorNames == null){//if the parameter is discluded, show all images
            connectorNames = [];
            for (connectorName in digest.cachedData){
                if (digest.cachedData[connectorName].hasImages)
                    connectorNames[connectorNames.length] = connectorName;
            }
        }
        var noPhotos = true;
        for (var i = 0; i < connectorNames.length && noPhotos; i++){
            noPhotos = digest.cachedData[connectorNames[i]] == null || !digest.cachedData[connectorNames[i]].hasImages;
        }
        if (noPhotos)
            return null;
        var data = [];
        for (var i = 0; i < connectorNames.length; i++){
            for (var j = 0; digest.cachedData[connectorNames[i]] != null && digest.cachedData[connectorNames[i]].hasImages && j < digest.cachedData[connectorNames[i]].length; j++){
                for (var k = 0; digest.cachedData[connectorNames[i]][j].hasImage && k <= data.length; k++){
                    if (k == data.length || data[k].start > digest.cachedData[connectorNames[i]][j].start || digest.cachedData[connectorNames[i]][j].start == null){
                        data.splice(k,0,digest.cachedData[connectorNames[i]][j]);
                        if (data[k].orientation == null)
                            data[k].orientation = 1;
                        data[k].transformations = orientationStyles[data[k].orientation-1];
                        break;
                    }
                }
            }
        }
        for (var i = 0; i < data.length; i++){
            data[i].active = i == 0;
        }
        return carouselTemplate.render({photos:data,includeNav:data.length > 1});
    }

    function isSideways(orientation){
        return orientation >= 5 && orientation <= 8;
    }

    function getNegationFactor(orientation){
        return orientation >= 7 && orientation <= 8 ? -1 : 1;
    }

    function getNegationFactorHorizontal(orientation){
        return  (orientation == 5 || orientation == 8) ? -1 : 1;
    }

    function doResize(imageId){
        var container = $("#photoViewContainer");
        var photoHolder = container.find(".photoHolder");
        var image = container.find("#photo-" + imageId).find("img");
        var fullSizeImage = photoHolder.find("." + image.attr("fullsize"));
        var doResize = function(image,fullSizeImage){
            if (fullSizeImage.width() == 0 || fullSizeImage.height() == 0){
                setTimeout(function(){
                    doResize(image,fullSizeImage);
                },100);
                return;
            }
            var cWidth = container.width();
            var cHeight = container.height();
            var ratio = fullSizeImage.width() / fullSizeImage.height();
            var cRatio = cWidth / cHeight;
            var orientation = image.attr("orientation");
            image.css("max-height","none");
            image.css("max-width","none");
            var extraTransformations = "";
            var height, width;
            var marginTop;
            var forceFullSize = container.find(".carousel-inner").hasClass("fullSizeImages");
            if (isSideways(orientation)){
                if (ratio < cRatio){
                    if (forceFullSize || Height < fullSizeImage.width()){
                        width = cHeight;
                        height = cHeight / ratio;
                    }
                    else{
                        width = fullSizeImage.width();
                        height = fullSizeImage.height();
                    }
                    extraTransformations = "translateX(" + getNegationFactor(orientation) * ((width - height) / 2) + "px)";
                }
                else{
                    var height, width;
                    if (forceFullSize || cWidth < fullSizeImage.height()){
                        width = cWidth * ratio;
                        height = cWidth;
                    }
                    else{
                        width = fullSizeImage.width();
                        height = fullSizeImage.height();
                    }
                    extraTransformations = "translateX(" + getNegationFactor(orientation) * ((width - height) / 2) + "px)";
                    extraTransformations += "translateY(" + getNegationFactorHorizontal(orientation) * ((width - height) / 2) + "px)";

                }
                marginTop = (cHeight - width) / 2;
            }
            else{
                if (ratio < cRatio){
                    var height, width;
                    if (forceFullSize || cHeight < fullSizeImage.height()){
                        height = cHeight;
                        width = ratio * cHeight;
                    }
                    else{
                        width = fullSizeImage.width();
                        height = fullSizeImage.height();
                    }
                }
                else{
                    if (forceFullSize || cWidth < fullSizeImage.width()){
                        height = cWidth / ratio;
                        width = cWidth;
                    }
                    else{
                        width = fullSizeImage.width();
                        height = fullSizeImage.height();
                    }
                }
                marginTop = (cHeight - height) / 2;
            }
            image.width(width);
            image.height(height);
            var fullTransformation = image.attr("transformations") + " " + extraTransformations;
            image.css("transform",fullTransformation);
            image.css("-ms-transform",fullTransformation);
            image.css("-webkit-transform",fullTransformation);
            image.css("-o-transform",fullTransformation);
            image.css("margin-top",marginTop + "px");

        }
        doResize(image,fullSizeImage);
    }

    function showCarouselHTML(html,defaultImage){
        var widget = $(html);
        var items = widget.find(".carousel-inner .item");
        var photoIdToIndex = {};
        var indexToPhotoId = [];
        for (var i = 0, li = items.length; i < li; i++){
            var photoId = $(items[i]).attr("photoId");
            photoIdToIndex[photoId] = i;
            indexToPhotoId[i] = photoId;
        }
        $("body").append(widget);
        var oldTop = $('body').scrollTop();
        $("body").addClass("photoCarouselViewing");

        if (defaultImage == null)
            defaultImage = 0;
        else
            defaultImage = photoIdToIndex[defaultImage];

        function showImage(){
            doResize(indexToPhotoId[defaultImage]);
            widget.find(".carousel-inner div.item.active").removeClass("active");
            widget.find(".carousel-inner #photo-"+indexToPhotoId[defaultImage]).addClass("active");
        }
        showImage();

        widget.find(".carousel-control.left").click(function(){
            defaultImage--;
            if (defaultImage < 0)
                defaultImage += indexToPhotoId.length;
            showImage();
            return false;
        });

        widget.find(".carousel-control.right").click(function(){
            defaultImage++;
            if (defaultImage >= indexToPhotoId.length)
                defaultImage -= indexToPhotoId.length;
            showImage();
            return false;
        });

        widget.find("#closePhotoViewer").click(function(){
            widget.remove();
            handleKeyEvent = handleResize = function(){};
            $("body").removeClass("photoCarouselViewing");
            $("body").scrollTop(oldTop);
            return false;
        });

        handleResize = function(){
            doResize(indexToPhotoId[defaultImage]);
        }

        handleKeyEvent = function(event){
            switch (event.keyCode){
                case 32://space
                case 39://right
                    widget.find(".carousel-control.right").click();
                    break;
                case 37://left
                    widget.find(".carousel-control.left").click();
                    break;
                case 27://escape
                    widget.find("#closePhotoViewer").click();
                    break;
            }
            console.log(event.keyCode);
        }

        widget.find(".carousel-inner img").click(function(){
            if (widget.find(".carousel-inner").hasClass("fullSizeImages"))
                widget.find(".carousel-inner").removeClass("fullSizeImages");
            else
                widget.find(".carousel-inner").addClass("fullSizeImages");
            handleResize();
        });
    }

    $(window).resize(function(){
        handleResize();
    });

    $(window).keydown(function(event){
        handleKeyEvent(event);
    });

    function getUserSelectedTags() {
        var tags = [];
        $.each($("#_timeline_photo_dialog_tags_editor .tagedit-listelement-old input"),
            function(index, inputElement) {
                var val = inputElement['value'];
                if (typeof val === 'string' && val != '') {
                    tags[tags.length] = val;
                }
            });
        return tags;
    };

    function showPhotoDialog(deviceName, channelName, id, timestamp) {
        console.log(deviceName + "/" + channelName + "/" + id + "/" + timestamp);
        photoCache = Grapher.createPhotoDialogCache(deviceName, channelName, [], "any");

        photoCache.initialize(deviceName + "." + channelName + "." + id,
            timestamp/1000,
            function() {
                createPhotoDialog(deviceName + "." + channelName + "." + id,
                    timestamp/1000,
                    function() {
                        centerPhotoDialog();
                    });
            });

        $("#photoDialog").html(App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_loading_template").render({}));
        $("#photoDialog")['dialog']("option", "position", 'center');
        $("#photoDialog")['dialog']('open');
    }


    function centerPhotoDialog() {
        // center the dialog
        $("#photoDialog")['dialog']("option", "position", 'center');
    }

    var createPhotoDialog = function(compoundPhotoId, timestamp, completionCallback) {

        console.log("photoCache");
        console.log(photoCache);

        var photoMetadata = photoCache.getPhotoMetadata(compoundPhotoId);
        var thumbnails = photoMetadata['thumbnails'];
        // This assumes the thumbnails are ordered from smallest to largest.  Might be better to eventually search for the largest.
        var mediumResImageUrl = (thumbnails != null && thumbnails.length > 0) ? thumbnails[thumbnails.length - 1]['url'] : photoMetadata['url'];
        var highResImageUrl = photoMetadata['url'];
        var photoOrientation = photoMetadata['orientation'];
        if (typeof photoOrientation === 'undefined' || photoOrientation == null) {
            photoOrientation = 1;
        }
        var highResOrientationCssClass = "_timeline_photo_dialog_image_orientation_" + photoOrientation;
        var photoDialogTemplate = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_template");
        var photoDialogHtml = photoDialogTemplate.render({"photoUrl" : mediumResImageUrl});
        $("#photoDialog").html(photoDialogHtml);

        var updateGoToNeighborOnSaveWidgets = function() {
            var isEnabled = $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor").is(':checked');
            var direction = TOOLS.parseInt($("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor_choice").val(),0);
            PREFS.set("photo_dialog.goto_neighbor_on_save.enabled", isEnabled);
            PREFS.set("photo_dialog.goto_neighbor_on_save.direction", direction);

            if (isEnabled) {
                $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor_choice").removeAttr("disabled");
                $("#photoDialog #_timeline_photo_dialog_save_preferences label").css("color", "#000000");
                if (direction < 0) {
                    $("#photoDialog #_timeline_photo_dialog_save_button").html("Save &amp; Previous");
                } else {
                    $("#photoDialog #_timeline_photo_dialog_save_button").html("Save &amp; Next");
                }
            } else {
                $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor_choice").attr("disabled", "disabled");
                $("#photoDialog #_timeline_photo_dialog_save_preferences label").css("color", "#aaaaaa");
                $("#photoDialog #_timeline_photo_dialog_save_button").text("Save");
            }
        };

        // set the widgets for the Save button behavior based on saved prefs
        var goToNeighborOnSaveEnabled = !!PREFS.get("photo_dialog.goto_neighbor_on_save.enabled", false);
        var goToNeighborOnSaveDirection = TOOLS.parseInt(PREFS.get("photo_dialog.goto_neighbor_on_save.direction", 0), 0);
        $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor").prop("checked", goToNeighborOnSaveEnabled);
        $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor").change(updateGoToNeighborOnSaveWidgets);
        $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor_choice").val(goToNeighborOnSaveDirection == 0 ? 1 : goToNeighborOnSaveDirection);
        $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor_choice").change(updateGoToNeighborOnSaveWidgets);

        // display Loading status message
        $("#photoDialog #_timeline_photo_dialog_form_status").text("Loading...").show();

        // set previous and next buttons initially hidden
        $("#photoDialog #_timeline_photo_dialog_previous_button").hide();
        $("#photoDialog #_timeline_photo_dialog_next_button").hide();

        // Fetch the metadata for the preceding, following, and current photos from the cache.
        var previousPhotoMetadata = photoCache.getPreviousPhotoMetadata(compoundPhotoId);
        var nextPhotoMetadata = photoCache.getNextPhotoMetadata(compoundPhotoId);
        var isPreviousPhoto = previousPhotoMetadata != null &&
                              typeof previousPhotoMetadata !== 'undefined' &&
                              typeof previousPhotoMetadata['photoId'] !== 'undefined';
        if (isPreviousPhoto) {
            $("#photoDialog #_timeline_photo_dialog_previous_button").show().click(function() {
                createPhotoDialog(previousPhotoMetadata['photoId'],
                    previousPhotoMetadata['timestamp']);
            });
        }

        var isNextPhoto = nextPhotoMetadata != null &&
                          typeof nextPhotoMetadata !== 'undefined' &&
                          typeof nextPhotoMetadata['photoId'] !== 'undefined';
        if (isNextPhoto) {
            $("#photoDialog #_timeline_photo_dialog_next_button").show().click(function() {
                createPhotoDialog(nextPhotoMetadata['photoId'],
                    nextPhotoMetadata['timestamp']);
            });
        }

        // treat undefined or null comment as an empty comment
        if (typeof photoMetadata['comment'] === 'undefined' || photoMetadata['comment'] == null) {
            photoMetadata['comment'] = '';
        }

        // treat undefined or null tags as an empty array
        if (typeof photoMetadata['tags'] === 'undefined' || photoMetadata['tags'] == null) {
            photoMetadata['tags'] = [];
        }

        // add click handler for photo to allow viewing of high-res version
        $("#photoDialog #_timeline_photo_dialog_image").click(function() {
            var theImage = $(this);
            var formContainer = $("#photoDialog #_timeline_photo_dialog_form_container");
            if ($("#photoDialog #_timeline_photo_dialog_form_container").is(":visible")) {
                // fade out the form and show the hi-res version of the image
                formContainer.fadeOut(100, function() {
                    var imageAspectRatio = theImage.width() / theImage.height();
                    var imageHeight = $("body").height() - 60;
                    var imageWidth = imageAspectRatio * imageHeight;

                    // make sure the image isn't too wide now
                    if (imageWidth > $("body").width()) {
                        imageWidth = $("body").width() - 100;
                        imageHeight = imageWidth / imageAspectRatio;
                    }

                    theImage.attr("src",highResImageUrl);
                    if (photoOrientation <= 4) {
                        theImage.width(imageWidth).height(imageHeight);
                        theImage.css("max-width", imageWidth).css("max-height", imageHeight);
                    } else {
                        theImage.width(imageHeight).height(imageWidth);
                        theImage.css("max-width", imageHeight).css("max-height", imageWidth);
                    }
                    theImage.removeClass("_timeline_photo_dialog_image_orientation_1");
                    theImage.addClass(highResOrientationCssClass);
                    $("#photoDialog ._timeline_photo_dialog_photo_table").width(Math.max(imageHeight,imageWidth)).height(imageHeight);
                    centerPhotoDialog();
                });
            } else {
                // fade the form back in and show the medium-res version of the image
                formContainer.fadeIn(100, function() {

                    theImage.attr("src", mediumResImageUrl);

                    var originalWidth = theImage.width();
                    var originalHeight = theImage.height();
                    var imageHeight = 300;
                    var imageWidth = 300;
                    var imageAspectRatio = (photoOrientation <= 4 ) ? originalWidth / originalHeight : originalHeight / originalWidth;
                    if (imageAspectRatio > 1) {
                        imageHeight = Math.round(imageWidth / imageAspectRatio);
                    } else {
                        imageWidth = imageAspectRatio * imageHeight;
                    }

                    if (originalWidth != 0 && originalHeight != 0 && !isNaN(imageWidth) && !isNaN(imageHeight)) {
                        theImage.width(imageWidth).height(imageHeight);
                    }
                    theImage.css("max-width", "300").css("max-height", "300");

                    $("#photoDialog ._timeline_photo_dialog_photo_table").width(300).height(300);
                    centerPhotoDialog(grapher);
                    theImage.removeClass(highResOrientationCssClass);
                    theImage.addClass("_timeline_photo_dialog_image_orientation_1");
                });
            }
        });

        var createCommentAndTagForm = function(comment, tags) {

            var isDirty = function() {
                // first check the comment, since it's easy and cheap
                if ($("#photoDialog #_timeline_photo_dialog_comment").val() != comment) {
                    return true;
                }

                // if the comment isn't dirty, then check the tags
                var newTags = getUserSelectedTags();

                // start by checking the length
                if (newTags.length != tags.length) {
                    return true;
                }

                // now compare individual tags
                for (var i = 0; i < newTags.length; i++) {
                    if (newTags[i] != tags[i]) {
                        return true;
                    }
                }

                return false;
            };

            var setEnabledStateOfRevertAndSaveButtons = function() {
                if (isDirty()) {
                    //$("#_timeline_photo_dialog_save_button").removeAttr("disabled");
                    $("#photoDialog #_timeline_photo_dialog_revert_button").removeAttr("disabled");
                } else {
                    //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
                    $("#photoDialog #_timeline_photo_dialog_revert_button").attr("disabled", "disabled");
                }
            };

            // build the form for the metadata editor
            var photoMetadataForm = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_form_template").render({});
            $("#photoDialog #_timeline_photo_dialog_form").html(photoMetadataForm);

            // fill in the timestamp
            if (typeof photoMetadata['timestampString'] === 'undefined') {
                $("#photoDialog #_timeline_photo_dialog_timestamp").html("&nbsp;");
            } else {
                var photoTimestamp = new Date(photoMetadata['timestampString']);
                var photoTimestampStr = null;
                if (photoMetadata['isLocalTimeType']) {
                    // if local time type, then get the timezone offset (in minutes), convert
                    // it to millis, and add to the time to get the correct time
                    photoTimestamp = new Date(photoTimestamp.getTime() + photoTimestamp.getTimezoneOffset() * 60000);

                    // format the date without the timezone
                    photoTimestampStr = photoTimestamp.toDateString() + " " +
                                        (photoTimestamp.getHours() < 10 ? "0" : "") + photoTimestamp.getHours() +
                                        ":" +
                                        (photoTimestamp.getMinutes() < 10 ? "0" : "") + photoTimestamp.getMinutes() +
                                        ":" +
                                        (photoTimestamp.getSeconds() < 10 ? "0" : "") + photoTimestamp.getSeconds();
                }
                else {
                    photoTimestampStr = photoTimestamp.toString();
                }
                $("#photoDialog #_timeline_photo_dialog_timestamp").text(photoTimestampStr);
            }

            // fill in the comment, if any
            if (typeof comment === 'undefined' || comment == null) {
                $("#photoDialog #_timeline_photo_dialog_comment").val('');
            } else {
                $("#photoDialog #_timeline_photo_dialog_comment").val(comment);
            }

            // Set up focus and blur event handlers for the comment field, to toggle
            // close on ESC for the photo dialog.  We don't want the ESC key to close
            // the dialog when the user is editing the comment.
            $("#photoDialog #_timeline_photo_dialog_comment").focus(function() {
                $("#photoDialog")['dialog']("option", "closeOnEscape", false);
            });
            $("#photoDialog #_timeline_photo_dialog_comment").blur(function() {
                $("#photoDialog")['dialog']("option", "closeOnEscape", true);
            });
            $("#photoDialog #_timeline_photo_dialog_comment").keyup(setEnabledStateOfRevertAndSaveButtons);

            // add the tags, if any
            if ($.isArray(tags) && tags.length > 0) {
                $.each(tags,
                    function(index, value) {
                        var tagHtml =App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_tags_editor_tag_template").render({"value" : value});
                        $("#photoDialog #_timeline_photo_dialog_tags_editor").append(tagHtml);
                    });
            } else {
                var tagHtml = App.fetchCompiledMustacheTemplate("core/grapher/timelineTemplates.html","_timeline_photo_dialog_tags_editor_tag_template").render({"value" : ""});
                $("#photoDialog #_timeline_photo_dialog_tags_editor").append(tagHtml);
            }

            // construct the tag editor
            var tagEditorOptions = {
                autocompleteOptions : {
                    "minLength" : 0, // TODO: make this 1 or 2 if the list of tags is huge
                    "delay"     : 0,
                    "autoFocus" : false,
                    source      : function(request, response) {
                        var tagsToExclude = getUserSelectedTags();
                        var cachedTagsData = TAG_MANAGER.getCachedTagsForTagEditor(tagsToExclude);
                        return response($.ui.autocomplete.filter(cachedTagsData, request.term));
                    }
                },
                // return, comma, space, period, semicolon
                breakKeyCodes       : [ 13, 44, 32, 59 ],
                additionalListClass : '_timeline_photo_tags_input',
                animSpeed           : 100,
                allowAdd            : true,
                allowEdit           : true,
                allowDelete         : false,
                texts               : {
                    removeLinkTitle    : 'Remove this tag from the list',
                    saveEditLinkTitle  : 'Save changes',
                    breakEditLinkTitle : 'Undo changes'
                }
            };
            $('#photoDialog #_timeline_photo_dialog_tags_editor input.tag').tagedit(tagEditorOptions);
            $('#photoDialog #_timeline_photo_dialog_tags_editor').bind('tagsChanged', setEnabledStateOfRevertAndSaveButtons);
            $('#photoDialog #_timeline_photo_dialog_tags_editor').bind('receivedFocus', function() {
                $("#photoDialog")['dialog']("option", "closeOnEscape", false);
            });
            $('#photoDialog #_timeline_photo_dialog_tags_editor').bind('tabToNextElement', function(event) {
                $("#photoDialog")['dialog']("option", "closeOnEscape", true);

                $("#photoDialog #_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_forward").focus();
                return false;
            });
            $('#photoDialog #_timeline_photo_dialog_tags_editor').bind('tabToPreviousElement', function(event) {
                $("#photoDialog")['dialog']("option", "closeOnEscape", true);

                $("#photoDialog #_timeline_photo_dialog_comment").select().focus();
                return false;
            });

            // set form buttons to initially disabled
            //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
            $("#photoDialog #_timeline_photo_dialog_revert_button").attr("disabled", "disabled");

            // configure the Revert button
            $("#photoDialog #_timeline_photo_dialog_revert_button").click(function() {
                $("#photoDialog #_timeline_photo_dialog_form").hide();
                $("#photoDialog #_timeline_photo_dialog_form_status").text("Loading...").show();

                // recreate the comment and tag form
                createCommentAndTagForm(comment, tags);
                $("#photoDialog #_timeline_photo_dialog_form_status").hide();
                $("#photoDialog #_timeline_photo_dialog_form").show();

                // focus on the comment
                $("#photoDialog #_timeline_photo_dialog_comment").select().focus();
            });

            // configure the Save button
            $("#photoDialog #_timeline_photo_dialog_save_button").click(function() {

                // set form buttons to disabled while saving
                //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
                $("#photoDialog #_timeline_photo_dialog_revert_button").attr("disabled", "disabled");

                $("#photoDialog #_timeline_photo_dialog_form").hide();
                $("#photoDialog #_timeline_photo_dialog_form_status").text("Saving...").show();

                var compoundPhotoIdComponents = compoundPhotoId.split(".");

                $.ajax({
                    cache    : false,
                    type     : "POST",
                    url      : "/api/bodytrack/metadata/" + App.getUID() + "/" + compoundPhotoIdComponents[0] + "." + compoundPhotoIdComponents[1] + "/" + compoundPhotoIdComponents[2] + "/set",
                    data     : {
                        "tags"    : getUserSelectedTags().join(','),
                        "comment" : $("#photoDialog #_timeline_photo_dialog_comment").val()
                    },
                    dataType : "json",
                    success  : function(savedData, textStatus, jqXHR) {
                        if (typeof savedData === 'object') {
                            console.log("Successfully saved comment and tags for photo [" + compoundPhotoId + "]");
                            console.log(savedData);
                            photoCache.update(compoundPhotoId, {
                                "comment": savedData['payload']['comment'],
                                "tags": savedData['payload']['tags']
                            });
                            TAG_MANAGER.refreshTagCache(function() {

                                $("#photoDialog #_timeline_photo_dialog_form_status")
                                    .text("Saved.")
                                    .delay(250)
                                    .fadeOut(500, function() {
                                        // read the desired direction from the prefs
                                        goToNeighborOnSaveEnabled = !!PREFS.get("photo_dialog.goto_neighbor_on_save.enabled", false);
                                        goToNeighborOnSaveDirection = TOOLS.parseInt(PREFS.get("photo_dialog.goto_neighbor_on_save.direction", 0), 0);

                                        // now determine what action to take upon save
                                        if (goToNeighborOnSaveEnabled && isPreviousPhoto && goToNeighborOnSaveDirection < 0) {
                                            $("#photoDialog #_timeline_photo_dialog_previous_button").click();
                                        } else if (goToNeighborOnSaveEnabled && isNextPhoto && goToNeighborOnSaveDirection > 0) {
                                            $("#photoDialog #_timeline_photo_dialog_next_button").click();
                                        } else {
                                            // recreate the comment and tag form
                                            createCommentAndTagForm(savedData['payload']['comment'], savedData['payload']['tags']);

                                            $("#_timeline_photo_dialog_form").show();

                                            // focus on the comment
                                            $("#_timeline_photo_dialog_comment").select().focus();
                                        }
                                    });
                            });
                        } else {
                            console.log("Unexpected response when saving comment and tags for photo [" + compoundPhotoId + "]:  savedData=[" + savedData + "] textStatus=[" + textStatus + "]");
                            $("#photoDialog #_timeline_photo_dialog_form_status").text("Saved failed.").show();
                        }
                    },
                    error    : function(jqXHR, textStatus, errorThrown) {
                        console.log("Failed to save comment and tags for photo [" + compoundPhotoId + "]:  textStatus=[" + textStatus + "] errorThrown=[" + errorThrown + "]");
                        $("#photoDialog #_timeline_photo_dialog_form_status").text("Saved failed.").show();
                    }
                });
            });

            updateGoToNeighborOnSaveWidgets();

            // set up tabbing and focus handling
            $("#photoDialog #_timeline_photo_dialog_form #tagedit-input").attr("tabindex", 102);
            $("#photoDialog #_timeline_photo_dialog_tabhelper_pre_proxy_backward").focus(function() {
                if ($("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor_choice").is(":enabled")) {
                    $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor_choice").focus();
                } else {
                    $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor").focus();
                }
                return false;
            });
            $("#photoDialog #_timeline_photo_dialog_previous_button").focus(function() {
                $(this).css("background-position", "0 -38px");
            }).blur(function() {
                    $(this).css("background-position", "0 0");
                });
            $("#photoDialog #_timeline_photo_dialog_next_button").focus(function() {
                $(this).css("background-position", "0 -38px");
            }).blur(function() {
                    $(this).css("background-position", "0 0");
                });
            $("#photoDialog #_timeline_photo_dialog_comment_tabhelper_pre_proxy_forward").focus(function() {
                $("#_timeline_photo_dialog_comment").focus().select();
                return false;
            });
            $("#photoDialog #_timeline_photo_dialog_comment_tabhelper_pre_proxy_backward").focus(function() {
                if (isNextPhoto) {
                    $("#photoDialog #_timeline_photo_dialog_next_button").focus();
                } else if (isPreviousPhoto) {
                    $("#photoDialog #_timeline_photo_dialog_previous_button").focus();
                } else {
                    $("#photoDialog #_timeline_photo_dialog_tabhelper_pre_proxy_backward").focus();
                }
                return false;
            });
            $("#photoDialog #_timeline_photo_dialog_comment").focus(function() {
                return false;
            });
            $("#photoDialog #_timeline_photo_dialog_tags_editor_tabhelper_pre_proxy_forward").focus(function() {
                $("#photoDialog #_timeline_photo_dialog_tags_editor ul").click();
            });
            $("#photoDialog #_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_forward").focus(function() {
                if ($("#photoDialog #_timeline_photo_dialog_save_button").is(":disabled")) {
                    $("#photoDialog #_timeline_photo_dialog_save_should_goto_neighbor").focus();
                } else {
                    $("#photoDialog #_timeline_photo_dialog_save_button").focus();
                }
                return false;
            });
            $("#photoDialog #_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_backward").focus(function() {
                $("#_timeline_photo_dialog_tags_editor ul").click();
            });
            $("#photoDialog #_timeline_photo_dialog_revert_button").focus(function() {
                $(this).css("color", "#18B054");
            }).blur(function() {
                    $(this).css("color", "#000000");
                });
            $("#photoDialog #_timeline_photo_dialog_save_button").focus(function(event) {
                $(this).css("color", "#18B054");
            }).blur(function(event) {
                    $(this).css("color", "#000000");
                });
            $("#photoDialog #_timeline_photo_dialog_post_proxy_forward").focus(function() {
                if (isPreviousPhoto) {
                    $("#photoDialog #_timeline_photo_dialog_previous_button").focus();
                } else if (isNextPhoto) {
                    $("#photoDialog #_timeline_photo_dialog_next_button").focus();
                } else {
                    $("#photoDialog #_timeline_photo_dialog_comment").focus().select();
                }
                return false;
            });

            // set focus on the comment input, and select all the text
            $("#photoDialog #_timeline_photo_dialog_comment").select().focus();

        };

        // create the comment and tag form, hide the status area, and show the form
        createCommentAndTagForm(photoMetadata['comment'], photoMetadata['tags']);
        $("#photoDialog #_timeline_photo_dialog_form_status").hide();
        $("#photoDialog #_timeline_photo_dialog_form").show();

        // Finally, call the completion callback, if any
        if (typeof completionCallback === 'function') {
            completionCallback();
        }
    };

    var PhotoUtils = {};
    PhotoUtils.getCarouselHTML = getCarouselHTML;
    PhotoUtils.showCarouselHTML = showCarouselHTML;
    PhotoUtils.showPhotoDialog = showPhotoDialog;

    return PhotoUtils;
});