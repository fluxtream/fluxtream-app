define(["core/Tab", "core/FlxState", "core/grapher/Grapher",
        "applications/calendar/App"],
    function(Tab, FlxState, Grapher, Calendar) {



    function createPhotoDialogCache(channelFilterTags, isAndJoin) {
        var cache = {
            photos                             : [],
            photosById                         : {}, // maps photo ID to an index in the photos array
            isLoadingPreceding                 : false,
            isLoadingFollowing                 : false,
            NUM_PHOTOS_TO_FETCH                : 20,
            DISTANCE_FROM_END_TO_TRIGGER_FETCH : 10,
            __loadNeighboringPhotoMetadata     : function(currentPhotoId,
                                                          currentPhotoTimestamp,
                                                          tagsFilterArray,
                                                          isAndJoin,
                                                          shouldLoadPreviousNeighbor, // flag which determines whether the previous or following neighbor will be loaded
                                                          callbacks) {
                currentPhotoId = TOOLS.parseInt(currentPhotoId, -1);
                if (currentPhotoId >= 0) {
                    if (typeof callbacks === 'undefined') {
                        callbacks = {};
                    }
                    var successCallback = callbacks['success'];
                    var errorCallback = callbacks['error'];
                    var completeCallback = callbacks['complete'];

                    shouldLoadPreviousNeighbor = !!shouldLoadPreviousNeighbor;
                    isAndJoin = !!isAndJoin;

                    var url = "/bodytrack/users/" + App.getUID() + "/log_items/get";
                    var urlParams = {
                        "id"         : currentPhotoId,
                        "time"       : currentPhotoTimestamp,
                        "type"       : "photos",
                        "descending" : shouldLoadPreviousNeighbor,
                        "exclusive"  : false,
                        "count"      : cache.NUM_PHOTOS_TO_FETCH
                    };

                    if (isAndJoin) {
                        urlParams["all_tags"] = tagsFilterArray.join(",");
                    } else {
                        urlParams["any_tags"] = tagsFilterArray.join(",");
                    }

                    TOOLS.loadJson(url, urlParams, {
                        "success"  : function(photos) {
                            if ($.isArray(photos)) {
                                if (typeof successCallback === 'function') {
                                    var photosMetadata = [];
                                    $.each(photos, function(index, photo) {
                                        photosMetadata[index] = {
                                            "photoId"         : photo['id'],
                                            "comment"         : photo['comment'],
                                            "tags"            : photo['tags'],
                                            "timestamp"       : photo['end_d'],
                                            "timestampString" : photo['end']
                                        };
                                    });

                                    // mark the last photo as the end if we got fewer photos than we wanted
                                    if (photos.length < cache.NUM_PHOTOS_TO_FETCH) {
                                        console.log("PhotoDialogCache.__loadNeighboringPhotoMetadata(): Requested ["+cache.NUM_PHOTOS_TO_FETCH+"] photos, but only got ["+photos.length+"].  Marking the last photo as the end to prevent spurious fetches.");
                                        photosMetadata[photosMetadata.length-1]['isEndingPhoto'] = true;
                                    }

                                    successCallback(photosMetadata);
                                }
                            } else if (typeof errorCallback == 'function') {
                                errorCallback("loadNeighboringPhotoMetadata(): Returned data is not an array");
                            }
                        },
                        "error"    : errorCallback,
                        "complete" : completeCallback
                    });
                }
            }, __loadPreceding                 : function(photoId, timestamp, successCallback) {
                if (cache.isLoadingPreceding) {
                    console.log("PhotoDialogCache.__loadPreceding(): doing nothing since we're already loading");
                } else {
                    cache.isLoadingPreceding = true;
                    cache.__loadNeighboringPhotoMetadata(photoId,
                        timestamp,
                        channelFilterTags,
                        isAndJoin,
                        true,
                        {
                            "success" : successCallback,
                            "complete": function() {
                                cache.isLoadingPreceding = false;
                            }
                        });
                }
            },
            __loadFollowing                    : function(photoId, timestamp, successCallback) {
                if (cache.isLoadingFollowing) {
                    console.log("PhotoDialogCache.__loadFollowing(): doing nothing since we're already loading");
                } else {
                    cache.isLoadingFollowing = true;
                    cache.__loadNeighboringPhotoMetadata(photoId,
                        timestamp,
                        channelFilterTags,
                        isAndJoin,
                        false,
                        {
                            "success" : successCallback,
                            "complete": function() {
                                cache.isLoadingFollowing = false;
                            }
                        });
                }
            },
            initialize                         : function(photoId, timestamp, callback) {
                //console.log("PhotoDialogCache.initialize()------------------------------------------");

                // To build up the initial cache, fetch the photos BEFORE this photo, then the photos AFTER it.
                cache.__loadPreceding(photoId,
                    timestamp,
                    function(precedingPhotosMetadata) {
                        cache.__loadFollowing(photoId,
                            timestamp,
                            function(followingPhotosMetadata) {
                                // Create the initial cache.  We do this by first reversing the array
                                // containing the preceding photos, then slicing off the first element
                                // of the array containing the following photos (since it's a dupe
                                // of the last element in the reverse precedingPhotosMetadata
                                // array) and then concatenating the two together.
                                cache.photos = precedingPhotosMetadata.reverse().concat(followingPhotosMetadata.slice(1));

                                // now create the map which maps photo ID to photo array element index
                                $.each(cache.photos, function(index, photo) {
                                    cache.photosById[photo['photoId']] = index;
                                });

                                // now that the cache is created, we can call the callback
                                if (typeof callback === 'function') {
                                    callback();
                                }
                            })
                    });
            },

            __getPhotoMetadata : function(photoId, offset) {
                if (photoId in cache.photosById) {
                    var indexOfRequestedPhoto = cache.photosById[photoId] + offset;
                    if (indexOfRequestedPhoto >= 0 && indexOfRequestedPhoto < cache.photos.length) {
                        return cache.photos[indexOfRequestedPhoto];
                    }
                }
                //console.log("PhotoDialogCache.__getPhotoMetadata(): Failed to get photo offset [" + offset + "] for ID [" + photoId + "]");
                return null;
            },

            getPreviousPhotoMetadata : function(photoId) {
                var photo = cache.__getPhotoMetadata(photoId, -1);

                if (photo != null) {
                    // Check how close we are to the beginning of the array.  If it's within __DISTANCE_FROM_END_TO_TRIGGER_FETCH,
                    // then spawn an asyncrhonous job to fetch more photos
                    var distance = cache.photosById[photoId];
                    if (distance < cache.DISTANCE_FROM_END_TO_TRIGGER_FETCH) {
                        var endingPhoto = cache.photos[0];
                        if ('isEndingPhoto' in endingPhoto) {
                            console.log("PhotoDialogCache.getPreviousPhotoMetadata(): No need to fetch more photos since we've already loaded up to the end [" + endingPhoto['photoId'] + "]");
                        } else {
                            console.log("PhotoDialogCache.getPreviousPhotoMetadata(): Fetching more photos preceding id ["+endingPhoto['photoId']+"]");
                            cache.__loadPreceding(endingPhoto['photoId'],
                                endingPhoto['timestamp'],
                                function(photosMetadata) {
                                    console.log("PhotoDialogCache.getPreviousPhotoMetadata(): Fetched ["+photosMetadata.length+"] more previous photos.");

                                    // make sure that the cache didn't change while we were doing the fetch
                                    if (endingPhoto['photoId'] == cache.photos[0]['photoId']) {
                                        // create a new photos array for the cache
                                        var newPhotos = photosMetadata.slice(1).reverse().concat(cache.photos);
                                        var newPhotosById = {};

                                        // now recreate the map which maps photo ID to photo array element index
                                        $.each(newPhotos, function(index, photo) {
                                            newPhotosById[photo['photoId']] = index;
                                        });

                                        //var s = "";
                                        //$.each(newPhotos, function(index, photo) {
                                        //  s += photo['photoId'] + ","
                                        //});
                                        //console.log("length=[" + newPhotos.length + "," + cache.photos.length + "]: " + s);

                                        // update the cache's array and map
                                        cache.photos = newPhotos;
                                        cache.photosById = newPhotosById;
                                    } else {
                                        console.log("PhotoDialogCache.getPreviousPhotoMetadata(): cache has changed, won't update");
                                    }
                                });
                        }
                    }
                }

                return photo;
            },

            getNextPhotoMetadata : function(photoId) {
                var photo = cache.__getPhotoMetadata(photoId, 1);

                if (photo != null) {
                    // Check how close we are to the beginning of the array.  If it's within __DISTANCE_FROM_END_TO_TRIGGER_FETCH,
                    // then spawn an asyncrhonous job to fetch more photos
                    var distance = cache.photos.length - 1 - cache.photosById[photoId];
                    if (distance < cache.DISTANCE_FROM_END_TO_TRIGGER_FETCH) {
                        var endingPhoto = cache.photos[cache.photos.length - 1];
                        if ('isEndingPhoto' in endingPhoto) {
                            console.log("PhotoDialogCache.getNextPhotoMetadata(): No need to fetch more photos since we've already loaded up to the end [" + endingPhoto['photoId'] + "]");
                        } else {
                            console.log("PhotoDialogCache.getNextPhotoMetadata(): Fetching more photos following id ["+endingPhoto['photoId']+"]");
                            cache.__loadFollowing(endingPhoto['photoId'],
                                endingPhoto['timestamp'],
                                function(photosMetadata) {
                                    console.log("PhotoDialogCache.getNextPhotoMetadata(): Fetched ["+photosMetadata.length+"] more following photos.");

                                    // make sure that the cache didn't change while we were doing the fetch
                                    if (endingPhoto['photoId'] == cache.photos[cache.photos.length - 1]['photoId']) {
                                        // create a new photos array for the cache
                                        var newPhotos = cache.photos.concat(photosMetadata.slice(1));
                                        var newPhotosById = {};

                                        // now recreate the map which maps photo ID to photo array element index
                                        $.each(newPhotos, function(index, photo) {
                                            newPhotosById[photo['photoId']] = index;
                                        });

                                        //var s = "";
                                        //$.each(newPhotos, function(index, photo) {
                                        //  s += photo['photoId'] + ","
                                        //});
                                        //console.log("length=[" + newPhotos.length + "," + cache.photos.length + "]: " + s);

                                        // update the cache's array and map
                                        cache.photos = newPhotos;
                                        cache.photosById = newPhotosById;
                                    } else {
                                        console.log("PhotoDialogCache.getNextPhotoMetadata(): cache has changed, won't update");
                                    }
                                });
                        }
                    }
                }

                return photo;
            },

            getPhotoMetadata : function(photoId) {
                return cache.__getPhotoMetadata(photoId, 0);
            },

            update : function(photoId, newData) {
                if (photoId in cache.photosById) {
                    var index = cache.photosById[photoId];
                    cache.photos[index] = {
                        "photoId"         : newData['id'],
                        "comment"         : newData['comment'],
                        "tags"            : newData['tags'],
                        "timestamp"       : newData['end_d'],
                        "timestampString" : newData['end']
                    };
                }
            }
        };
        return cache;
    }

    function dataPointListener(pointObj, sourceInfo) {
        if (pointObj) {
            $("#_timeline_dataPointValueLabel").html(Hogan.compile($("#_timeline_data_point_value_label_template").html()).render(pointObj));
        } else {
            $("#_timeline_dataPointValueLabel").html("");
        }
    }

    function loadLogrecMetadata(logrecId, callbacks) {
        logrecId = TOOLS.parseInt(logrecId, -1);
        if (logrecId >= 0) {

            var url = "/bodytrack/users/" + App.getUID() + "/logrecs/" + logrecId + "/get";

            TOOLS.loadJson(url, {}, callbacks);
        }
    }

    function photoDataPointListener(channelElementId) {
        return function(pointObj, sourceInfo) {
            if (pointObj && sourceInfo && sourceInfo['info']) {

                // returns the array of tags currently selected for this photo
                var getUserSelectedTags = function() {
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

                // returns the array of tags selected for this channel's filter
                var getTagFilterForChannel = function() {
                    var tags = [];
                    $.each($("#" + channelElementId + "-photo-tags-filter .tagedit-listelement-old input"),
                        function(index, inputElement) {
                            var val = inputElement['value'];
                            if (typeof val === 'string' && val != '') {
                                tags[tags.length] = val;
                            }
                        });
                    return tags;
                };
                // get the channel's current settings for tag filtering
                var isAndJoin = $("#" + channelElementId + "-photo-tags-isAndJoin").val() === 'true';
                var channelFilterTags = getTagFilterForChannel();

                // create the photo cache
                var photoCache = createPhotoDialogCache(channelFilterTags, isAndJoin);

                var createPhotoDialog = function(photoId, timestamp, completionCallback) {

                    var mediumResImageUrl = Hogan.compile($("#_timeline_photo_dialog_medium_res_image_url_template").html()).render({"photoId" : photoId, "userId" : App.getUID()});
                    var highResImageUrl = Hogan.compile($("#_timeline_photo_dialog_high_res_image_url_template").html()).render({"photoId" : photoId, "userId" : App.getUID()});
                    $("#_timeline_photo_dialog").html(Hogan.compile($("#_timeline_photo_dialog_template").html()).render({"photoUrl" : mediumResImageUrl}));

                    var updateGoToNeighborOnSaveWidgets = function() {
                        var isEnabled = $("#_timeline_photo_dialog_save_should_goto_neighbor").is(':checked');
                        var direction = TOOLS.parseInt($("#_timeline_photo_dialog_save_should_goto_neighbor_choice").val(),0);
                        PREFS.set("photo_dialog.goto_neighbor_on_save.enabled", isEnabled);
                        PREFS.set("photo_dialog.goto_neighbor_on_save.direction", direction);

                        if (isEnabled) {
                            $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").removeAttr("disabled");
                            $("#_timeline_photo_dialog_save_preferences label").css("color", "#000000");
                            if (direction < 0) {
                                $("#_timeline_photo_dialog_save_button").html("Save &amp; Previous");
                            } else {
                                $("#_timeline_photo_dialog_save_button").html("Save &amp; Next");
                            }
                        } else {
                            $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").attr("disabled", "disabled");
                            $("#_timeline_photo_dialog_save_preferences label").css("color", "#aaaaaa");
                            $("#_timeline_photo_dialog_save_button").text("Save");
                        }
                    };

                    // set the widgets for the Save button behavior based on saved prefs
                    var goToNeighborOnSaveEnabled = !!PREFS.get("photo_dialog.goto_neighbor_on_save.enabled", false);
                    var goToNeighborOnSaveDirection = TOOLS.parseInt(PREFS.get("photo_dialog.goto_neighbor_on_save.direction", 0), 0);
                    $("#_timeline_photo_dialog_save_should_goto_neighbor").prop("checked", goToNeighborOnSaveEnabled);
                    $("#_timeline_photo_dialog_save_should_goto_neighbor").change(updateGoToNeighborOnSaveWidgets);
                    $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").val(goToNeighborOnSaveDirection == 0 ? 1 : goToNeighborOnSaveDirection);
                    $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").change(updateGoToNeighborOnSaveWidgets);

                    // display Loading status message
                    $("#_timeline_photo_dialog_form_status").text("Loading...").show();

                    // set previous and next buttons initially hidden
                    $("#_timeline_photo_dialog_previous_button").hide();
                    $("#_timeline_photo_dialog_next_button").hide();

                    // Fetch the metadata for the preceding, following, and current photos from the cache.
                    var previousPhotoMetadata = photoCache.getPreviousPhotoMetadata(photoId);
                    var nextPhotoMetadata = photoCache.getNextPhotoMetadata(photoId);
                    var data = photoCache.getPhotoMetadata(photoId);

                    var isPreviousPhoto = previousPhotoMetadata != null &&
                        typeof previousPhotoMetadata !== 'undefined' &&
                        typeof previousPhotoMetadata['photoId'] !== 'undefined';
                    if (isPreviousPhoto) {
                        $("#_timeline_photo_dialog_previous_button").show().click(function() {
                            createPhotoDialog(previousPhotoMetadata['photoId'],
                                previousPhotoMetadata['timestamp']);
                        });
                    }

                    var isNextPhoto = nextPhotoMetadata != null &&
                        typeof nextPhotoMetadata !== 'undefined' &&
                        typeof nextPhotoMetadata['photoId'] !== 'undefined';
                    if (isNextPhoto) {
                        $("#_timeline_photo_dialog_next_button").show().click(function() {
                            createPhotoDialog(nextPhotoMetadata['photoId'],
                                nextPhotoMetadata['timestamp']);
                        });
                    }

                    if (typeof data === 'string') {
                        data = JSON.parse(data);
                    }

                    // treat undefined or null comment as an empty comment
                    if (typeof data['comment'] === 'undefined' || data['comment'] == null) {
                        data['comment'] = '';
                    }

                    // treat undefined or null tags as an empty array
                    if (typeof data['tags'] === 'undefined' || data['tags'] == null) {
                        data['tags'] = [];
                    }

                    // add click handler for photo to allow viewing of high-res version
                    $("#_timeline_photo_dialog_image").click(function() {
                        var theImage = $(this);
                        var imageAspectRatio = $(this).width() / $(this).height();
                        var formContainer = $("#_timeline_photo_dialog_form_container");
                        if ($("#_timeline_photo_dialog_form_container").is(":visible")) {
                            // fade out the form and show the hi-res version of the image
                            formContainer.fadeOut(100, function() {
                                var imageHeight = $("body").height() - 60;
                                var imageWidth = imageAspectRatio * imageHeight;

                                // make sure the image isn't too wide now
                                if (imageWidth > $("body").width()) {
                                    imageWidth = $("body").width() - 100;
                                    imageHeight = imageWidth / imageAspectRatio;
                                }

                                theImage.attr("src",highResImageUrl).height(imageHeight).width(imageWidth);
                                $("#_timeline_photo_dialog_photo_table").height(imageHeight).width(imageWidth);
                                centerPhotoDialog();
                            });
                        } else {
                            // fade the form back in and show the medium-res version of the image
                            formContainer.fadeIn(100, function() {
                                var imageHeight = 300;
                                var imageWidth = imageAspectRatio * imageHeight;
                                theImage.height(imageHeight).width(imageWidth);
                                $("#_timeline_photo_dialog_photo_table").height(imageHeight).width(imageWidth);
                                centerPhotoDialog();
                                theImage.attr("src", mediumResImageUrl);
                            });
                        }
                    });

                    var createCommentAndTagForm = function(comment, tags) {

                        var isDirty = function() {
                            // first check the comment, since it's easy and cheap
                            if ($("#_timeline_photo_dialog_comment").val() != comment) {
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
                                $("#_timeline_photo_dialog_revert_button").removeAttr("disabled");
                            } else {
                                //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
                                $("#_timeline_photo_dialog_revert_button").attr("disabled", "disabled");
                            }
                        };

                        // build the form for the metadata editor
                        var photoMetadataForm = Hogan.compile($("#_timeline_photo_dialog_form_template").html()).render({});
                        $("#_timeline_photo_dialog_form").html(photoMetadataForm);

                        // fill in the timestamp
                        if (typeof data['timestampString'] === 'undefined') {
                            $("#_timeline_photo_dialog_timestamp").html("&nbsp;");
                        } else {
                            $("#_timeline_photo_dialog_timestamp").text(new Date(data['timestampString']).toString());
                        }

                        // fill in the comment, if any
                        if (typeof comment === 'undefined' || comment == null) {
                            $("#_timeline_photo_dialog_comment").val('');
                        } else {
                            $("#_timeline_photo_dialog_comment").val(comment);
                        }

                        // Set up focus and blur event handlers for the comment field, to toggle
                        // close on ESC for the photo dialog.  We don't want the ESC key to close
                        // the dialog when the user is editing the comment.
                        $("#_timeline_photo_dialog_comment").focus(function() {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", false);
                        });
                        $("#_timeline_photo_dialog_comment").blur(function() {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);
                        });
                        $("#_timeline_photo_dialog_comment").keyup(setEnabledStateOfRevertAndSaveButtons);

                        // add the tags, if any
                        if ($.isArray(tags) && tags.length > 0) {
                            $.each(tags,
                                function(index, value) {
                                    var tagHtml =Hogan.compile($("#_timeline_photo_dialog_tags_editor_tag_template").html()).render({"value" : value});
                                    $("#_timeline_photo_dialog_tags_editor").append(tagHtml);
                                });
                        } else {
                            var tagHtml = Hogan.compile($("#_timeline_photo_dialog_tags_editor_tag_template").html()).render({"value" : ""});
                            $("#_timeline_photo_dialog_tags_editor").append(tagHtml);
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
                        $('#_timeline_photo_dialog_tags_editor input.tag').tagedit(tagEditorOptions);
                        $('#_timeline_photo_dialog_tags_editor').bind('tagsChanged', setEnabledStateOfRevertAndSaveButtons);
                        $('#_timeline_photo_dialog_tags_editor').bind('receivedFocus', function() {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", false);
                        });
                        $('#_timeline_photo_dialog_tags_editor').bind('tabToNextElement', function(event) {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);

                            $("#_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_forward").focus();
                            return false;
                        });
                        $('#_timeline_photo_dialog_tags_editor').bind('tabToPreviousElement', function(event) {
                            $("#_timeline_photo_dialog")['dialog']("option", "closeOnEscape", true);

                            $("#_timeline_photo_dialog_comment").select().focus();
                            return false;
                        });

                        // set form buttons to initially disabled
                        //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
                        $("#_timeline_photo_dialog_revert_button").attr("disabled", "disabled");

                        // configure the Revert button
                        $("#_timeline_photo_dialog_revert_button").click(function() {
                            $("#_timeline_photo_dialog_form").hide();
                            $("#_timeline_photo_dialog_form_status").text("Loading...").show();

                            // recreate the comment and tag form
                            createCommentAndTagForm(comment, tags);
                            $("#_timeline_photo_dialog_form_status").hide();
                            $("#_timeline_photo_dialog_form").show();

                            // focus on the comment
                            $("#_timeline_photo_dialog_comment").select().focus();
                        });

                        // configure the Save button
                        $("#_timeline_photo_dialog_save_button").click(function() {

                            // set form buttons to disabled while saving
                            //$("#_timeline_photo_dialog_save_button").attr("disabled", "disabled");
                            $("#_timeline_photo_dialog_revert_button").attr("disabled", "disabled");

                            $("#_timeline_photo_dialog_form").hide();
                            $("#_timeline_photo_dialog_form_status").text("Saving...").show();

                            $.ajax({
                                cache    : false,
                                type     : "POST",
                                url      : "/bodytrack/users/" + App.getUID() + "/logrecs/" + photoId + "/set",
                                data     : {
                                    "tags"    : getUserSelectedTags().join(','),
                                    "comment" : $("#_timeline_photo_dialog_comment").val()
                                },
                                dataType : "json",
                                success  : function(savedData, textStatus, jqXHR) {
                                    if (typeof savedData === 'object') {
                                        console.log("Successfully saved comment and tags for photo [" + photoId + "]");
                                        console.log(savedData);
                                        photoCache.update(photoId, savedData);
                                        TAG_MANAGER.refreshTagCache(function() {

                                            $("#_timeline_photo_dialog_form_status")
                                                .text("Saved.")
                                                .delay(250)
                                                .fadeOut(500, function() {
                                                    // read the desired direction from the prefs
                                                    goToNeighborOnSaveEnabled = !!PREFS.get("photo_dialog.goto_neighbor_on_save.enabled", false);
                                                    goToNeighborOnSaveDirection = TOOLS.parseInt(PREFS.get("photo_dialog.goto_neighbor_on_save.direction", 0), 0);

                                                    // now determine what action to take upon save
                                                    if (goToNeighborOnSaveEnabled && isPreviousPhoto && goToNeighborOnSaveDirection < 0) {
                                                        $("#_timeline_photo_dialog_previous_button").click();
                                                    } else if (goToNeighborOnSaveEnabled && isNextPhoto && goToNeighborOnSaveDirection > 0) {
                                                        $("#_timeline_photo_dialog_next_button").click();
                                                    } else {
                                                        // recreate the comment and tag form
                                                        createCommentAndTagForm(savedData['comment'], savedData['tags']);

                                                        $("#_timeline_photo_dialog_form").show();

                                                        // focus on the comment
                                                        $("#_timeline_photo_dialog_comment").select().focus();
                                                    }
                                                });
                                        });
                                    } else {
                                        console.log("Unexpected response when saving comment and tags for photo [" + photoId + "]:  savedData=[" + savedData + "] textStatus=[" + textStatus + "]");
                                        $("#_timeline_photo_dialog_form_status").text("Saved failed.").show();
                                    }
                                },
                                error    : function(jqXHR, textStatus, errorThrown) {
                                    console.log("Failed to save comment and tags for photo [" + photoId + "]:  textStatus=[" + textStatus + "] errorThrown=[" + errorThrown + "]");
                                    $("#_timeline_photo_dialog_form_status").text("Saved failed.").show();
                                }
                            });
                        });

                        updateGoToNeighborOnSaveWidgets();

                        // set up tabbing and focus handling
                        $("#_timeline_photo_dialog_form #tagedit-input").attr("tabindex", 102);
                        $("#_timeline_photo_dialog_tabhelper_pre_proxy_backward").focus(function() {
                            if ($("#_timeline_photo_dialog_save_should_goto_neighbor_choice").is(":enabled")) {
                                $("#_timeline_photo_dialog_save_should_goto_neighbor_choice").focus();
                            } else {
                                $("#_timeline_photo_dialog_save_should_goto_neighbor").focus();
                            }
                            return false;
                        });
                        $("#_timeline_photo_dialog_previous_button").focus(function() {
                            $(this).css("background-position", "0 -38px");
                        }).blur(function() {
                                $(this).css("background-position", "0 0");
                            });
                        $("#_timeline_photo_dialog_next_button").focus(function() {
                            $(this).css("background-position", "0 -38px");
                        }).blur(function() {
                                $(this).css("background-position", "0 0");
                            });
                        $("#_timeline_photo_dialog_comment_tabhelper_pre_proxy_forward").focus(function() {
                            $("#_timeline_photo_dialog_comment").focus().select();
                            return false;
                        });
                        $("#_timeline_photo_dialog_comment_tabhelper_pre_proxy_backward").focus(function() {
                            if (isNextPhoto) {
                                $("#_timeline_photo_dialog_next_button").focus();
                            } else if (isPreviousPhoto) {
                                $("#_timeline_photo_dialog_previous_button").focus();
                            } else {
                                $("#_timeline_photo_dialog_tabhelper_pre_proxy_backward").focus();
                            }
                            return false;
                        });
                        $("#_timeline_photo_dialog_comment").focus(function() {
                            return false;
                        });
                        $("#_timeline_photo_dialog_tags_editor_tabhelper_pre_proxy_forward").focus(function() {
                            $("#_timeline_photo_dialog_tags_editor ul").click();
                        });
                        $("#_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_forward").focus(function() {
                            if ($("#_timeline_photo_dialog_save_button").is(":disabled")) {
                                $("#_timeline_photo_dialog_save_should_goto_neighbor").focus();
                            } else {
                                $("#_timeline_photo_dialog_save_button").focus();
                            }
                            return false;
                        });
                        $("#_timeline_photo_dialog_tags_editor_tabhelper_post_proxy_backward").focus(function() {
                            $("#_timeline_photo_dialog_tags_editor ul").click();
                        });
                        $("#_timeline_photo_dialog_revert_button").focus(function() {
                            $(this).css("color", "#18B054");
                        }).blur(function() {
                                $(this).css("color", "#000000");
                            });
                        $("#_timeline_photo_dialog_save_button").focus(function(event) {
                            $(this).css("color", "#18B054");
                        }).blur(function(event) {
                                $(this).css("color", "#000000");
                            });
                        $("#_timeline_photo_dialog_post_proxy_forward").focus(function() {
                            if (isPreviousPhoto) {
                                $("#_timeline_photo_dialog_previous_button").focus();
                            } else if (isNextPhoto) {
                                $("#_timeline_photo_dialog_next_button").focus();
                            } else {
                                $("#_timeline_photo_dialog_comment").focus().select();
                            }
                            return false;
                        });

                        // set focus on the comment input, and select all the text
                        $("#_timeline_photo_dialog_comment").select().focus();

                    };

                    // create the comment and tag form, hide the status area, and show the form
                    createCommentAndTagForm(data['comment'], data['tags']);
                    $("#_timeline_photo_dialog_form_status").hide();
                    $("#_timeline_photo_dialog_form").show();

                    // Finally, call the completion callback, if any
                    if (typeof completionCallback === 'function') {
                        completionCallback();
                    }
                };

                // initialize the photo cache--when it's done preloading then open the photo dialog
                photoCache.initialize(sourceInfo['info']['imageId'],
                    pointObj['date'],
                    function() {
                        createPhotoDialog(sourceInfo['info']['imageId'],
                            pointObj['date'],
                            function() {
                                centerPhotoDialog();
                            });
                    });

                // Open the dialog
                $("#_timeline_photo_dialog").html(Hogan.compile($("#_timeline_photo_dialog_loading_template").html()).render({}));
                $("#_timeline_photo_dialog")['dialog']('open');
            }
        };
    }

    function centerPhotoDialog() {
        // center the dialog
        $("#_timeline_photo_dialog")['dialog']("option", "position", 'center');
    }

    function saveDefaultChannelStyle(channel, defaultStyleObj, callbacks) {
        if (typeof channel === 'object' &&
            typeof defaultStyleObj === 'object' &&
            typeof channel["device_name"] === 'string' &&
            typeof channel["channel_name"] === 'string') {

            if (typeof callbacks === 'undefined') {
                callbacks = {};
            }
            var successCallback = callbacks['success'];
            var errorCallback = callbacks['error'];
            var completeCallback = callbacks['complete'];

            var url = "/bodytrack/users/" + App.getUID() + "/channels/" + encodeURIComponent(channel["device_name"]) + "." + encodeURIComponent(channel["channel_name"]) + "/set";
            $.ajax({
                cache    : false,
                type     : "POST",
                url      : url,
                data     : {"user_default_style" : JSON.stringify(defaultStyleObj)},
                success  : function(data, textStatus, jqXHR) {
                    try {
                        if (typeof successCallback === 'function') {
                            successCallback(data);
                        }
                    }
                    catch (ex) {
                        console.log("saveDefaultChannelStyle.success: JSON parse error, or failure in the successCallback: " + ex);
                    }
                },
                error    : function(jqXHR, textStatus, errorThrown) {
                    try {
                        if (typeof errorCallback === 'function') {
                            errorCallback(textStatus, errorThrown);
                        }
                    }
                    catch (ex) {
                        console.log("saveDefaultChannelStyle.error: Failure in the errorCallback: " + ex);
                    }
                },
                complete : function(jqXHR, textStatus) {
                    try {
                        if (typeof completeCallback === 'function') {
                            completeCallback(textStatus);
                        }
                    }
                    catch (ex) {
                        console.log("saveDefaultChannelStyle.complete: Failure in the completeCallback: " + ex);
                    }
                }
            });
        }
    }

    function dragAreaOnMouseDown(plotId) {
        var channelElementId = "_timeline_channel_" + plotId;
        var plotElementId = "_timeline_plot_" + plotId;
        var yAxisElementId = "_timeline_yAxis_" + plotId;
    	
    	var mostRecentY = null;
        var resizeTimer = null;
        var dylist = [];

        var resizePlot = function(dy) {
            var container = plotContainersMap[channelElementId];
            var cPlaceholder = $("#" + container.getPlaceholder());
            var containerW = cPlaceholder.width();
            var containerH = cPlaceholder.height();

            var plot = plotsMap[channelElementId];
            var yAxis = plot.getVerticalAxis();
            var yAixsW = $("#" + yAxis.getPlaceholder()).width();

            var dragAreaH = $("._timeline_dragArea").height() - CHANNEL_PADDING;

            if ((dy > 0) || (Math.abs(dy) < containerH)) {
                // There is a min height of 67, which is taken from the
                // min height of the channel label
                if (containerH + dy + dragAreaH < 67) {
                    dy = 67 - containerH - dragAreaH;
                }

                // Set the size of the plot container itself
                $("#" + plotElementId).height(containerH + dy);
                container.setSize(containerW, containerH + dy,
                    SequenceNumber.getNext());

                // Set the size of the Y-axis
                $("#" + yAxisElementId).height(containerH + dy);
                yAxis.setSize(yAixsW, containerH + dy,
                    SequenceNumber.getNext());

                // Set the size of the channel label
                $("#_timeline_channelTab_" + plotId).height(
                    containerH + dy + CHANNEL_PADDING);

                // Update the view data to match the new channel height
                if ((!!VIEWS.data) && (!!VIEWS.data["v2"])
                    && (!!VIEWS.data["v2"]["y_axes"])
                    && (VIEWS.data["v2"]["y_axes"].length > plotId)) {
                    VIEWS.data["v2"]["y_axes"][plotId]["channel_height"] =
                        containerH + dy;
                }
            }

            mostRecentY = mostRecentY + dy;
            return false;
        };
        var mouseup = null;
        var mousemove = null;
        var updatePlotSize = function() {
            if (dylist.length > 0) {
                resizePlot(dylist[dylist.length - 1]);
                dylist = [];
            }
        };
        var stopListening = function() {
            $(window).unbind({
                mousemove: mousemove,
                mouseup: mouseup
            });
            clearInterval(resizeTimer);
            return false;
        };
        mousemove = function(event) {
            if (mostRecentY == null) {
                mostRecentY = event.pageY;
            } else {
                dylist.push(event.pageY - mostRecentY);
            }
            return false; // Stops the event from propagating
        };
        mouseup = function(event) {
            if (mostRecentY == null) {
                return stopListening();
            }
            stopListening();
            resizePlot(event.pageY - mostRecentY);
            return false; // Stops the event from propagating
        };

        // TODO: Add mouseout event handler as well?
        $(window).bind({
            mousemove: mousemove,
            mouseup: mouseup
        });
        resizeTimer = setInterval(updatePlotSize, 100);

        return false;
    }

    function gotoTime(action) {
        var xAxis = dateAxis;
        if (!xAxis) {
            console.log("Missing date axis: cannot goto another time");
            return false;
        }
        var xMin = xAxis.getMin();
        var xMax = xAxis.getMax();
        var xWidth = xMax - xMin;

        if (action == "beginning") {
            var minTime = Number.MAX_VALUE;
            for (var channelKey in channelsMap) {
                var channel = channelsMap[channelKey];
                if (!!channel && channel.hasOwnProperty("min_time")) {
                    minTime = Math.min(minTime, channel["min_time"]);
                }
            }

            if (minTime < 0.99 * Number.MAX_VALUE) {
                xAxis.setRange(minTime, minTime + xWidth);
            }
        } else if (action == "back") {
            xAxis.setRange(xMin - xWidth, xMin);
        } else if (action == "forward") {
            xAxis.setRange(xMax, xMax + xWidth);
        } else if (action == "end") {
            var maxTime = -Number.MAX_VALUE;
            for (channelKey in channelsMap) {
                channel = channelsMap[channelKey];
                if (!!channel && channel.hasOwnProperty("max_time")) {
                    maxTime = Math.max(maxTime, channel["max_time"]);
                }
            }

            if (maxTime > -0.99 * Number.MAX_VALUE) {
                xAxis.setRange(maxTime - xWidth, maxTime);
            }
        } else {
            return false; // No change, so don't need to repaint plots
        }

        repaintAllPlots();

        return false;
    }

    function zoomTime(action) {
        var xAxis = dateAxis;
        if (!xAxis) {
            console.log("Missing date axis: cannot goto another time");
            return false;
        }
        var xMin = xAxis.getMin();
        var xMax = xAxis.getMax();
        var xWidth = xMax - xMin;
        var newXWidth = xWidth; // Just so newXWidth is always initialized

        if (action == "out") {
            newXWidth = xWidth * 1.4;
        } else if (action == "in") {
            newXWidth = xWidth / 1.4;
        } else {
            return false; // No change, so don't need to repaint plots
        }

        var dEndpoint = (newXWidth - xWidth) / 2.0;
        xAxis.setRange(xMin - dEndpoint, xMax + dEndpoint);

        repaintAllPlots();

        return false;
    }

    function setRange(start, end) {
        if (dateAxis) {
            dateAxis.setRange(start, end);
        } else {
            console.log("we don't have a dateAxis yet");
        }
        repaintAllPlots();
    }

    function repaintAllPlots() {
        for (var plotKey in plotsMap) {
            var plot = plotsMap[plotKey];
            if (plot) {
                plot.setStyle(plot.getStyle());
            }
        }
    }



    var timelineTab = new Tab("calendar", "timeline", "Candide Kemmler", "icon-film", false);
    var digest;

    function connectorDisplayable(connector){
        return connector.channelNames.length != 0;
    }

    function connectorsAlwaysEnabled(){
        return true;
    }


    var grapher = null;
    var connectorEnabled;

    function render(params) {
        digest = params.digest;
        connectorEnabled = params.connectorEnabled;
        this.getTemplate("text!applications/calendar/tabs/timeline/template.html", "timeline", function() {
            setup(digest, params.timeUnit);
        });
        timelineTab.setRange(digest.tbounds.start/1000, digest.tbounds.end/1000);
    }



    function setup(digest, timeUnit) {
        if (grapher == null) {
            grapher = new Grapher($("#timelineTabContainer"),{onLoad:function(){
                grapher.setRange(digest.tbounds.start/1000, digest.tbounds.end/1000);
                for (var connectorName in connectorEnabled){
                    connectorToggled(connectorName,null,connectorEnabled[connectorName]);
                }
                var prevDateString = null;
                grapher.dateAxis.addAxisChangeListener(function() {
                    var timeUnit = grapher.getCurrentTimeUnit();
                    var center = (grapher.dateAxis.getMin() +grapher. dateAxis.getMax()) / 2.0;
                    var date = new Date(center * 1000);
                    var dateChangeBuffer = 24 * 3600 * 1000 / 12;
                    var dateEarly = new Date(center * 1000 - dateChangeBuffer);
                    var dateLater = new Date(center * 1000 + dateChangeBuffer);
                    var dateString = Calendar.toDateString(date,timeUnit);
                    var dateStringEarly = Calendar.toDateString(dateEarly,timeUnit);
                    var dateStringLater = Calendar.toDateString(dateLater,timeUnit);
                    if (dateString != prevDateString && dateStringEarly != prevDateString && dateStringLater != prevDateString) {
                        Calendar.dateChanged(dateString, timeUnit);
                        prevDateString = dateString;
                    }
                });
            }});
        }
        else{
            $(window).resize();
        }
    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            if (connectorName == digest.selectedConnectors[i].connectorName){
                var channels = digest.selectedConnectors[i].channelNames;
                for (var i = 0; i < channels.length; i++){
                    if (enabled){
                        grapher.addChannel(channels[i]);
                    }
                    else{
                        grapher.removeChannel(channels[i]);
                    }

                }
                return;
            }
        }
    }

    function timeNavigation(nav){
        var splits = nav.split("/");
        switch (splits[0]){
            case "prev":
                grapher.gotoTime("back");
                break;
            case "next":
                grapher.gotoTime("forward");
                break;
            case "DAY":
                if (grapher.getCurrentTimeUnit() != "DAY"){
                    grapher.setZoom(24*3600);
                }
                break;
            case "WEEK":
                if (grapher.getCurrentTimeUnit() != "WEEK"){
                    grapher.setZoom(7*24*3600);
                }
                break;
            case "MONTH":
                if (grapher.getCurrentTimeUnit() != "MONTH"){
                    grapher.setZoom(30*24*3600);
                }
                break;
            case "YEAR":
                if (grapher.getCurrentTimeUnit() != "YEAR"){
                    grapher.setZoom(365*24*3600);
                }
                break;
            case "today":
                var end = new Date().getTime() / 1000;
                var diff = end - grapher.dateAxis.getMax();
                grapher.setRange(grapher.dateAxis.getMin() + diff, grapher.dateAxis.getMax() + diff);
                break;
            case "set":
                switch (splits[1]){
                    case "date":
                        var dateParts = splits[2].split("-");
                        var year = dateParts[0];
                        var month = parseInt(dateParts[1]) - 1;
                        var day = dateParts[2];
                        var start = new Date(year,month,day,0,0,0,0).getTime()/1000;
                        var end = new Date(year,month,day,11,59,59,999).getTime()/1000;
                        grapher.setRange(start,end);
                        break;
                    case "week":
                        var datePartsStart = splits[2].split("-");
                        var datePartsEnd = splits[3].split("-");
                        var yearStart = datePartsStart[0];
                        var monthStart = parseInt(datePartsStart[1]) - 1;
                        var dayStart = datePartsStart[2];
                        var yearEnd = datePartsEnd[0];
                        var monthEnd = parseInt(datePartsEnd[1]) - 1;
                        var dayEnd = datePartsEnd[2];
                        var start = new Date(yearStart,monthStart,dayStart,0,0,0,0).getTime()/1000;
                        var end = new Date(yearEnd,monthEnd,dayEnd,11,59,59,999).getTime()/1000;
                        grapher.setRange(start,end);
                        break;
                    case "month":
                        var year = splits[2];
                        var month = splits[3];
                        var start = new Date(year,month,1,0,0,0,0).getTime()/1000;
                        var end = new Date(year,month,App.getLastDayOfMonth(year,month),11,59,59,999).getTime()/1000;
                        grapher.setRange(start,end);
                        break;
                    case "year":
                        var year = splits[2];
                        var start = new Date(year,0,1,0,0,0,0).getTime() /1000;
                        var end = new Date(year,11,31,11,59,59,999).getTime() /1000;
                        grapher.setRange(start,end);
                        break;
                    default:
                        return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }

    timelineTab.initialized = false;
    timelineTab.render = render;
    timelineTab.connectorToggled = connectorToggled;
    timelineTab.setRange = setRange;
    timelineTab.connectorDisplayable = connectorDisplayable;
    timelineTab.connectorsAlwaysEnabled = connectorsAlwaysEnabled;
    timelineTab.timeNavigation = timeNavigation;
    return timelineTab;
});