<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"%><%@ page
	import="com.fluxtream.domain.*"%><%@ page import="java.util.List"%><%@ page
	import="java.util.ArrayList"%><%@ page import="java.lang.Object"%>
<%
	GuestSettings settings = (GuestSettings) request
			.getAttribute("settings");
	String firstname = (String) request.getAttribute("firstname");
	String lastname = (String) request.getAttribute("lastname");
%>
<div id="fb-Settings-add-remove"  class="modalForm">
  <h2>Settings</h2>
	<form id="settingsForm" action="/settings/save" method="POST">
		
		<h3>Your Personal Details</h3>
		<table>
			<tr>
				<td><label for="guest_firstname">First name</label></td>
				<td><input type="text" name="guest_firstname"
					value="<%=firstname%>"></td>
			</tr>
			<tr>
				<td><label for="guest_lastname">Last name</label></td>
				<td><input type="text" name="guest_lastname"
					value="<%=lastname%>"></td>
			</tr>
		</table>
		<h3>Units of measure Preferences</h3>
		<table>
			<tr>
				<td><label for="length_measure_unit">Length Measure</label></td>
				<td><select name="length_measure_unit">
						<option <% if (settings.lengthMeasureUnit==GuestSettings.LengthMeasureUnit.SI) out.print("selected=selected");%> value="SI">Cm</option>
						<option <% if (settings.lengthMeasureUnit==GuestSettings.LengthMeasureUnit.FEET_INCHES) out.print("selected=selected");%> value="FEET_INCHES">Feet/Inches</option>
				</select></td>
			</tr>
			<tr>
				<td><label for="distance_measure_unit">Distance Measure</label></td>
				<td><select name="distance_measure_unit">
						<option <% if (settings.distanceMeasureUnit==GuestSettings.DistanceMeasureUnit.SI) out.print("selected=selected");%> value="SI">Km</option>
						<option <% if (settings.distanceMeasureUnit==GuestSettings.DistanceMeasureUnit.MILES_YARDS) out.print("selected=selected");%> value="MILES_YARDS">Miles/Yards</option>
				</select></td>
			</tr>
			<tr>
				<td><label for="weight_measure_unit">Weight Measure</label></td>
				<td><select name="weight_measure_unit">
						<option <% if (settings.weightMeasureUnit==GuestSettings.WeightMeasureUnit.SI) out.print("selected=selected");%> value="SI">Kg/Grams</option>
						<option <% if (settings.weightMeasureUnit==GuestSettings.WeightMeasureUnit.STONES) out.print("selected=selected");%>value="STONES">Stones</option>
						<option <% if (settings.weightMeasureUnit==GuestSettings.WeightMeasureUnit.POUNDS) out.print("selected=selected");%> value="POUNDS">Pounds</option>
				</select></td>
			</tr>
			<tr>
				<td><label for="temperature_unit">Temperatures</label></td>
				<td><select name="temperature_unit">
						<option <% if (settings.temperatureUnit==GuestSettings.TemperatureUnit.CELSIUS) out.print("selected=selected");%> value="CELSIUS">Celsius</option>
						<option <% if (settings.temperatureUnit==GuestSettings.TemperatureUnit.FAHRENHEIT) out.print("selected=selected");%> value="FAHRENHEIT">Fahrenheit</option>
				</select></td>
			</tr>
			<tr>
			 <td colspan="2"><input type="submit" value="Save Settings" class="btn primary"/></td>
			</tr>
			</table>
      <div style="border-top: 1px solid #c4c4c4; padding-top: 10px;">
			<h3>Cancel this account</h3>
  			<table>
  			<tr>
  				<td><a href="javascript:eraseEverything();" id="eraseDataButton" class="btn" style="font-size:13px">Erase all my data</a></td>
  				<td style="width:250px"><i><small style="line-height:90%">You didn't like the experience? Before leaving, please press the 'feedback' button and tell us why!</small></i></td>
  			</tr>
  		  </table>
  		  </div>
	</form>
</div>
