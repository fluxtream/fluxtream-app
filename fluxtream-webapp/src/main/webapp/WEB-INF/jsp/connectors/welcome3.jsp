<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"%><%
%><%@ page import="java.util.*"%><%
ResourceBundle res =
	ResourceBundle.getBundle("messages/connectors");
%>

<div style="font-size:80%">

	
	<br/>
	<p>
		More important than any other connector, the Google Latitude connector lets us know
		your location. This is our only required connector. Fluxtream would be pointless
		without it.
	</p><br/>

	<a class="btn" href="javascript:initSequenceExit()">Cancel</a>&nbsp;&nbsp;
	<a class="btn" href="javascript:loadModal('/connectors/welcome2')">&larr; back</a>
	<a class="btn primary" href="javascript:loadModal('/connectors/init')">No Problems, let's do that! &rarr;</a>
</div>
