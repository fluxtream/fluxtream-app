<footer id="footer" class="container-fluid">
<% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
<% String release = ""; if (request.getAttribute("release")!=null) release = request.getAttribute("release") + "";%>
	<div>
		<hr/>
		<small class="flx-credits"><span title="<%=release%>">Fluxtream 1.0</span> <br> &copy; 2011<%=until %> The BodyTrack Team @ CMU CREATE Lab &amp; Candide Kemmler</small>
	</div>

	<div class="feedback-call">
	   <div class="anne">
	   </div>
	   <div class="bubbles">
	       <div class="bubble1">
	           <p>We value <a href="mailto:arwright@andrew.cmu.edu">your opinion</a></p>
	           <span class="tail"></span>
	       </div>
	       <div class="bubble2">
	           <p>Pls send us <a href="mailto:candide+feedback@fluxtream.org">your feedback</a></p>
	           <span class="tail"></span>
	       </div>
	   </div>
	   <div class="candide">
	   </div>
	</div>

</footer>