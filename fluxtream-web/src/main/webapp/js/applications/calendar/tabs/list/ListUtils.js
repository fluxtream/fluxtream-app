define([],function(){

    var templates;

    App.loadAllMustacheTemplates("applications/calendar/tabs/list/listTemplates.html",function(listTemplates){
        templates = listTemplates;
    });

    var ListUtils = {};

    function buildList(facets,citiesList) {

        function appendItems(currentArray,allDay,normal){
            var details = currentArray[0].getDetails(currentArray);
            var content = $(templates.item.render({item:details.outerHTML()}));
            // let's not append empty events
            if (!_.isUndefined(currentArray[0]["isEmpty"])&&currentArray[0]["isEmpty"]){
                return false;
            }
            if (!_.isUndefined(currentArray[0]["allDay"])&&currentArray[0]["allDay"]){
                allDay.append(content);
                allDay.parent().find(".timePeriod").show();
            }
            else {
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
        var minPhotoTime = 1e300;
        var maxPhotoTime = -1e300;

        function handlePhotoClick(event){
            var dTarget = $(event.delegateTarget);
            App.apps.calendar.showPhotoDialog(dTarget.attr("data-deviceName"), dTarget.attr("data-channelName"),
                dTarget.attr("data-id"), dTarget.attr("data-timestamp"),{minTime:minPhotoTime,maxTime:maxPhotoTime});
        }

        for (var i = 0; i < photos.length; i++){
            minPhotoTime = Math.min(minPhotoTime,parseInt($(photos[i]).attr("data-timestamp")));
            maxPhotoTime = Math.max(maxPhotoTime,parseInt($(photos[i]).attr("data-timestamp")));
            $(photos[i]).click(handlePhotoClick);
        }
        return list.children();
    }

    ListUtils.buildList = buildList;
    return ListUtils;
});