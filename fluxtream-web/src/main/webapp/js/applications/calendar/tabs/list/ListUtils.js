define(["applications/calendar/tabs/photos/PhotoUtils"],function(PhotoUtils){

    var templates;
    App.loadAllMustacheTemplates("applications/calendar/tabs/list/listTemplates.html",function(listTemplates){
        templates = listTemplates;
    });

    return {
        buildList: function(facets,citiesList){

            function appendItems(currentArray,allDay,normal){
                var details = currentArray[0].getDetails(currentArray);
                var content = $(templates.item.render({item:details.outerHTML()}));
                if (currentArray[0].allDay){
                    allDay.append(content);
                }
                else{
                    normal.append(content);
                }
                details.on("contentchange",function(){
                    content.html(details.outerHTML());
                    App.apps.calendar.rebindDetailsControls(content,facets);
                });
                details.trigger("contentchange");
            }

            App.apps.calendar.processFacets(facets);//ensure we can build details

            var currentArray = [];
            var currentDate;
            var currentCity;
            var prevDate = null;
            var list = $("<div></div>");

            facets.sort(function(a,b){
                if (a.date== b.date) return a.start- b.start;
                return a.date> b.date?1 : -1;
            });

            for (var i = 0, li = facets.length; i < li; i++){
                var facet = facets[i];
                var facetCity = App.getFacetCity(facet, citiesList);
                if (facetCity==null)
                    continue;
                if (currentArray.length == 0){
                    currentArray = [facet];
                    currentDate = facetCity.dateWithTimezone;
                    currentCity = facetCity;
                }
                else if (currentArray[0].shouldGroup(facet) && facetCity.dateWithTimezone == currentDate)
                    currentArray[currentArray.length] = facet;
                else {
                    if (currentDate != prevDate) {
                        list.append(templates.date.render({date:App.prettyDateFormat(currentDate),city:currentCity.name,timezone:currentCity.shortTimezone,state:"list/date/"+currentDate.split(" ")[0]}));
                        prevDate = currentDate;
                        var curContainer = $(templates.itemContainer.render({}));
                        list.append(curContainer);
                    }
                    appendItems(currentArray,curContainer.find(".allDayContainer"),curContainer.find(".normalContainer"));
                    currentArray = [facet];
                    currentDate = facetCity.dateWithTimezone;
                    currentCity = facetCity;
                }
            }
            if (currentArray.length != 0){
                if (currentDate != prevDate) {
                    list.append(templates.date.render({date:App.prettyDateFormat(currentDate),city:currentCity.name,timezone:facetCity.shortTimezone,state:"list/date/"+currentDate.split(" ")[0]}));
                    var curContainer = $(templates.itemContainer.render({}));
                    list.append(curContainer);
                }
                appendItems(currentArray,curContainer.find(".allDayContainer"),curContainer.find(".normalContainer"));
            }
            if (list.children().length == 0)
                list.append("Sorry, no data to show.");
            var photos = list.find(".flx-photo");
            for (var i = 0; i < photos.length; i++){
                $(photos[i]).click(function(event){
                    PhotoUtils.showCarouselHTML(PhotoUtils.getCarouselHTML(App.apps.calendar.digest),$(event.delegateTarget).attr("photoId"));
                });
            }
            return list.children();
        }
    }
});