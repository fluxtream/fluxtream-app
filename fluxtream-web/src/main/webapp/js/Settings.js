define(function() {

    function show(){
        var mainSettingsTemplate;
        $.ajax("/api/settings",{
            success:function(settings){
                App.loadAllMustacheTemplates("settingsTemplates.html",function(){
                    var dialogTemplate = App.fetchCompiledMustacheTemplate("settingsTemplates.html", "dialog");
                    var setPasswordTemplate = App.fetchCompiledMustacheTemplate("settingsTemplates.html", "setPassword");
                    var resetPasswordTemplate = App.fetchCompiledMustacheTemplate("settingsTemplates.html", "resetPassword");
                    if (settings.registrationMethod==="REGISTRATION_METHOD_FACEBOOK")
                        bindMainSettingsTemplate(dialogTemplate, setPasswordTemplate, settings);
                    else
                        bindMainSettingsTemplate(dialogTemplate, resetPasswordTemplate, settings);
                });
            }
        });
    }

    function bindMainSettingsTemplate(template, passwordTemplate, settings){
        var html = template.render();
        App.makeModal(html);
        $("#password-settings").append(passwordTemplate.render());
        $("#username-uneditable").html(settings.username);
        $("#guest_firstname").val(settings.firstName);
        $("#guest_lastname").val(settings.lastName);
        var lengthOptions = $("#length_measure_unit").children();
        for (var i = 0; i < lengthOptions.length; i++){
            if ($(lengthOptions[i]).attr("value") == settings.lengthMeasureUnit){
                $("#length_measure_unit")[0].selectedIndex = i;
                break;
            }
        }
        var distanceOptions = $("#distance_measure_unit").children();
        for (var i = 0; i < distanceOptions.length; i++){
            if ($(distanceOptions[i]).attr("value") == settings.distanceUnit){
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
            var formData = $("#settingsForm").serializeArray();
            var submitdata = {};
            for (var i = 0; i < formData.length; i++) {
                submitdata[formData[i].name] = formData[i].value;
            }
            $("#setPasswordError").hide();
            $.ajax("/api/settings",{
                type:"POST",
                data:submitdata,
                success:function(status) {
                    if (status.result=="OK"){
                        App.closeModal();
                        var nameDisplay = $("#loggedInUser");
                        var newNameEncoded = App.htmlEscape($("#guest_firstname").val() + " " + $("#guest_lastname").val());
                        var oldNameEncoded = App.htmlEscape(settings.firstName + " " + settings.lastName);
                        nameDisplay.html(nameDisplay.html().replace(oldNameEncoded,newNameEncoded));
                    }
                    else {
                        $("#setPasswordError").show();
                        $("#setPasswordError").html(status.message);
                    }
                },
                error:App.closeModal
            });
        });
        $("#settingsTabs").tab();
    }

    var Settings = {};
    Settings.show = show;
    return Settings;

});