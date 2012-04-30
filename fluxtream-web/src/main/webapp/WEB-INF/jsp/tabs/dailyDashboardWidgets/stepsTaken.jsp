<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"%>
	
<div class="statsWell span{{stepsTaken.columns}}">

	<div class="statsWell-inner">
		<div class="flx-icon-{{stepsTaken.device}}"></div>
		<div class="statLabel">You took</div>
		<div class="bigFigure">
			{{stepsTaken.steps}} <small>steps</small>
		</div>
	</div>

</div>
