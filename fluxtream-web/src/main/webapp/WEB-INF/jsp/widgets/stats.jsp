<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.mvc.widgets.controllers.StatsHelper"
%><%@ page import="java.util.*"
%>

<%
	List<StatsHelper.Widget> userWidgets = (List<StatsHelper.Widget>) request.getAttribute("userWidgets");
	String timeUnit = (String) request.getAttribute("timeUnit");
%>

<div id="statsWidget" class="row-fluid">

	<% for (int i=0; i<userWidgets.size(); i++) { %>
		<jsp:include page="<%=timeUnit+\"StatsWidgets/\"+userWidgets.get(i).name+\".jsp\"%>" />
	<% } %>

</div>
