define(["applications/calendar/tabs/Tab"], function(Tab) {

    var listTab = new Tab("list", "Candide Kemmler", "icon-list", true);

    function render(digest, timeUnit, calendarState, connectorEnabled) {
        this.getTemplate("text!applications/calendar/tabs/list/list.html", "list", function(){setup(digest,connectorEnabled);});
    }

    var items;
    var itemGroups;
    var list;
    var pagination;
    var maxPerPage = 10;
    var currentPage = 0;
    var initializing;

    function setup(digest,connectorEnabled){
        initializing = true;
        list = $("#list");
        pagination = $("#pagination");
        currentPage = 0;
        items = [];
        itemGroups = {};
        list.empty();
        for (var connectorName in digest.cachedData){
            if (!shouldDisplayInListView(connectorName))
                continue;
            for (var i = 0; i < digest.cachedData[connectorName].length; i++){
                var item = $("<div style=\"overflow: auto;\">" + digest.cachedData[connectorName][i].getDetails() + "</div>");
                item.facet = digest.cachedData[connectorName][i];
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
                for (var j = 0; j <= items.length; j++){
                    if (j == items.length){
                        items[j] = item;
                        break;
                    }
                    if (items[j].facet.start > item.facet.start){
                        items.splice(j,0,item);
                        break;
                    }

                }
                if (itemGroups[item.facet.type] == null)
                    itemGroups[item.facet.type] = [];
                itemGroups[item.facet.type][itemGroups[item.facet.type].length] = item;
            }
        }


        rebuildPagination();
        repopulateList();
        updateNumberOfEvents();
        initializing = false;
    }

    function rebuildPagination(){
        pagination.empty();
        var totalPages = getTotalPages();
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
        list.empty();
        var visibleCount = 0;
        for (var i = 0; i < items.length; i++){
           var item = items[i];
           if (item.visible){
               visibleCount++;
               if (visibleCount > currentPage * maxPerPage && visibleCount <= (currentPage + 1) * maxPerPage)
                    list.append(item);
           }
        }
        if (list.children().length == 0)
            list.append("Sorry, no data to show.");
    }

    function paginationClickCallback(event){
        var pageNum = $(event.target).attr("pageNumber");
        if (pageNum == "prev"){
            if (currentPage == 0)
                return;
            currentPage--;
        }
        else if (pageNum == "next"){
            if (currentPage >= getTotalPages() - 1)
                return;
            currentPage++;
        }
        else{
            if (currentPage == pageNum)
                return;
            currentPage = pageNum;
        }
        rebuildPagination();
        repopulateList();
        return false;
    }

    function shouldDisplayInListView(connectorId){
        switch (connectorId){
            case "google_latitude":
            case "bodytrack":
                return false;
            default:
                return true;
        }
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



    listTab.render = render;
    listTab.connectorToggled = connectorToggled;
    return listTab;
	
});
