define(function() {

    function show(){
        $.ajax("/api/v1/settings",{
            success:function(settings){
                App.loadAllMustacheTemplates("settingsTemplates.html",function(){
                    var dialogTemplate = App.fetchCompiledMustacheTemplate("settingsTemplates.html", "dialog");
                    var setPasswordTemplate = App.fetchCompiledMustacheTemplate("settingsTemplates.html", "setPassword");
                    var resetPasswordTemplate = App.fetchCompiledMustacheTemplate("settingsTemplates.html", "resetPassword");
                    if (settings["registrationMethod"]==="REGISTRATION_METHOD_FACEBOOK")
                        bindMainSettingsTemplate(dialogTemplate, setPasswordTemplate, settings);
                    else
                        bindMainSettingsTemplate(dialogTemplate, resetPasswordTemplate, settings);
                });
            }
        });
    }

    function renderLinkedAppsTemplate(settings) {
        var linkedAppsTemplate = App.fetchCompiledMustacheTemplate("settingsTemplates.html", "linkedApps");
        var accessTokens = settings["accessTokens"];
        for (var i=0; i<accessTokens.length; i++)
            accessTokens[i].createdAt = moment(accessTokens[i]["creationTime"]).calendar();
        settings.hasAccessTokens = !_.isUndefined(settings["accessTokens"])&&settings["accessTokens"].length > 0;
        return linkedAppsTemplate.render(settings);
    }

    function bindMainSettingsTemplate(template, passwordTemplate, settings){
        var html = template.render();
        App.makeModal(html);
        var renderedLinkedAppsTemplate = renderLinkedAppsTemplate(settings);
        renderWidgetRepositoriesTemplate();
        $("#apps-settings").append(renderedLinkedAppsTemplate);
        $("#password-settings").append(passwordTemplate.render());
        $("#username-uneditable").html(settings["username"]);
        $("#guest_username").val(settings["username"]);
        $("#guest_firstname").val(settings["firstName"]);
        $("#guest_lastname").val(settings["lastName"]);
        var lengthOptions = $("#length_measure_unit").children();
        for (var i = 0; i < lengthOptions.length; i++){
            if ($(lengthOptions[i]).attr("value") == settings.lengthMeasureUnit){
                $("#length_measure_unit")[0].selectedIndex = i;
                break;
            }
        }
        var distanceOptions = $("#distance_measure_unit").children();
        for (var i = 0; i < distanceOptions.length; i++){
            if ($(distanceOptions[i]).attr("value") == settings.distanceMeasureUnit){
                $("#distance_measure_unit")[0].selectedIndex = i;
                break;
            }
        }
        var weightOptions = $("#weight_measure_unit").children();
        for (var i = 0; i < weightOptions.length; i++){
            if ($(weightOptions[i]).attr("value") == settings.weightMeasureUnit){
                $("#weight_measure_unit")[0].selectedIndex = i;
                break;
            }
        }
        var temperatureOptions = $("#temperature_unit").children();
        for (var i = 0; i < temperatureOptions.length; i++){
            if ($(temperatureOptions[i]).attr("value") == settings.temperatureUnit){
                $("#temperature_unit")[0].selectedIndex = i;
                break;
            }
        }
        $("#saveSettingsBtn").click(function(event){
            event.preventDefault();
            var settingsId = $("ul#settingsTabs li.active").attr("id");
            switch (settingsId) {
                case "generalSettings":
                    saveGeneralSettings(settings);
                    break;
                case "passwordSettings":
                    savePasswordSettings();
                    break;
                case "unitsSettings":
                    saveUnitsSettings();
                    break;
            }
        });
        bindLinkedAppsTemplate();
        bindWidgetRepositoriesTemplate();
        $("#settingsTabs").tab();
    }

    function renderWidgetRepositoriesTemplate() {
        var widgetRepositoriesTemplate = App.fetchCompiledMustacheTemplate("settingsTemplates.html", "widgetRepositories");
        $.ajax({
            url: "/api/v1/repositories/",
            type: "GET",
            success: function(repositoryURLs, statusText, jqXHR) {
                var renderedWidgetRepositoriesTemplate = widgetRepositoriesTemplate.render({urls:repositoryURLs, hasUrls:repositoryURLs.length>0});
                $("#widgetRepositoriesWrapper").empty().append(renderedWidgetRepositoriesTemplate);
                bindWidgetRepositoriesTemplate();
            },
            error: function(jqXHR, statusText, errorThrown) {
                var errorMessage = errorThrown + ": " + jqXHR.responseText;
                console.log(errorMessage);
                $("#apps-settings").empty().append("<h4>Something went wrong... please contact us</h1>")
            }
        });
    }

    function bindWidgetRepositoriesTemplate() {
        $(".removeWidgetRepositoryURL").unbind().click(function(event){
            var url = $(event.target).attr("data-url");
            $.ajax({
                url: "/api/v1/repositories/",
                type: "DELETE",
                data: { url : url },
                success: function(body, statusText, jqXHR) {
                    console.log(body);
                    renderWidgetRepositoriesTemplate();
                },
                error: function(jqXHR, statusText, errorThrown) {
                    var errorMessage = errorThrown + ": " + jqXHR.responseText;
                    console.log(errorMessage);
                    $("#widgetRepositoriesWrapper").empty().append("<h4>Something went wrong... please contact us</h1>")
                }
            });
        });
        var addWidgetRepository = function (){
            var url = $("#widgetRepositoryURL").val();
            $.ajax({
                url: "/api/v1/repositories/",
                type: "POST",
                data: { url : url },
                success: function(body, statusText, jqXHR) {
                    console.log(body);
                    renderWidgetRepositoriesTemplate();
                },
                error: function(jqXHR, statusText, errorThrown) {
                    var errorMessage = errorThrown + ": " + jqXHR.responseText;
                    alert(errorMessage);
                }
            });
        };
        $("#widgetRepositoryURL").keypress(function(e) {
           if (e.which==13)
           addWidgetRepository();
        });
        $("#addWidgetRepositoryURL").unbind().click(addWidgetRepository);
    }

    function bindLinkedAppsTemplate() {
        $(".revokeAccessToken").unbind();
        $(".revokeAccessToken").click(function(event){
            var accessToken = $(event.target).attr("data-accessToken");
            $.ajax({
                url: "/api/v1/settings/accessTokens/" + accessToken,
                type: "DELETE",
                success: function(body, statusText, jqXHR) {
                    console.log(body);
                    $.ajax("/api/v1/settings",{
                        success: function(settings, statusText, jqXHR) {
                            var renderedLinkedAppsTemplate = renderLinkedAppsTemplate(settings);
                            $("#apps-settings").empty().append(renderedLinkedAppsTemplate);
                            bindLinkedAppsTemplate();
                        },
                        error: function(jqXHR, statusText, errorThrown) {
                            var errorMessage = errorThrown + ": " + jqXHR.responseText;
                            console.log(errorMessage);
                            alert("Could load settings: " + errorMessage);
                        }
                    });
                },
                error: function(jqXHR, statusText, errorThrown) {
                    var errorMessage = errorThrown + ": " + jqXHR.responseText;
                    console.log(errorMessage);
                    $("#apps-settings").empty().append("<h4>Something went wrong... please contact us</h1>")
                }
            });
        });
    }

    function saveGeneralSettings(settings) {
        var formData = $("#generalSettingsForm").serializeArray();
        var submitdata = {};
        for (var i = 0; i < formData.length; i++) {
            submitdata[formData[i].name] = formData[i].value;
        }
        $.ajax("/api/v1/settings/general",{
            type:"POST",
            data:submitdata,
            success: function(body, statusText, jqXHR) {
                App.closeModal();
                var nameDisplay = $("#loggedInUser");
                var newNameEncoded = App.htmlEscape($("#guest_firstname").val() + " " + $("#guest_lastname").val());
                var oldNameEncoded = App.htmlEscape(settings["firstName"] + " " + settings["lastName"]);
                nameDisplay.html(nameDisplay.html().replace(oldNameEncoded, newNameEncoded));
            },
            error: function(jqXHR, statusText, errorThrown) {
                var errorMessage = errorThrown + ": " + jqXHR.responseText;
                console.log(errorMessage);
                App.closeModal();
            }
        });
    }

    function savePasswordSettings() {
        var formData = $("#passwordSettingsForm").serializeArray();
        var submitdata = {};
        for (var i = 0; i < formData.length; i++) {
            submitdata[formData[i].name] = formData[i].value;
        }
        $("#setPasswordError").hide();
        $.ajax("/api/v1/settings/password",{
            type:"POST",
            data:submitdata,
            success:function(status) {
                if (status.result==="OK"){
                    App.closeModal();
                }
                else {
                    $("#setPasswordError").show();
                    $("#setPasswordError").html(status.message);
                }
            },
            error: function(jqXHR, statusText, errorThrown) {
                var errorMessage = errorThrown + ": " + jqXHR.responseText;
                console.log(errorMessage);
                App.closeModal();
            }
        });
    }

    function saveUnitsSettings() {
        var formData = $("#unitsSettingsForm").serializeArray();
        var submitdata = {};
        for (var i = 0; i < formData.length; i++) {
            submitdata[formData[i].name] = formData[i].value;
        }
        $.ajax("/api/v1/settings/units",{
            type:"POST",
            data:submitdata,
            success: function(body, statusText, jqXHR) {
                    App.closeModal();
            },
            error: function(jqXHR, statusText, errorThrown) {
                var errorMessage = errorThrown + ": " + jqXHR.responseText;
                console.log(errorMessage);
                App.closeModal();
            }
        });
    }

    var Settings = {};
    Settings.show = show;
    return Settings;

});