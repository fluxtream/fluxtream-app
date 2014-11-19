<div class="well">
    <h4>Facet Models:</h4>
    <ul class="dropdown-menu" aria-labelledby="dLabel" role="menu" id="connectorsmenu"></ul>
    <ul class="dropdown-menu" aria-labelledby="dLabel" role="menu" id="facetVosMenu"></ul>
    <script>
        $(document).ready(function(){
            $.ajax({
                url:"/api/v1/datamodel/facetVos",
                success : function(voData) {
                    var dropdownMenu = $("<ul class=\"dropdown-menu\" role=\"menu\" aria-labelledby=\"connectorsMenuButton\"></ul>");
                    for (var i=0; i<voData.length; i++) {
                        var dropdownSubmenuWrapper = $("<li role=\"menuitem\"><a href=\"javascript:void(0);\" class=\"connectorName-menuitem\" data-connectorName=\"" + voData[i]["connectorName"] + "\">" + voData[i]["connectorName"] + "</a></li>");
                        dropdownMenu.append(dropdownSubmenuWrapper);
                    }
                    var dropdown = $("<div class=\"dropdown\" style=\"display:inline\"></div>");
                    dropdown.append("<button id=\"connectorsMenuButton\" class=\"btn btn-default dropdown-toggle\" type=\"button\" data-toggle=\"dropdown\">Please select a connector <span class=\"caret\"></span></button>");
                    dropdown.append(dropdownMenu);
                    $('.dropdown-toggle').dropdown()
                    $("#connectorsmenu").replaceWith(dropdown);

                    function getShortVoName(clazzName) {
                        var a = clazzName.split(".");
                        var shortVoName = a[a.length-1];
                        return shortVoName;
                    }

                    function createFacetVosMenu(facetVos) {
                        var dropdownMenu = $("<ul class=\"dropdown-menu\" role=\"menu\" aria-labelledby=\"facetVosMenuButton\"></ul>");
                        for (var i=0; i<facetVos.length; i++) {
                            var shortVoName = getShortVoName(facetVos[i]);
                            var dropdownSubmenuWrapper = $("<li role=\"menuitem\"><a href=\"javascript:void(0);\" class=\"vo-menuitem\" data-voName=\"" + facetVos[i] + "\">" + shortVoName + "</a></li>");
                            dropdownMenu.append(dropdownSubmenuWrapper);
                        }
                        var dropdown = $("<div class=\"dropdown\" id=\"facetVosMenu\" style=\"display:inline\"></div>");
                        dropdown.append("<button id=\"facetVosMenuButton\" class=\"btn btn-default dropdown-toggle\" type=\"button\" data-toggle=\"dropdown\">Please select a Model <span class=\"caret\"></span></button>");
                        dropdown.append(dropdownMenu);
                        $('.dropdown-toggle').dropdown()
                        $("#facetVosMenu").replaceWith(dropdown);
                        $(".vo-menuitem").unbind().click(function(event){
                            var clazzName = $(event.target).attr("data-voName");
                            console.log("clicked on " + clazzName);
                            var shortVoName = getShortVoName(clazzName);
                            $("#facetVosMenuButton").html(shortVoName + " <span class=\"caret\"></span>");

                            $.ajax({url:"/api/v1/datamodel/get?class="+clazzName,
                                        success: function(models) {
                                            var sModel;
                                            var referencedModels = [];
                                            for (var i=0; i<models.length; i++) {
                                                if (models[i]["qualifiedType"]===clazzName) {
                                                    sModel = new SwaggerModel(models[i].name, models[i]);
                                                    continue;
                                                }
                                                referencedModels.push(models[i]);
                                            }
                                            sModel.setReferencedModels(referencedModels);
                                            var mockSignature = sModel.getMockSignature();
                                            var mockSignatures = [mockSignature];
                                            for (var i=0; i<referencedModels.length; i++) {
                                                var rModel = new SwaggerModel(referencedModels[i].name, referencedModels[i]);
                                                mockSignatures.push(rModel.getMockSignature());
                                            }
                                            var signatures = mockSignatures.join("<br>");
                                            var sample = sModel.createJSONSample();
                                            $(".description.facetvos").empty().html(signatures);
                                            $(".snippet.facetvos").empty().html("<code><pre>"+JSON.stringify(sample, null, 3)+"</pre></code>");
                                            console.log(sample);
                                        }}
                            );

                        });
                    }

                    $(".connectorName-menuitem").click(function(event) {
                        var selectedConnectorName = $(event.target).attr("data-connectorName");
                        $("#connectorsMenuButton").html(selectedConnectorName + " <span class=\"caret\"></span>")
                        for (var i=0; i<voData.length; i++) {
                            if (selectedConnectorName===voData[i]["connectorName"]) {
                                createFacetVosMenu(voData[i]["facetVos"]);
                                $(".description.facetvos").empty().append("Please choose a connector/facet model above");
                                $(".snippet.facetvos").empty().append("Please choose a connector/facet model above");
                            }
                        }
                    });
                }
            });
        });
    </script>
    <span class="model-signature">
        <div>
            <div>
                <div>
                    <div class="signature-container">

                        <ul class="nav nav-pills" role="tablist" style="margin-top:1em">
                            <li class="active"><a href="#facetvo-description" role="tab" data-toggle="pill">Description</a></li>
                            <li><a href="#facetvo-snippet" role="tab" data-toggle="pill">Snippet</a></li>
                        </ul>

                        <!-- Tab panes -->
                        <div class="tab-content">
                            <div class="tab-pane active description facetvos" id="facetvo-description" style="margin:1em 0 0 0">
                                Please choose a connector/facet model above
                            </div>
                            <div class="tab-pane snippet facetvos" id="facetvo-snippet" style="margin-top: 1em">
                                Please choose a connector/facet model above
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </span>
</div>
