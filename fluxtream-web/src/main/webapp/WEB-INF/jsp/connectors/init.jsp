<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="org.fluxtream.core.*"%><%
%><%@ page import="org.fluxtream.core.connectors.*"%><%
%><%@ page import="org.fluxtream.core.domain.*"%><%
%><%@ page import="java.util.*"%><%
	ResourceBundle res =
		ResourceBundle.getBundle("messages/connectors");
	List<ApiKey> userConnectors = (List<ApiKey>) request.getAttribute("userConnectors");
	boolean hasLatitude = (Boolean) request.getAttribute("hasLatitude");
	boolean hasFitbit = (Boolean) request.getAttribute("hasFitbit");
%>

<div>
	<div class="addConnectorsMain<%if(userConnectors.size()==0) out.print(" init");%>">
	<h2>Your First Connector</h2>
	<h3>You're almost done</h3>
	
	<div class="halfwidth">
		<article>
			<h4>Google Latitude</h4>
			<a href="/google_latitude/token"><img src="/${release}/images/connector-focus-google_latitude1.jpg" alt="" width=283 height=190></a>
			<p><%=res.getString("google_latitude") %></p>
			<p>Don't use it yet? <a href="https://www.google.com/latitude/" target="_blank">Click here</a></p>
		</article>
	</div>
		
	</div>
</div>
