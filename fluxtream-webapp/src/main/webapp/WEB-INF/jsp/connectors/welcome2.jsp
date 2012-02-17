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
		To make it possible however, we need to extract information from the tools and devices that you
		use every day. We have plenty of connectors, and you will get to discover them all
		very soon.
	</p><br/>

	<a class="btn" href="javascript:initSequenceExit()">Cancel</a>&nbsp;&nbsp;
	<a class="btn" href="javascript:loadModal('/connectors/main')">&larr; back</a>
	<a class="btn primary" href="javascript:loadModal('/connectors/welcome3')">Gotcha, continue! &rarr;</a>
</div>
