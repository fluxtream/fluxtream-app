define([],function(){

    function TabInterface(tabPaths){
        this.tabs = {};
        this.nav = $("<ul class='nav nav-tabs'></ul>");
        this.nav.listeners = [];
        loadTab(this,tabPaths,0);
        var nav = this.nav;
        var tabs = this.tabs;
        this.nav.addClickListener = function(listener){
            nav.listeners.push(listener);
        }
        this.nav.click(function(event){
            event.preventDefault();
            event.stopImmediatePropagation();
            var target = $(event.target);
            while (target != null){
                if (target[0].tagName == "LI")
                    break;
                else if (target[0] == nav[0])
                    target = null;
                else
                    target = target.parent();
            }
            var targetName = null;
            for (var tabname in tabs)
                if (tabs[tabname].nav[0] == target[0]){
                    targetName = tabname;
                    break;
                }
            if (targetName != null){
                for (var i = 0; i < nav.listeners.length; i++)
                    nav.listeners[i](targetName);
            }
        });
    }

    TabInterface.prototype.getRenderParams = function(){
        return {};
    }

    TabInterface.prototype.setRenderParamsFunction = function(fn){
        this.getRenderParams = fn;
    }

    TabInterface.prototype.setTabVisibility = function(tabnames,visible){
        if (typeof tabnames == "string")
            tabnames = [tabnames];
        for (var i = 0; i < tabnames.length; i++)
            setFieldValue(this,tabnames[i],"visible",visible);
    }

    TabInterface.prototype.setActiveTab = function(tabname){
        setFieldValue(this,tabname,"active",true);
    }

    TabInterface.prototype.getActiveTab = function(){
        for (var tabname in this.tabs)
            if (this.tabs[tabname].active)
                return this.tabs[tabname].tab;
        return null;
    }

    TabInterface.prototype.getNav = function(){
        return this.nav;
    }

    function loadTab(ti,tabPaths,i){ //loads all tabs in order they are listed consistently
        require([tabPaths[i]],function(tab){
            setTabObject(ti,tab);
            if (++i < tabPaths.length)
                loadTab(ti,tabPaths,i);
        });
    }


    function setTabObject(ti, tab){
        setFieldValue(ti,tab.name,"nav",$("<li style='cursor:pointer'><a class='" + tab.appname + "-" + tab.name + "-" + "tab' data-toggle='tab'><i class= '" + tab.icon + "'></i> " + capitalizeFirstLetter(tab.name) + "</a></li>"));
        setFieldValue(ti,tab.name,"tab",tab);
    }

    function setFieldValue(ti,tabname,key,value){
        if (ti.tabs[tabname] == null)
            ti.tabs[tabname] = {};
        ti.tabs[tabname][key] = value;
        if (key == "nav"){
            ti.nav.append(value);
        }
        updateNav(ti,tabname);
        switch (key){
            case "active":
                if (ti.tabs[tabname].nav != null){
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