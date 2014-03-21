<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
        %><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
        %><%@ page isELIgnored="false"
        %><%@ page import="org.fluxtream.*"
        %><%@ page import="org.fluxtream.mvc.tabs.controllers.DashboardWidgetsHelper"
        %><%@ page import="java.util.*"
        %>

<%
    List<DashboardWidgetsHelper.DashboardWidget> userWidgets = (List<DashboardWidgetsHelper.DashboardWidget>) request.getAttribute("userWidgets");
    String timeUnit = (String) request.getAttribute("timeUnit");
%>

<div id="dashboardWidget" class="row-fluid"> <!-- to make it fixed add class leftAligned -->

    <!--
	<% for (int i=0; i<userWidgets.size(); i++) {
		String widgetsDir = timeUnit + "lyDashboardWidgets/";
		if (timeUnit.equalsIgnoreCase("day"))
			widgetsDir = "dailyDashboardWidgets/";%>
		<jsp:include page="<%=widgetsDir+userWidgets.get(i).name+\".jsp\"%>" />
	<% } %>
-->

    <div class="flx-widget span4">
        <header>
            <div class="flx-widget-controls">
                <a href="#" class="flx-settings"><i class="icon-cog"></i></a>
                <a href="#" class="flx-remove"><i class="icon-remove"></i></a>
            </div>
            <h3 class="flx-title">Steps</h3>
        </header>
        <div class="flx-body">
            <div class="flx-stepsNmbr"><img src="/${release}/css/dashboardImgDummies/w1.png" alt="w1" width="" height="" /></div>
        </div>
        <footer>
        </footer>
    </div>
    <div class="flx-widget span4">
        <header>
            <div class="flx-widget-controls">
                <a href="#" class="flx-settings"><i class="icon-cog"></i></a>
                <a href="#" class="flx-remove"><i class="icon-remove"></i></a>
            </div>
            <h3 class="flx-title">Sleep</h3>
        </header>
        <div class="flx-body">
            <div class="flx-sleepStats"><img src="/${release}/css/dashboardImgDummies/w2.png" alt="w2" width="" height="" /></div>
        </div>
        <footer>
        </footer>
    </div>
    <div class="flx-widget span4">
        <header>
            <div class="flx-widget-controls">
                <a href="#" class="flx-settings"><i class="icon-cog"></i></a>
                <a href="#" class="flx-remove"><i class="icon-remove"></i></a>
            </div>
            <h3 class="flx-title">Food</h3>
        </header>
        <div class="flx-body">
            <div class="flx-foodthumbnails"><img src="/${release}/css/dashboardImgDummies/w3.png" alt="w3" width="" height="" /></div>
        </div>
        <footer>
        </footer>
    </div>

</div>

<div class="row-fluid">

    <div class="flx-widget span4">
        <header>
            <div class="flx-widget-controls">
                <a href="#" class="flx-settings"><i class="icon-cog"></i></a>
                <a href="#" class="flx-remove"><i class="icon-remove"></i></a>
            </div>
            <h3 class="flx-title">Activity</h3>
        </header>
        <div class="flx-body">
            <div class="flx-activity"><img src="/${release}/css/dashboardImgDummies/w4.png" alt="w4" width="" height="" /></div>
        </div>
        <footer>
        </footer>
    </div>
    <div class="flx-widget span4">
        <header>
            <div class="flx-widget-controls">
                <a href="#" class="flx-settings"><i class="icon-cog"></i></a>
                <a href="#" class="flx-remove"><i class="icon-remove"></i></a>
            </div>
            <h3 class="flx-title">Finance</h3>
        </header>
        <div class="flx-body">
            <div class="flx-finance"><img src="/${release}/css/dashboardImgDummies/w5.png" alt="w5" width="" height="" /></div>
        </div>
        <footer>
        </footer>
    </div>
    <div class="flx-widget span4">
        <header>
            <div class="flx-widget-controls">
                <a href="#" class="flx-settings"><i class="icon-cog"></i></a>
                <a href="#" class="flx-remove"><i class="icon-remove"></i></a>
            </div>
            <h3 class="flx-title">mffboston09</h3>
        </header>
        <div class="flx-body">
            <div class="flx-stepsNmbr"><img src="/${release}/css/dashboardImgDummies/w6.png" alt="w6" width="" height="" /></div>
        </div>
        <footer>
        </footer>
    </div>




    <!--     <div class="flx-spacer"></div> -->
</div>
