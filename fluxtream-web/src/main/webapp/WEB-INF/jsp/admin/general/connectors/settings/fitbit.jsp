<%@ page isELIgnored="false"%>
<%@ page import="com.fluxtream.mvc.admin.controllers.*"%>
<%@ page import="com.fluxtream.connectors.*"%>
<%@ page import="com.fluxtream.connectors.fitbit.*"%>
<%@ page import="java.util.*"%>

<%
	AdminHelper adminHelper = (AdminHelper) request.getAttribute("helper");
	FitbitAdminHelper fitbitHelper = (FitbitAdminHelper) adminHelper.getHelper("fitbit");
%>

