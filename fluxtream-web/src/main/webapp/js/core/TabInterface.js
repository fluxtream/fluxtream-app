define([],function(){

    function TabInterface(tabPaths){
        this.tabs = {};
        this.nav = $("<ul class='nav nav-tabs'></ul>");
        this.nav.listeners = [];
        loadTabs(this, tabPaths);
        this.nav.addClickListener = function(listener){
            this.nav.listeners.push(listener);
        }.bind(this);
        this.nav.click(function(event){
            event.preventDefault();
            event.stopImmediatePropagation();
            var targetName = $(event.target).closest('li').attr('name');
            if (_.isUndefined(targetName) || !_.has(this.tabs, targetName)) {
                return;
            }
            $.each(this.nav.listeners, function(i, listener) {
                listener(targetName);
            });
        }.bind(this));
    }

    TabInterface.prototype.getRenderParams = function(){
        return {};
    };

    TabInterface.prototype.setRenderParamsFunction = function(fn){
        this.getRenderParams = fn;
    };

    TabInterface.prototype.setTabVisibility = function(tabnames,visible){
        if (typeof tabnames == "string")
            tabnames = [tabnames];
        if (typeof tabnames=="undefined")
            return;
        for (var i = 0; i < tabnames.length; i++)
            setFieldValue(this,tabnames[i],"visible",visible);
    };

    TabInterface.prototype.setActiveTab = function(tabname){
        setFieldValue(this,tabname,"active",true);
    };

    function getActiveTab(ti){
        for (var tabname in ti.tabs)
            if (ti.tabs[tabname].active)
                return ti.tabs[tabname];
        return null
    };

    TabInterface.prototype.getActiveTab = function(){
        var tab = getActiveTab(this);
        return tab == null ? null : tab.tab;
    };

    TabInterface.prototype.getNav = function(){
        return this.nav;
    };

    function loadTabs(ti,tabPaths){ //loads all tabs in order they are listed consistently
        require(tabPaths, function(/* tabs */) {
            for (var i = 0; i < arguments.length; i++) {
                var tab = arguments[i];
                setFieldValue(ti,tab.name,"nav",$("<li name='" + tab.name + "' style='cursor:pointer'><a class='" + tab.appname + "-" + tab.name + "-" + "tab' data-toggle='tab'><i class= '" + tab.icon + "'></i> " + tab.name.upperCaseFirst() + "</a></li>"));
                setFieldValue(ti,tab.name,"tab",tab);
            }
        });
    }

    function setFieldValue(ti,tabname,key,value){
        var oldTab = null;
        if (key == "active")
            oldTab = getActiveTab(ti);
        if (ti.tabs[tabname] == null)
            ti.tabs[tabname] = {};
        ti.tabs[tabname][key] = value;
        if (key == "nav"){
            ti.nav.append(value);
        }
        updateNav(ti,tabname);
        switch (key){
            case "active":
                if (ti.tabs[tabname].nav != null && value){
                    if (oldTab != null && oldTab != ti.tabs[tabname])
                        setFieldValue(ti,oldTab.tab.name,"active",false);
                    ti.tabs[tabname].nav.children().tab('show');
                    ti.tabs[tabname].tab.render(ti.getRenderParams());
                }
                break;
            case "nav":
                if (ti.tabs[tabname].active)
                    ti.tabs[tabname].nav.children().tab('show');
                break;
            case "tab":
                if (ti.tabs[tabname].active)
                    ti.tabs[tabname].tab.render(ti.getRenderParams());
                break;
        }
    }

    function capitalizeFirstLetter(string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

    function updateNav(ti,tabname){
        if (ti.tabs[tabname].nav != null){
            ti.tabs[tabname].nav.css("display",ti.tabs[tabname].visible ? "" : "none");
        }
    }



    return TabInterface;
});