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

<div id="dashboardWidget" class="row-fluid leftAligned">

	<% for (int i=0; i<userWidgets.size(); i++) {
		String widgetsDir = timeUnit + "lyDashboardWidgets/";
		if (timeUnit.equalsIgnoreCase("day"))
			widgetsDir = "dailyDashboardWidgets/";%>
		<jsp:include page="<%=widgetsDir+userWidgets.get(i).name+\".jsp\"%>" />
	<% } %>

    <div class="flx-spacer"></div>
</div>
