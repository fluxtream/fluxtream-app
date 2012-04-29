<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"%>
	
<div class="statsWell span{{caloriesBurned.columns}}">

	<div class="statsWell-inner">
		<div class="flx-icon-{{caloriesBurned.device}}"></div>
		<div class="statLabel">You burned</div>
		<div class="bigFigure">
			{{caloriesBurned.kcals}} <small>kcal</small>
		</div>
	</div>

</div>
