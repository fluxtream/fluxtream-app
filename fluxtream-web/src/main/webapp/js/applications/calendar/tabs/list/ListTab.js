define(["applications/calendar/tabs/Tab"], function(Tab) {

    var listTab = new Tab("list", "Candide Kemmler", "icon-list", true);

    function render(digest, timeUnit) {
        this.getTemplate("text!applications/calendar/tabs/list/list.html", "list", function(){setup(digest);});
    }

    var items;
    var itemGroups;
    var list;
    var pagination;
    var clicked = {};
    var maxPerPage = 10;
    var currentPage = 0;
    var initializing;

    function setup(digest){
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
                var item = $("<div style=\"min-height:80px;\">" + digest.cachedData[connectorName][i].getDetails() + "</div>");
                item.facet = digest.cachedData[connectorName][i];
                item.visible = true;
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
        var checkedContainer = $("#selectedConnectors");
        checkedContainer.empty();
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            var displayable = false;
            for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length && !displayable; j++){
                displayable = shouldDisplayInListView(digest.selectedConnectors[i].facetTypes[j])
            }
            if (displayable){
                var enabled = false;
                for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length && !enabled; j++){
                    enabled =  itemGroups[digest.selectedConnectors[i].facetTypes[j]] != null;
                }
                enabled = enabled ? "enabled" : "disabled";
                var button = $('<button class="btnList btn btnListChecked ' + enabled + '">' + digest.selectedConnectors[i].prettyName + '</button>');
                button.click({button:button,objectTypeNames:digest.selectedConnectors[i].facetTypes,connectorName:digest.selectedConnectors[i].connectorName}, function(event){
                    connectorClicked(event.data.button,event.data.objectTypeNames,event.data.connectorName);
                });
                if (clicked[digest.selectedConnectors[i].connectorName] == null)
                    clicked[digest.selectedConnectors[i].connectorName] = false;
                if (clicked[digest.selectedConnectors[i].connectorName])
                    button.click();
                checkedContainer.append(button);
                checkedContainer.append("&nbsp;");
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

    function connectorClicked(button,objectTypeNames,connectorName){
        if (!initializing)
            clicked[connectorName] = !clicked[connectorName];
        if (clicked[connectorName]){
            button.removeClass("btnListChecked");
            button.addClass("btn-inverse");
        }
        else{
            button.addClass("btnListChecked");
            button.removeClass("btn-inverse");
        }
        if (button.is(".disabled"))
            return;
        for (var i = 0; i < objectTypeNames.length; i++){
            if (itemGroups[objectTypeNames[i]] == null)
                continue;
            for (var j = 0; j < itemGroups[objectTypeNames[i]].length; j++)
                itemGroups[objectTypeNames[i]][j].visible = !clicked[connectorName];
        }
        if (!initializing){
            var totalPages = getTotalPages();
            if (currentPage >= totalPages)
                currentPage = totalPages - 1;
            if (currentPage < 0)
                currentPage = 0;
            rebuildPagination();
            repopulateList();
            updateNumberOfEvents();
        }
    }



    listTab.render = render;
    return listTab;
	
});
