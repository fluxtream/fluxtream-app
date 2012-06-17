define(function() {

    function show(){
        $.ajax("/api/widgets",{
            success: function(data, textStatus, jqXHR){
                dataLoaded({widgets: data});
            }
        });
    }

    function dataLoaded(data){
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/addWidgetTemplate.html","mainDialog",function(template){
            var html = template.render(data);
            App.makeModal(html);
            bindDialog();
        });
    }

    function bindDialog(){
    }

    var AddWidget = {};
    AddWidget.show = show;
    return AddWidget;
});