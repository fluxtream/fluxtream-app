<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"%><%
%><%@ page import="java.util.*"%><%
ResourceBundle res =
	ResourceBundle.getBundle("messages/connectors");
%>

<div style="font-size:80%">

	<h2>Welcome!</h2><br/>
	
	<p>
		Fluxtream is a fun way to remember what was on your mind any day of the year.
		It is non judgmental and non-normative; it provides you with a smart mirror of
		your daily whereabouts. We do our best to connect the dots but in the end,
		<b>You</b> are the one who knows what it all means, and
		we strive to make it fun and easy for you to write your own story.
	</p><br/>
	<a class="btn" href="javascript:initSequenceExit()">Cancel</a>&nbsp;&nbsp;
	<a class="btn primary" href="javascript:loadModal('/connectors/welcome2')">Next &rarr;</a>
</div>
