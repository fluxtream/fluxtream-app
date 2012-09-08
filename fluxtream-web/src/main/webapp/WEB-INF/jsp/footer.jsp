    <footer id="footer" class="container-fluid">
<% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
<% String release = ""; if (request.getAttribute("release")!=null) release = "Release " + request.getAttribute("release") + "";%>
	<div>
		<hr/>
		<small class="flx-credits">Fluxtream - <%=release %> &copy; 2011<%=until %> <br/>The BodyTrack Team @ CMU CREATE Lab &amp; Candide Kemmler</small>
	</div>

	<div class="feedback-call">
	   <div class="anne">
	   </div>
	   <div class="bubbles">
	       <div class="bubble1">
	           <p>We value your <a href="mailto:arwright@andrew.cmu.edu">opinion</a></p>
	           <span class="tail"></span>
	       </div>
	       <div class="bubble2">
	           <p>Send us your <a href="mailto:candide@fluxtream.com">feedback</a></p>
	           <span class="tail"></span>
	       </div>
	   </div>
	   <div class="candide">
	   </div>
	   
	</div>

</footer>