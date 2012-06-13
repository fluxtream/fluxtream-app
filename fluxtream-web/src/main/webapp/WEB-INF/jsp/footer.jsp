<footer id="footer" class="container-fluid">
<% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
<% String release = ""; if (request.getAttribute("release")!=null) release = "Release " + request.getAttribute("release") + "";%>
	<div>
		<hr/>
		<small>Fluxtream - <%=release %> &copy; 2011<%=until %> The BodyTrack Team @ CMU CREATE Lab &amp; Candide Kemmler</small>
	</div>
</footer>