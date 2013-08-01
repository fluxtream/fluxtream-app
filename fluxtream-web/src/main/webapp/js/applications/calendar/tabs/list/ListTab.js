define(["core/Tab", "applications/calendar/tabs/photos/PhotoUtils"], function(Tab, PhotoUtils) {

    var listTab = new Tab("calendar", "list", "Candide Kemmler", "icon-list", true);

    var lastTimestamp = null;

    var setTabParam;

    var currentTimeUnit;

    function render(params) {
        currentTimeUnit = params.timeUnit;
        var doneLoading = params.doneLoading;

        params.doneLoading = function(){
            if (params.facetToShow != null){
                var findResults = $("#list ." + params.facetToShow.type + "-" + params.facetToShow.id);
                if (findResults.length > 0){
                    var facetDiv = $(findResults[0]);
                    var offset = facetDiv.offset();
                    $("body").scrollTop(offset.top - 101);
                }

            }
            doneLoading();
        }
        setTabParam = params.setTabParam;
        this.getTemplate("text!applications/calendar/tabs/list/list.html", "list", function() {
            //TODO: implement comment refreshing algorithm so the entire list tab doesn't have to be refreshed every time
           /* if (lastTimestamp == params.digest.generationTimestamp && !params.forceReload){        //disabled for now to force refreshing of comments
                params.doneLoading();
                return;
            }
            else   */
                lastTimestamp = params.digest.generationTimestamp;
            setup(params.digest,params.connectorEnabled,0,params.doneLoading);
        });
    }

    var items;
    var itemGroups;
    var list;
    var pagination;
    var maxPerPage = 200;
    var currentPage = 0;
    var photoCarouselHTML;
    var dgst;

    var templates;
    var metadata;

    var rendererCount = 0;

    function setup(digest,connectorEnabled,page,doneLoading){
        App.loadAllMustacheTemplates("applications/calendar/tabs/list/listTemplates.html",function(listTemplates){
            dgst = digest;
            templates = listTemplates;
            list = $("#list");
            pagination = $("#pagination");
            currentPage = page;
            items = [];
            itemGroups = {};
            list.empty();
            metadata = digest.metadata;
            var photoCount = 0;
            there:for (var connectorName in digest.cachedData){
                if (!shouldDisplayInListView(connectorName))
                    continue;
                for (var i = 0; i < digest.cachedData[connectorName].length; i++){
                    var item = {};
                    item.facet = digest.cachedData[connectorName][i];
                    if (typeof(item.facet)=="undefined") {
                        console.log("warning: undefined facet for connector " + connectorName);
                        continue;
                    }
                    if (item.facet.hasPhoto)
                        item.facet.id = photoCount++;
                    item.visible = true;
                    var found = false;
                    for (var j = 0; j < digest.selectedConnectors.length; j++){
                        for (var k = 0; !found && k < digest.selectedConnectors[j].facetTypes.length; k++){
                            found = item.facet.type == digest.selectedConnectors[j].facetTypes[k];
                        }
                        if (found){
                            item.visible = connectorEnabled[digest.selectedConnectors[j].connectorName];
                            break;
                        }

                    }

                    var itemCity = App.getFacetCity(item.facet, metadata);
                    if (itemCity==null)
                        continue;
                    for (var j = 0; j <= items.length; j++){
                        if (j == items.length){
                            items[j] = item;
                            break;
                        }
                        var facetCity = App.getFacetCity(items[j].facet, metadata);
                        if (facetCity==null)
                            continue there;
                        if (items[j].facet.start + facetCity.tzOffset > item.facet.start + itemCity.tzOffset || item.facet.start + itemCity.tzOffset == null){
                            items.splice(j,0,item);
                            break;
                        }

                    }
                    if (itemGroups[item.facet.type] == null)
                        itemGroups[item.facet.type] = [];
                    itemGroups[item.facet.type][itemGroups[item.facet.type].length] = item;
                }
            }

            photoCarouselHTML = PhotoUtils.getCarouselHTML(digest);


            rebuildPagination();
            repopulateList();
            updateNumberOfEvents();
            doneLoading();
        });
    }

    function rebuildPagination(){
        pagination.empty();
        var totalPages = getTotalPages();
        if (totalPages>1){
            pagination.show();
        }
        else {
            pagination.hide();
            return;
        }
        var pageList = $("<ul></ul>");
        var button = $("<li " + (currentPage == 0 ? "class='disabled'" : "") + "><a href='#' pageNumber='prev'>&#171;</a></li>");
        pageList.append(button);
        button.click(paginationClickCallback);
        for (var i = 0; i  < totalPages; i++){
            button = $("<li " + (currentPage == i ? "class='active'" : "") + "><a href='#' pageNumber='" + (i) + "'>" + (i + 1) + "</a></li>");
            pageList.append(button);
            button.click(paginationClickCallback);
        }
        button = $("<li " + (currentPage >= totalPages - 1 ? "class='disabled'" : "") + "><a href='#' pageNumber='next'>&#187;</a></li>");
        pageList.append(button);
        button.click(paginationClickCallback);
        pagination.append(pageList);
    }

    function getTotalPages(){
        var totalCount = 0;
        for (var i = 0; i < items.length; i++)
            if (items[i].visible)
                totalCount++;
        return Math.floor(totalCount / maxPerPage) + (totalCount % maxPerPage == 0 ? 0 : 1);
    }

    function updateNumberOfEvents(){
        var totalCount = 0;
        for (var i = 0; i < items.length; i++)
            if (items[i].visible)
                totalCount++;
        $("#eventCount").empty().append(totalCount + " event" + (totalCount == 1 ? "" : "s"));
    }

    function repopulateList(){
        var dateCounter = 2;
        var currentDate = null;
        var prevDate = null;
        list.empty();
        var visibleCount = 0;
        var currentArray = [];
        var facetCity;
        var currentCity;

        function appendItems(currentArray,list){
            var details = currentArray[0].getDetails(currentArray);
            var content = $(templates.item.render({item:details.outerHTML()}));
            list.append(content);
            details.on("contentchange",function(){
                content.html(details.outerHTML());
                App.apps.calendar.rebindDetailsControls(content);
            });
            details.trigger("contentchange");
        }

        for (var i = 0; i < items.length; i++){
           var item = items[i];
           if (item.visible){
               if (item.facet != null && item.facet.hasPhoto)
                   item.facet.id = i;
               visibleCount++;
               if (visibleCount > currentPage * maxPerPage && visibleCount <= (currentPage + 1) * maxPerPage){
                    facetCity = App.getFacetCity(item.facet, metadata);
                    if (facetCity==null)
                        continue;
                    if (currentArray.length == 0){
                        currentArray = [item.facet];
                        currentDate = facetCity.dateWithTimezone;
                        currentCity = facetCity;
                    }
                    else if (currentArray[0].shouldGroup(item.facet) && facetCity.dateWithTimezone == currentDate)
                        currentArray[currentArray.length] = item.facet;
                    else {
                        if (currentDate != prevDate) {
                            list.append(templates.date.render({date:App.prettyDateFormat(currentDate),city:currentCity.name,timezone:currentCity.shortTimezone,state:"list/date/"+currentDate.split(" ")[0]}));
                            prevDate = currentDate;
                        }
                        appendItems(currentArray,list);
                        currentArray = [item.facet];
                        currentDate = facetCity.dateWithTimezone;
                        currentCity = facetCity;
                    }
               }
           }
        }
        if (currentArray.length != 0){
            if (currentDate != prevDate) {
                facetCity = App.getFacetCity(item.facet, metadata);
                if (facetCity!=null) {
                    list.append(templates.date.render({date:App.prettyDateFormat(currentDate),city:currentCity.name,timezone:facetCity.shortTimezone,state:"list/date/"+currentDate.split(" ")[0]}));
                }
            }
            if (facetCity!=null)
                appendItems(currentArray,list);
        }
        if (list.children().length == 0)
            list.append("Sorry, no data to show.");
        var photos = $(".flx-photo");
        for (var i = 0; i < photos.length; i++){
            $(photos[i]).click(function(event){
                PhotoUtils.showCarouselHTML(photoCarouselHTML,$(event.delegateTarget).attr("photoId"));
            });
        }

    }

    function paginationClickCallback(event){
        var pageNum = $(event.target).attr("pageNumber");
        if (pageNum == "prev"){
            if (currentPage == 0)
                return false;
            currentPage--;
        }
        else if (pageNum == "next"){
            if (currentPage >= getTotalPages() - 1)
                return false;
            currentPage++;
        }
        else{
            if (currentPage == pageNum)
                return false;
            currentPage = Number(pageNum);
        }
        rebuildPagination();
        repopulateList();
        return false;
    }

    function shouldDisplayInListView(facetName){
        return App.getFacetConfig(facetName).list;
    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        for (var i = 0; i < objectTypeNames.length; i++){
            if (itemGroups[objectTypeNames[i]] == null)
                continue;
            for (var j = 0; j < itemGroups[objectTypeNames[i]].length; j++)
                itemGroups[objectTypeNames[i]][j].visible = enabled;
        }
        var numPages = getTotalPages();
        if (numPages < currentPage)
            currentPage = numPages == 0 ? 0 : numPages -1;
        rebuildPagination();
        repopulateList();
        updateNumberOfEvents();
    }

    function connectorDisplayable(connector){
        for (var i = 0; i < connector.facetTypes.length; i++){
            var config = App.getFacetConfig(connector.facetTypes[i]);
            if (config.list)
                return true;
        }
        return false;
    }

    function onScroll(scrollPosition){
        var listTops = $("#list .dateHeadingGroup");
        for (var i = 0, li = listTops.length; i < li; i++){
            var listTop = $(listTops[i]);
            var hr = listTop.find(".priorRuler");
            var floater = listTop.find(".dateLabel");
            var placeholder = listTop.find(".placeholder");
            var beginFloat = hr.offset().top + hr.outerHeight() + parseInt(hr.css("marginBottom"));
            if (beginFloat < 0){
                beginFloat = 0;
            }
            var endFloat = null;
            if (i < li - 1){
                var nextListTop = $(listTops[i+1]);
                var nextHr = nextListTop.find(".priorRuler");
                endFloat = nextHr.offset().top + nextHr.outerHeight() + parseInt(nextHr.css("marginBottom"));
            }
            if (scrollPosition < beginFloat){
                placeholder.addClass("hidden");
                floater.removeClass("floating");
                floater.css("marginTop","0px");
            }
            else{
                placeholder.removeClass("hidden");
                floater.addClass("floating");
                floater.css("top",$("#selectedConnectors").height() + "px");
                if (endFloat != null){
                    var temp = scrollPosition +  floater.outerHeight();
                    var marginAmount = endFloat - temp;
                    if (marginAmount > 0) marginAmount = 0;
                    floater.css("marginTop",marginAmount + "px");
                }

            }
            placeholder.height(floater.height());

        }
    }

    $(window).scroll(function(){
        if ($("#listTab").parent().hasClass("active"))
            onScroll($("body").scrollTop() + $("#selectedConnectors").height());
        else
            onScroll(-100);
    });

    /*$(window).resize(function(){
        $(window).scroll();
    });*/

    listTab.render = render;
    listTab.connectorToggled = connectorToggled;
    listTab.connectorDisplayable = connectorDisplayable;
    return listTab;
	
});
