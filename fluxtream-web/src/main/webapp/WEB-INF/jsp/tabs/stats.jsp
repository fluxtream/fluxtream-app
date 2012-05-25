<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
        %><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
        %><%@ page isELIgnored="false"
        %><%@ page import="com.fluxtream.*"
        %><%@ page import="com.fluxtream.mvc.tabs.controllers.DashboardWidgetsHelper"
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


    <div class="row"> <!-- Remove when added to the clock tab -->
        <div class="flx-clockToolTipContainer">
			<span class="flx-toolTipTail">
			</span>
            <div class="flx-toolTipBody">
                <div class="flx-toolTipData">
                    <h4 class="flx-dataType">Weight</h4>
                    <span class="flx-deviceIcon"></span>
                    <span class="flx-tTime">9:43</span>
                    <span class="flx-data">71.05 kg</span>
                    <span class="flx-miniGraph"></span>
                    <ul class="flx-toolTipLinks">
                        <li><a href="#"></i>List <i class="icon-list"></a></li>
                        <li><a href="#"></i>Timeline <i class="icon-film"></a></li>
                        <li><a href="#"></i>Bodytrack <i class="icon-map-marker"></a></li>
                    </ul>
                </div>
                <div class="flx-toolTipContext">
                    <h5>
                </div>
            </div>
        </div>
    </div>


    <!--     <div class="flx-spacer"></div> -->
</div>
