<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="java.util.*"
%>

<%
	List<String> userWidgets = (List<String>) request.getAttribute("userWidgets");
%>

<div id="statsWidget" class="span12">

	<% for (int i=0; i<userWidgets.size(); i++) { %>
		<jsp:include page="<%=\"day/\"+userWidgets.get(i)+\".jsp\"%>" />
	<% } %>

</div>
