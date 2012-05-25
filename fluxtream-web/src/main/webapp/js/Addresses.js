define(function() {

    var addresses;
    var geocoder;
    var typeNames = ["ADDRESS_OTHER","ADDRESS_HOME","ADDRESS_WORK"];

    function buildDialog(){
        buildAddressRows(function(rowHTML){
            App.loadHTMLTemplate("addressesTemplate.html","addressesDialog",{
                tableContents:rowHTML
            },function(html){
                App.makeModal(html);
                $("#addAddress").click(addAddressDialog);


                for (var i = 0; i < addresses.length; i++){
                    $("#delete-" + i).click({index:i}, function(event){
                        confirmDelete(event.data.index);
                    });

                }
            })
        });

    }

    function buildAddressRows(onDone){
        var rowHTML = "";
        if (addresses.length == 0)
            onDone(rowHTML);
        var i = 0;
        App.loadHTMLTemplate("addressesTemplate.html","addressRow",getAddressParams(i),function(html){
            rowHTML += html;
            i++;
            if (i == addresses.length)
                onDone(rowHTML);
            else
                App.loadHTMLTemplate("addresseTemplate.html","addressRow",getAddressParams(i),this);
        });
    }

    function getAddressParams(index){
        var params = {
           address:addresses[index].address,
           type:addresses[index].type,
           index:index
        };
        var sinceDate = new Date(addresses[index].since);
        params.since = sinceDate.getFullYear() + '-' + (sinceDate.getMonth() + 1) + '-' + sinceDate.getDate();
        var untilDate = new Date(addresses[index].until);
        if (isNaN(untilDate.getFullYear()))
            params.until = "Present";
        else
            params.until = untilDate.getFullYear() + '-' + (untilDate.getMonth() + 1) + '-' + untilDate.getDate();
        return params;
    }

    function confirmDelete(index){
        App.closeModal();
        $("#modal").on("hidden", function(){
            App.loadHTMLTemplate("addressesTemplate.html","deleteConfirm",{address:addresses[index].address},function(html){
                App.makeModal(html);


                var confirmDelete = $("#confirmDeleteBtn");

                confirmDelete.click(function(){
                    $.ajax("/api/guest/" + App.getUsername() + "/address/" + index,{
                        type:"DELETE",
                        success:App.closeModal,
                        error: App.closeModal
                    });

                });

                $("#modal").on("hidden",show);
            });
        });
    }

    function addAddressDialog(){
        App.closeModal();
        $("#modal").on("hidden", function(){
            App.loadHTMLTemplate("addressesTemplate.html","addAddress",{},function(html){
                App.makeModal(html);

                var addressInput = $("#addressInput");
                var addressSearch = $("#addressSearch");
                var addressSelect = $("#addressSelect");
                var sinceInput = $("#sinceInput");
                var untilInput = $("#untilInput");
                var presentCheckbox = $("#presentCheckBox");
                var addressTypeSelect = $("#addressTypeSelect");
                var saveAddressBtn = $("#saveAddressBtn");

                var currentAddressPool = [];

                presentCheckbox.change(function(){
                    if (presentCheckbox.is(":checked"))
                        untilInput.attr("disabled","disabled");
                    else
                        untilInput.removeAttr("disabled");
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
                    geocoder.geocode({"address":addr},function(results,status){
                        var options = addressSelect.children();
                        for (var i = 1; i < options.length; i++)
                            $(options[i]).remove();
                        if (status == google.maps.GeocoderStatus.OK) {
                            for (var i = 0; i < results.length; i++){
                                addressSelect.append('<option>' + results[i].formatted_address + '</option>')
                            }
                            currentAddressPool = results;
                        }
                        else{
                            currentAddressPool = [];
                        }
                        addressSelect.removeAttr("disabled");
                    });
                });

                addressSelect.change(function(){
                    if (addressSelect[0].selectedIndex != 0)
                        addressSelect.parent().parent().removeClass("error");
                });

                saveAddressBtn.click(function(){
                    var selection = addressSelect[0].selectedIndex - 1;
                    if (selection == -1){
                        addressSelect.parent().parent().addClass("error");
                        return;
                    }
                    var address = currentAddressPool[selection];
                    var params = {address:address.formatted_address, latitude:address.geometry.location.lat(),
                        longitude:address.geometry.location.lng(), since:sinceInput.val()};
                    if (!presentCheckbox.is(":checked"))
                        params.until = untilInput.val();
                    $.ajax("/api/guest/" + App.getUsername() + "/address/" + typeNames[addressTypeSelect[0].selectedIndex],{
                        type: "POST",
                        data:  params,
                        success: function(data, textStatus, jqXHR){
                            if (data.result == "OK")
                                App.closeModal();
                            else
                                this.error();
                        },
                        error: function(){
                            $(".modal-body").append('<div class="alert alert-error"><button class="close" data-dismiss="alert">×</button><strong>Error!</strong> Failed to add address!</div>')
                        }
                    });
                });

                $("#modal").on("hidden",show);

            });
        });
    }


    function show(){
        if (geocoder == null)
            geocoder = new google.maps.Geocoder();
        $.ajax("/api/guest/" + App.getUsername() + "/address/all",{
            success: function(data, textStatus, jqXHR){
                    dataLoaded(data);
                }
        });
    }

    function dataLoaded(data){
        addresses = data;
        buildDialog();
    }


    var Addresses = {};
    Addresses.show = show;
    return Addresses;
});
