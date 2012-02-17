<%@ page isELIgnored="false"%>
<%@ page import="com.fluxtream.mvc.admin.controllers.*"%>
<%@ page import="com.fluxtream.domain.*"%>
<%@ page import="java.util.*"%>

<%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");

	List<Guest> guests = helper.guestService.getAllGuests();
%>

<div class="row">
	<div class="span3">
		<h2>Re-Index:</h2>
	</div>
	<div class="span13" style="padding-top:5px">
		<button class="btn primary" onclick="initLuceneIndex()">Go!</button>
	</div>
</div>

<div class="row">
	<div class="span3" style="padding-top:20px">
		<p>Search Facets:</p>
	</div>
	<div class="span13">
		<form action="javascript:void(0)">
			<fieldset>
				<div class="clearfix">
					<select name="guestSelect" id="facetsGuestSelect">
						<option value="">Please choose a guest...</option>
						<%
							for (Guest nextGuest : guests) {
								out.println("<option ");
								out.println("value=\"" + nextGuest.getId() + "\">"
										+ nextGuest.username + "</option>");
							}
						%>
					</select>
					<input class="xlarge" id="facetsSearchTerms" name="xlInput" size="30"
						type="text">
					<button onclick="searchFacets($('#facetsGuestSelect').val(), $('#facetsSearchTerms').val())" type="submit" class="btn">Search</button>
				</div>
			</fieldset>
		</form>
	</div>
</div>

<div class="row">

	<div class="span16" id="searchResults"></div>	

</div>
