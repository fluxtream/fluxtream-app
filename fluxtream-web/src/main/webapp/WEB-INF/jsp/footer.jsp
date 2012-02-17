<footer id="footer">
<% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
<% String release = ""; if (request.getAttribute("release")!=null) release = "Release " + request.getAttribute("release") + "";%>
<div class="floatR" style="text-align:right;">
	<img class="email_me_icon" style="float:right;vertical-align:top;margin-top:5px;margin-left:10px;" src="/${release}/images/candide.png"/>
	<small><span style="color:#d47028;">Care to tell me something personal?</span><br><strong>Send me an <a href="mailto:candide@fluxtream.com">email!</a></strong></small><br>
</div> 
<div class="floatL bMargin">
	Follow us on twitter <a href="https://twitter.com/#!/_Fluxtream" target="_blank">@_Fluxtream</a>. Read our <a href="http://blog.fluxtream.com" target="_blank">Blog</a><hr><small>Fluxtream - <%=release %> &copy; Palacehotel Software sprl 2011<%=until %> - <a href="http://blog.fluxtream.com/post/13013817558/privacy" target="_blank">Your privacy</a></small>
	

	</div>
</footer>