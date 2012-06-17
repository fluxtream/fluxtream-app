define(function() {

    var addresses;
    var typeNames = ["ADDRESS_OTHER","ADDRESS_HOME","ADDRESS_WORK"];
    var currentAddressPool = [];

    function buildDialog(){
        App.loadMustacheTemplate("addressesTemplate.html","addressesDialog",function(template){
            App.makeModal(template.render({addresses:addresses.map(function(address){
                return getAddressParams(address);
            })}));
            $("#addAddress").click(addAddressDialog);


            for (var i = 0; i < addresses.length; i++){
                $("#delete-" + i).click({index:i}, function(event){
                    confirmDelete(event.data.index);
                });
                    $("#edit-" + i).click({index:i}, function(event){
                    updateAddressDialog(event.data.index);
                });
            }
        });

    }
    function getAddressParams(address){
        var params = {
           address:address.address,
           type:address.type,
           index:address.index,
           since:App.formatDate(address.since),
           until:App.formatDate(address.until)
        };
        return params;
    }

    function confirmDelete(index){
        App.closeModal();
        $("#modal").on("hidden", function(){
            App.loadMustacheTemplate("addressesTemplate.html","deleteAddressConfirm",function(tamplate){
                App.makeModal(tamplate.render({address:addresses[index].address}));


                var confirmDelete = $("#confirmDeleteBtn");

                confirmDelete.click(function(){
                    $.ajax("/api/addresses/" + index,{
                        type:"DELETE",
                        success:App.closeModal,
                        error: App.closeModal
                    });

                });

                $("#modal").on("hidden",show);
            });
        });
    }

    function addressDialogInitializer(html){
        App.makeModal(html);
        var addressInput = $("#addressInput");
        var addressSearch = $("#addressSearch");
        var addressSelect = $("#addressSelect");
        var sinceInput = $("#sinceInput");
        var untilInput = $("#untilInput");
        var presentCheckbox = $("#presentCheckBox");
        var addressTypeSelect = $("#addressTypeSelect");
        var saveAddressBtn = $("#saveAddressBtn");

        //sinceInput.datepicker({format:"yyyy-mm-dd"});
        sinceInput.datepicker().on("changeDate",function(){
            sinceInput.datepicker("hide");
            sinceInput.blur();
            sinceInput.parent().parent().removeClass("error");
        });

        //untilInput.datepicker().datepicker({format:"yyyy-mm-dd"});
        untilInput.datepicker().on("changeDate",function(){
            untilInput.datepicker("hide");
            untilInput.blur();
            untilInput.parent().parent().removeClass("error");
        });



        presentCheckbox.change(function(){
            if (presentCheckbox.is(":checked")){
                untilInput.val("");
                untilInput.attr("disabled","disabled");
            }
            else
                untilInput.removeAttr("disabled");
            untilInput.parent().parent().removeClass("error");
        });

        addressInput.keyup(function(event){
            if (event.keyCode == 13)
                addressSearch.click();
            else{
                var options = addressSelect.children();
                for (var i = 1; i < options.length; i++)
                    $(options[i]).remove();
                currentAddressPool = [];
            }
        })

        addressSearch.click(function(){
            var addr = addressInput.val();
            addressSelect.attr("disabled","disabled");
            addressSearch.attr("disabled","disabled");
            addressInput.attr("disabled","disabled");
            App.geocoder.geocode({"address":addr},function(results,status){
                var options = addressSelect.children();
                for (var i = 1; i < options.length; i++)
                    $(options[i]).remove();
                if (status == google.maps.GeocoderStatus.OK) {
                    for (var i = 0; i < results.length; i++){
                        addressSelect.append('<option>' + results[i].formatted_address + '</option>')
                    }
                    currentAddressPool = results;
                    for (var i = 0; i < currentAddressPool.length; i++){
                        if (currentAddressPool[i].formatted_address == addressInput.val())
                            addressSelect[0].selectedIndex = i + 1;
                    }
                }
                else{
                    currentAddressPool = [];
                }
                addressSelect.removeAttr("disabled");
                addressSearch.removeAttr("disabled");
                addressInput.removeAttr("disabled");
            });
        });

        addressSelect.change(function(){
            if (addressSelect[0].selectedIndex != 0)
                addressSelect.parent().parent().removeClass("error");
        });

        $("#modal").on("hidden",show);

    }

    function updateAddressDialog(index){
        var originalSince = App.formatDateAsDatePicker(addresses[index].since);
        var originalUntil = App.formatDateAsDatePicker(addresses[index].until);
        App.closeModal();
        $("#modal").on("hidden", function(){
            App.loadMustacheTemplate("addressesTemplate.html","addAddress",function(template){
                addressDialogInitializer(template.render({
                     title:"Edit Address",
                     sinceDate:originalSince,
                     untilDate:originalUntil == "Present" ? "" : originalUntil
                 }),originalSince,originalUntil);

                var addressInput = $("#addressInput");
                var addressSearch = $("#addressSearch");
                var addressSelect = $("#addressSelect");
                var sinceInput = $("#sinceInput");
                var untilInput = $("#untilInput");
                var presentCheckbox = $("#presentCheckBox");
                var addressTypeSelect = $("#addressTypeSelect");
                var saveAddressBtn = $("#saveAddressBtn");


                addressInput.val(addresses[index].address);
                addressSearch.click();
                if (originalUntil == "Present")
                    presentCheckbox.click();

                var typeIndex = 0;
                for (var i = 1; i < typeNames.length; i++){
                    if (typeNames[i] == addresses[index].type)
                        typeIndex = i;
                }
                addressTypeSelect[0].selectedIndex = typeIndex;

                saveAddressBtn.click(function(){
                    var selection = addressSelect[0].selectedIndex - 1;
                    var errors = false;
                    if (selection == -1){
                        addressSelect.parent().parent().addClass("error");
                        errors = true;
                    }
                    if (sinceInput.val() == ""){
                        sinceInput.parent().parent().addClass("error");
                        errors = true;
                    }
                    if (untilInput.val() == "" && !presentCheckbox.is(":checked")){
                        untilInput.parent().parent().addClass("error");
                        errors = true;
                    }
                    if (errors)
                        return;
                    addressInput.attr("disabled","disabled");
                    addressSearch.attr("disabled","disabled");
                    addressSelect.attr("disabled","disabled");
                    sinceInput.attr("disabled","disabled");
                    untilInput.attr("disabled","disabled");
                    presentCheckbox.attr("disabled","disabled");
                    addressTypeSelect.attr("disabled","disabled");
                    saveAddressBtn.attr("disabled","disabled");
                    var address = currentAddressPool[selection];
                    var params = {};
                    var hasParams = false;
                    if (address.formatted_address != addresses[index].address){
                        params.address = address.formatted_address;
                        params.latitude = address.geometry.location.lat();
                        params.longitude = address.geometry.location.lng();
                        hasParams = true;
                    }
                    if (originalSince != sinceInput.val()){
                        params.since = sinceInput.val();
                        hasParams = true;
                    }
                    if (presentCheckbox.is(":Checked")){
                        if (originalUntil != "Present"){
                            params.until = "Present";
                            hasParams = true;
                        }
                    }
                    else if (originalUntil != untilInput.val()){
                        params.until = untilInput.val();
                        hasParams = true;
                    }
                    if (addresses[index].type != typeNames[addressTypeSelect[0].selectedIndex]){
                        params.type = typeNames[addressTypeSelect[0].selectedIndex];
                        hasParams = true;
                    }
                    if (hasParams){
                        $.ajax("/api/addresses/" + index,{
                            type:"POST",
                            data:params,
                            success: function(data, textStatus, jqXHR){
                                if (data.result == "OK")
                                    App.closeModal();
                                else
                                    this.error();
                            },
                            error: function(){
                                $(".modal-body").append('<div class="alert alert-error"><button class="close" data-dismiss="alert">×</button><strong>Error!</strong> Failed to add address!</div>');
                                addressInput.removeAttr("disabled");
                                addressSearch.removeAttr("disabled");
                                addressSelect.removeAttr("disabled");
                                sinceInput.removeAttr("disabled");
                                if (!presentCheckbox.is(":Checked"))
                                    untilInput.removeAttr("disabled");
                                presentCheckbox.removeAttr("disabled");
                                addressTypeSelect.removeAttr("disabled");
                                saveAddressBtn.removeAttr("disabled");
                            }
                        });
                     }
                    else{
                        App.closeModal();
                    }
                });
            });
        });
    }

    function addAddressDialog(){
        App.closeModal();
        $("#modal").on("hidden", function(){
            App.loadMustacheTemplate("addressesTemplate.html","addAddress",function(template){
                addressDialogInitializer(template.render({
                     title:"Add Address",
                     sinceDate: App.formatDateAsDatePicker(new Date()),
                     untilDate: App.formatDateAsDatePicker(new Date())
                }));

                var addressInput = $("#addressInput");
                var addressSearch = $("#addressSearch");
                var addressSelect = $("#addressSelect");
                var sinceInput = $("#sinceInput");
                var untilInput = $("#untilInput");
                var presentCheckbox = $("#presentCheckBox");
                var addressTypeSelect = $("#addressTypeSelect");
                var saveAddressBtn = $("#saveAddressBtn");

                sinceInput.val("");
                untilInput.val("");

                currentAddressPool = [];

                saveAddressBtn.click(function(){
                    var selection = addressSelect[0].selectedIndex - 1;
                    var errors = false;
                    if (selection == -1){
                        addressSelect.parent().parent().addClass("error");
                        errors = true;
                    }
                    if (sinceInput.val() == ""){
                        sinceInput.parent().parent().addClass("error");
                        errors = true;
                    }
                    if (untilInput.val() == "" && !presentCheckbox.is(":checked")){
                        untilInput.parent().parent().addClass("error");
                        errors = true;
                    }
                    if (errors)
                        return;
                    addressInput.attr("disabled","disabled");
                    addressSearch.attr("disabled","disabled");
                    addressSelect.attr("disabled","disabled");
                    sinceInput.attr("disabled","disabled");
                    untilInput.attr("disabled","disabled");
                    presentCheckbox.attr("disabled","disabled");
                    addressTypeSelect.attr("disabled","disabled");
                    saveAddressBtn.attr("disabled","disabled");
                    var address = currentAddressPool[selection];
                    var params = {address:address.formatted_address, latitude:address.geometry.location.lat(),
                        longitude:address.geometry.location.lng(), since:sinceInput.val()};
                    if (!presentCheckbox.is(":checked"))
                        params.until = untilInput.val();
                    $.ajax("/api/addresses/" + typeNames[addressTypeSelect[0].selectedIndex],{
                        type: "POST",
                        data:  params,
                        success: function(data, textStatus, jqXHR){
                            if (data.result == "OK")
                                App.closeModal();
                            else
                                this.error();
                        },
                        error: function(){
                            $(".modal-body").append('<div class="alert alert-error"><button class="close" data-dismiss="alert">×</button><strong>Error!</strong> Failed to add address!</div>');
                            addressInput.removeAttr("disabled");
                            addressSearch.removeAttr("disabled");
                            addressSelect.removeAttr("disabled");
                            sinceInput.removeAttr("disabled");
                            if (!presentCheckbox.is(":Checked"))
                                untilInput.removeAttr("disabled");
                            presentCheckbox.removeAttr("disabled");
                            addressTypeSelect.removeAttr("disabled");
                            saveAddressBtn.removeAttr("disabled");
                        }
                    });
                });

            });
        });
    }


    function show(){
        $.ajax("/api/addresses",{
            success: function(data, textStatus, jqXHR){
                    dataLoaded(data);
                }
        });
    }

    function dataLoaded(data){
        addresses = data;
        for (var i = 0; i < addresses.length; i++){
            addresses[i].index = i;
            addresses[i].since += 12 * 3600 * 1000;
            addresses[i].until -= 12 * 3600 * 1000;
        }
        buildDialog();
    }


    var Addresses = {};
    Addresses.show = show;
    return Addresses;
});
