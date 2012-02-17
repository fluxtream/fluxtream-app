<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="java.util.List"
%><%@ page import="com.fluxtream.services.SettingsService"
%><%@ page import="java.util.ArrayList"
%><%@ page import="java.lang.Object"
%><%
	SettingsService settingsService = (SettingsService) request.getAttribute("settingsService");
	String address = (String) request.getAttribute("address");
	Boolean change = (Boolean) request.getAttribute("change");
	String since = (String) request.getAttribute("since");
	if (since==null||since.equals("")) since = "01/01/2007";
	long guestId = (Long) request.getAttribute("guestId");%><div id="fb-Settings-add-remove"  class="modalForm">
  <h2>Your home address</h2><form id="setAddressForm" action="/settings/setAddress" method="POST">
  	<%if (!change) { %>
	<p>You haven't specified a home address, yet. Or maybe not for this period of your life.</p>
	<% } %>
	<h3>Please type an address</h3>
	<p>Hit enter and retry until the correct address shows up below.</p>
	<input type="text" onkeypress="if(event.which==13) {geocode($(this).val());$('#setAddress_errorMessage').hide();return false;}" value="<%=address%>">
	<div style="font-weight:bold; margin-top: 10px" class="geocode_addresses">
	</div>
	<p id="setAddress_errorMessage" style="display:none;font-color:#ff0000">
	</p>
	<h3>Since when are you staying here?</h3>
  	<%if (!change) { %>
	<p>You may have stayed somewhere else before. 
	We'll get to that when you browse to a day before the date 
	that you specify here</p>
	<% } %>
	<input name="sinceDate" type="text" style="width:65px" id="sinceDate" value="<%=since%>"><br/>
	<c:if test="${change}">
		<input type="hidden" name="guest_address_id" id="guest_address_id"/>
	</c:if>
	<input type="hidden" name="geocode_address" id="geocode_address"/>
	<input type="hidden" name="geocode_latitude" id="geocode_latitude"/>
	<input type="hidden" name="geocode_longitude" id="geocode_longitude"/>
	<input type="submit" class="btn primary" value="Set My Address"/>
	<a href="javascript:$.facebox.close()" class="btn">Cancel</a>
</form>
</div>