define(["core/Tab", "applications/calendar/tabs/photos/PhotoUtils"], function(Tab, PhotoUtils) {

    var listTab = new Tab("calendar", "list", "Candide Kemmler", "icon-list", true);

    function render(params) {
        this.getTemplate("text!applications/calendar/tabs/list/list.html", "list", function() {
            setup(params.digest,params.connectorEnabled,params.tabParam == null ? 0 : parseInt(params.tabParam) - 1);
        });
    }

    var items;
    var itemGroups;
    var list;
    var pagination;
    var maxPerPage = 250;
    var currentPage = 0;
    var photoCarouselHTML;
    var timeZoneOffset;

    var templates;

    var rendererCount = 0;

    function setup(digest,connectorEnabled,page){
        App.loadAllMustacheTemplates("applications/calendar/tabs/list/listTemplates.html",function(listTemplates){
            templates = listTemplates;
            timeZoneOffset = digest.timeZoneOffset;
            list = $("#list");
            pagination = $("#pagination");
            currentPage = page;
            items = [];
            itemGroups = {};
            list.empty();
            for (var connectorName in digest.cachedData){
                if (!shouldDisplayInListView(connectorName))
                    continue;
                for (var i = 0; i < digest.cachedData[connectorName].length; i++){
                    var item = {};
                    item.facet = digest.cachedData[connectorName][i];
                    item.visible = true;
                    if (connectorName == "picasa-photo")
                        item.id = i;
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
                    for (var j = 0; j <= items.length; j++){
                        if (j == items.length){
                            items[j] = item;
                            break;
                        }
                        if (items[j].facet.start + timeZoneOffset > item.facet.start + timeZoneOffset || item.facet.start + timeZoneOffset == null){
                            items.splice(j,0,item);
                            break;
                        }

                    }
                    if (itemGroups[item.facet.type] == null)
                        itemGroups[item.facet.type] = [];
                    itemGroups[item.facet.type][itemGroups[item.facet.type].length] = item;
                }
            }

            photoCarouselHTML = PhotoUtils.getCarouselHTML(digest,["picasa-photo","flickr-photo","mymee-observation"]);


            rebuildPagination();
            repopulateList();
            updateNumberOfEvents();
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

    /*//new design for full loading
    function repopulateList(){
        list.empty();
        populateList(++rendererCount,0);

    }

    function populateList(expectedRendererCount, index){
        if (rendererCount != expectedRendererCount)
            return;
        var visibleCount = 0;
        var currentArray = [];
        var i = index;
        for (; i < items.length && visibleCount < maxPerPage; i++){
            var item = items[i];
            if (item.visible){
                if (currentArray.length == 0)
                    currentArray = [item.facet];
                else if (currentArray[0].shouldGroup(item.facet))
                    currentArray[currentArray.length] = item.facet;
                else{
                    list.append("<div class=\"flx-listItem\">" + currentArray[0].getDetails(currentArray) + "</div>");
                    currentArray = [];
                    i--;
                    visibleCount++;
                }
            }
        }
        var newIndex = i;
        var photos = $(".flx-box.picasa-photo img");
        for (var i = 0; i < photos.length; i++){
            $(photos[i]).unbind("click").click({i:i}, function(event){
                App.makeModal(photoCarouselHTML);
                App.carousel(event.data.i);
            });
        }
        if (currentArray.length != 0)
            list.append("<div class=\"flx-listItem\">" + currentArray[0].getDetails(currentArray) + "</div>");
        if (i == items.length){
            if (list.children().length == 0)
                list.append("Sorry, no data to show.");
        }
        else{
            $.doTimeout(1000,function(){
                populateList(expectedRendererCount,newIndex);
            });
        }
    }*/

    function repopulateList(){
        var currentDate = null;
        var prevDate = null;
        list.empty();
        var visibleCount = 0;
        var currentArray = [];
        for (var i = 0; i < items.length; i++){
           var item = items[i];
           if (item.visible){
               visibleCount++;
               if (visibleCount >= currentPage * maxPerPage && visibleCount <= (currentPage + 1) * maxPerPage){
                    var facetDate = App.formatDate(item.facet.start  + timeZoneOffset,false,true);
                    if (currentArray.length == 0){
                        currentArray = [item.facet];
                        currentDate = facetDate;
                    }
                    else if (currentArray[0].shouldGroup(item.facet) && facetDate == currentDate)
                        currentArray[currentArray.length] = item.facet;
                    else{
                        if (currentDate != prevDate){
                            list.append(templates.date.render({date:currentDate}));
                            prevDate = currentDate;
                        }
                        list.append(templates.item.render({item:currentArray[0].getDetails(currentArray)}));
                        currentArray = [item.facet];
                        currentDate = facetDate;
                    }
               }
           }
        }
        if (currentArray.length != 0){
            if (currentDate != prevDate)
                list.append(templates.date.render({date:currentDate}));
            list.append(templates.item.render({item:currentArray[0].getDetails(currentArray)}));
        }
        if (list.children().length == 0)
            list.append("Sorry, no data to show.");
        var photos = $(".flx-box.picasa-photo img, .flx-box.mymee-observation img");
        for (var i = 0; i < photos.length; i++){
            $(photos[i]).click({i:i}, function(event){
                App.makeModal(photoCarouselHTML);
                App.carousel(event.data.i);
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

    listTab.render = render;
    listTab.connectorToggled = connectorToggled;
    listTab.connectorDisplayable = connectorDisplayable;
    return listTab;
	
});
