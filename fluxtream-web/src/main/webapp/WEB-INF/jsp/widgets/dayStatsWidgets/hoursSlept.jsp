<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"%>

<div class="statsWell span{{hoursSlept.columns}}">
	<div class="statsWell-inner">
		<div class="flx-icon-{{hoursSlept.device}}"></div>
		<div class="statLabel">You slept</div>
		<div class="bigFigure">{{hoursSlept.hours}} <small>hrs</small> {{hoursSlept.minutes}} <small>min</small></div>
	</div>
</div>
