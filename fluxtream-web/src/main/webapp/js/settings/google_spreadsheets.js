define(["core/ConnectorSettingsHandler"], function (ConnectorSettingsHandler) {

    var GoogleSpreadsheetsSettingsHandler = new ConnectorSettingsHandler("google_spreadsheets");

    function getDocumentMetadata(settings, documentId) {
        for (var i=0; i<settings.documents.length; i++) {
            if (settings.documents[i].id==Number(documentId))
                return settings.documents[i];
        }
    }

    GoogleSpreadsheetsSettingsHandler.bindSettings = function (container, settings, apiKeyId) {
        if (settings.documents.length==0) {
            $("#documentSelect").empty().append("<option>No Spreadsheet currently available</option>")
        }
        $("#settingsImportSpreadsheetButton").click(function(e){
            App.loadMustacheTemplate("connectorMgmtTemplates.html","spreadsheetBrowser",function(template){
                handleSpreadsheetBrowser(template, this.connector);
            });
        });
        $("#documentSelect").change(function (e) {
            e.preventDefault();
            $("#spreadsheetEditForm").remove();
            var documentId = $("#documentSelect").val();
            var documentMetadata = getDocumentMetadata(settings, documentId);
            App.loadMustacheTemplate("connectorMgmtTemplates.html","google_spreadsheets-spreadsheetEditForm",function(template){
                var html = template.render(documentMetadata);
                $("#googleSpreadsheetsSettings").append(html);
                $("#spreadsheetEditForm").show();
            });
            $("#deleteSpreadsheetButton").unbind().click(function(e){
                $.ajax("/api/v1/spreadsheets/document/" + documentId, {
                    type: "DELETE",
                    success: function() {
                        console.log("We should reload the settings");
                        GoogleSpreadsheetsSettingsHandler.reloadSettings(GoogleSpreadsheetsSettingsHandler);
                    },
                    error: function(jqXHR, statusText, errorThrown) {
                        var errorMessage = errorThrown + ": " + jqXHR.responseText + ": " + statusText;
                        console.log("error (delete spreasheet): " + errorMessage);
                    }
                });
            });
            $("#collectionLabel").unbind().blur(function(e){
                $.ajax("/api/v1/spreadsheets/document/" + documentId + "/collectionLabel", {
                    data : {label : $("#collectionLabel").val()},
                    type: "PUT"
                });
            });
            $("#itemLabel").unbind().blur(function(e){
                $.ajax("/api/v1/spreadsheets/document/" + documentId + "/itemLabel", {
                    data : {label : $("#itemLabel").val()},
                    type    : "PUT"
                });
            });
        });
    };

    function handleSpreadsheetBrowser(template, connector) {
        $.ajax({
            url: "/api/v1/spreadsheets/",
            success: function(spreadsheets) {
                var html = template.render({connector:connector});
                App.makeModal(html);
                for (var i=0; i<spreadsheets.length; i++) {
                    $("#spreadsheetSelect").append('<option value="' + spreadsheets[i]["id"] + '">' + spreadsheets[i]["title"] + '</option>');
                }
                $("#importSpreadsheetButton").prop("disabled", true);
                $("#worksheetSelect").empty().prop("disabled", true);
                $("#worksheetSection").hide();
                //$("#dateTimeFormatSection").hide();
                $("#timeZoneSection").hide();
                $("#timeZoneSelect").val("none");
                $("#dateTimeTypeSelect").val("none").prop("disabled", true);
                $("#dateTimeFieldSelect").empty().prop("disabled", true);
                $("#collectionLabel").unbind().blur(function(e){
                    checkFormReady();
                });
                $("#itemLabel").unbind().blur(function(e){
                    checkFormReady();
                });
                $("#spreadsheetSelect").change(function(){
                    var spreadsheetId = $("#spreadsheetSelect").val();
                    loadSpreadsheetInfo(spreadsheetId);
                });
                $("#importSpreadsheetCancelButton").click(function(e) {
                    e.preventDefault();
                    $("#spreadsheetBrowserModal").modal("hide");
                });
                $("#importSpreadsheetButton").click(function(e) {
                    e.preventDefault();
                    console.log("import button pushed");
                    var spreadsheetId = $("#spreadsheetSelect").val();
                    var worksheetId = $("#worksheetSelect").val();
                    var dateTimeField = $("#dateTimeFieldSelect").val();
                    var dateTimeFormat = $("#dateTimeTypeSelect").val();
                    var collectionLabel = $("#collectionLabel").val();
                    var itemLabel = $("#itemLabel").val();
                    var timeZone = null;
                    if (!dateTimeFormat.endsWith("Z"))
                        timeZone = $("#timeZoneSelect").val();
                    $.ajax({
                        url: "/api/v1/spreadsheets/",
                        type: "POST",
                        data: {
                            spreadsheetId : spreadsheetId,
                            worksheetId : worksheetId,
                            dateTimeField : dateTimeField,
                            dateTimeFormat : dateTimeFormat,
                            collectionLabel : collectionLabel,
                            itemLabel : itemLabel,
                            timeZone : timeZone
                        },
                        success: function() {
                            console.log("ok");
                            GoogleSpreadsheetsSettingsHandler.reloadSettings(GoogleSpreadsheetsSettingsHandler);
                        },
                        error: function(jqXHR, statusText, errorThrown) {
                            console.log("error");
                            var errors = JSON.parse(jqXHR.responseText);
                            if (errors["missing"]){
                                for (var i=0; i<errors["missing"].length;i++) {
                                    var labelName = errors["missing"][i];
                                    $("#"+labelName+"-controls").remove(".help-inline").append("<div class='help-inline'>This field cannot be empty</div>");
                                }
                            } else if (errors["other"]){
                                alert(errors["other"]);
                            }
                        }
                    });
                });
            }
        });
    }

    function loadSpreadsheetInfo(spreadsheetId) {
        $.ajax({
            url: "/api/v1/spreadsheets/worksheets",
            data: {spreadsheetId : spreadsheetId},
            success: function(worksheets) {
                $("#worksheetSelect").empty().prop("disabled", false);
                $("#importSpreadsheetButton").prop("disabled", true);
                $("#dateTimeFieldSelect").empty().prop("disabled", true);
                $("#dateTimeTypeSelect").prop("disabled", true);
                //$("#dateTimeFormatSection").val("").hide();
                $("#timeSection").hide();
                $("#timeZoneSelect").val("none");
                $("#worksheetSection").show();
                //$("#dateTimeFormatSection").val("").hide();
                $("#timeZoneSection").val("none").hide();
                if (worksheets.length>1) {
                    $("#worksheetSelect").append('<option>Please choose one</option>');
                    for (var i = 0; i < worksheets.length; i++) {
                        $("#worksheetSelect").append('<option value="' + worksheets[i]["id"] + '">' + worksheets[i]["title"] + '</option>');
                    }
                    $("#worksheetSelect").change(function(){
                        var worksheetId = $("#worksheetSelect").val();
                        if (!_.isUndefined(worksheetId)&&worksheetId!=null) {
                            loadWorksheetInfo(spreadsheetId, worksheetId);
                        }
                    });
                } else {
                    $("#worksheetSection").hide();
                    loadWorksheetInfo(spreadsheetId, worksheets[0]["id"]);
                }
            }
        });
    }

    function loadWorksheetInfo(spreadsheetId, worksheetId) {
        $("#dateTimeFormatSelect").prop("disabled", true);
        //$("#dateTimeFormatSection").val("").hide();
        $("#timeZoneSection").hide();
        $("#dateTimeFieldSelect").empty().prop("disabled", true);
        $("#dateTimeTypeSelect").val("none").prop("disabled", true);
        $("#importSpreadsheetButton").prop("disabled", true);
        $("#timeZoneSelect").val("none");
        $.ajax({
            url: "/api/v1/spreadsheets/worksheet",
            data: {spreadsheetId : spreadsheetId, worksheetId : worksheetId},
            success: function(worksheet) {
                $("#dateTimeFieldSelect").empty().prop("disabled", false);
                var alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                $("#dateTimeFieldSelect").append('<option>Please choose one</option>');
                for (var i=0; i<worksheet["columnNames"].length; i++) {
                    var letterIndex = i%alphabet.length;
                    var prependIndex = Math.floor(i/alphabet.length);
                    var letter = alphabet.charAt(letterIndex);
                    var columnLetter = (prependIndex>0)
                        ? alphabet.charAt(prependIndex-1) + letter
                        : letter;
                    var value = worksheet["columnNames"][i];
                    if (value==null) value = columnLetter;
                    $("#dateTimeFieldSelect").append('<option value="' + value + '">' + value + '</option>');
                }
                $("#dateTimeFieldSelect").change(function(){
                    //$("#dateTimeFormatSection").val("").hide();
                    $("#timeZoneSection").hide();
                    $("#timeZoneSelect").val("none");
                    $("#importSpreadsheetButton").prop("disabled", true);
                    $("#dateTimeTypeSelect > option").each(function() {
                        var format = $(this).attr("data-momentjs-format");
                        if (!_.isUndefined(format)) {
                            var formatted = moment().format(format);
                            if (format.indexOf("Z")==-1)
                                formatted += " (Local Time)"
                            $(this).text(formatted);
                        }
                    });
                    $("#dateTimeTypeSelect").prop("disabled", false).val("none");
                    $("#dateTimeTypeSelect").change(function(){
                        var selectedFormat = $("#dateTimeTypeSelect").val();
                        if (_.contains(["none","epochSeconds","epochMillis"], selectedFormat)) {
                            $("#timeZoneSection").hide();
                            if($("#itemLabel").val()&&$("#collectionLabel").val())
                                $("#importSpreadsheetButton").prop("disabled", false);
                            return;
                        }
                        //if (selectedFormat==="format") {
                        //    $("#dateTimeFormatSection").show();
                        if (selectedFormat.indexOf("Z")==-1) {
                            // no time zone in format, it has to be specified manually
                            $("#importSpreadsheetButton").prop("disabled", true);
                            $("#timeZoneSection").show();
                            $("#timeZoneSelect").val("none").change(function(){
                                checkFormReady();
                            });
                        } else {
                            $("#timeZoneSection").hide();
                            checkFormReady()
                        }
                    });
                });
            }
        });
    }

    function checkFormReady() {
        console.log("hahah");
        var selectedFormat = $("#dateTimeTypeSelect").val();
        if (!selectedFormat) return;
        else if (selectedFormat.indexOf("Z")==-1)
            return;
        var itemLabelVal = $("#itemLabel").val();
        var collectionLabelVal = $("#collectionLabel").val();
        var itemLabelControls = $("#itemLabel-controls");
        var collectionLabelControls = $("#collectionLabel-controls");
        itemLabelControls.remove(".help-inline");
        collectionLabelControls.remove(".help-inline");
        itemLabelControls.parent().removeClass("error");
        if(itemLabelVal&&collectionLabelVal)
            $("#importSpreadsheetButton").prop("disabled", false);
        else {
            if (!itemLabelVal) {
                itemLabelControls.append("<div class='help-inline'>This field cannot be empty</div>");
                itemLabelControls.parent().addClass("error");
            } else if (!collectionLabelVal) {
                collectionLabelControls.append("<div class='help-inline'>This field cannot be empty</div>");
                collectionLabelControls.parent().addClass("error");
            }
        }
    }

    GoogleSpreadsheetsSettingsHandler.handleSpreadsheetBrowser = handleSpreadsheetBrowser;
    return GoogleSpreadsheetsSettingsHandler;

});