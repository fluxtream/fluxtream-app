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
<div id="modal" class="modal fade hide">
	<div class="modal-header">
		<a class="close" data-dismiss="modal">&times;</a>
		<h3>Settings</h3>
	</div>
	<form id="settingsForm" action="/settings/save" method="POST">
		<div class="modal-body">
	
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
							<option
								<%if (settings.lengthMeasureUnit == GuestSettings.LengthMeasureUnit.SI)
					out.print("selected=selected");%>
								value="SI">Cm</option>
							<option
								<%if (settings.lengthMeasureUnit == GuestSettings.LengthMeasureUnit.FEET_INCHES)
					out.print("selected=selected");%>
								value="FEET_INCHES">Feet/Inches</option>
					</select></td>
				</tr>
				<tr>
					<td><label for="distance_measure_unit">Distance Measure</label></td>
					<td><select name="distance_measure_unit">
							<option
								<%if (settings.distanceMeasureUnit == GuestSettings.DistanceMeasureUnit.SI)
					out.print("selected=selected");%>
								value="SI">Km</option>
							<option
								<%if (settings.distanceMeasureUnit == GuestSettings.DistanceMeasureUnit.MILES_YARDS)
					out.print("selected=selected");%>
								value="MILES_YARDS">Miles/Yards</option>
					</select></td>
				</tr>
				<tr>
					<td><label for="weight_measure_unit">Weight Measure</label></td>
					<td><select name="weight_measure_unit">
							<option
								<%if (settings.weightMeasureUnit == GuestSettings.WeightMeasureUnit.SI)
					out.print("selected=selected");%>
								value="SI">Kg/Grams</option>
							<option
								<%if (settings.weightMeasureUnit == GuestSettings.WeightMeasureUnit.STONES)
					out.print("selected=selected");%>
								value="STONES">Stones</option>
							<option
								<%if (settings.weightMeasureUnit == GuestSettings.WeightMeasureUnit.POUNDS)
					out.print("selected=selected");%>
								value="POUNDS">Pounds</option>
					</select></td>
				</tr>
				<tr>
					<td><label for="temperature_unit">Temperatures</label></td>
					<td><select name="temperature_unit">
							<option
								<%if (settings.temperatureUnit == GuestSettings.TemperatureUnit.CELSIUS)
					out.print("selected=selected");%>
								value="CELSIUS">Celsius</option>
							<option
								<%if (settings.temperatureUnit == GuestSettings.TemperatureUnit.FAHRENHEIT)
					out.print("selected=selected");%>
								value="FAHRENHEIT">Fahrenheit</option>
					</select></td>
				</tr>
			</table>
		</div>
		<div class="modal-footer">
			<input type="submit" value="Save Settings"
									class="btn btn-primary" onclick="App.closeModal()" />
			<a href="javascript:App.eraseEverything();" id="eraseDataButton"
				style="font-size: 13px">Erase all my data</a>
		</div>
	</form>
</div>
