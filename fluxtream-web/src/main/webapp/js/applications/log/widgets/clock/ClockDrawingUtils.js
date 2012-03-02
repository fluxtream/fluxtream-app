define([], function() {

	return {
		getDrawingUtils : function(config) {
			
			function arc (center, radius, startAngle, endAngle) {
				if (endAngle - startAngle < 2)
					endAngle += 1;
				var angle = startAngle, coords = toCoords(center, radius,
						angle), path = "M " + coords[0] + " " + coords[1];
				while (angle <= endAngle) {
					coords = toCoords(center, radius, angle);
					path += " L " + coords[0] + " " + coords[1];
					angle += 1;
				}
				return path;
			}

			function toCoords (center, radius, angle) {
				var radians = (angle / 180) * Math.PI, x = center[0]
						+ Math.cos(radians) * radius, y = center[1]
						+ Math.sin(radians) * radius;
				return [ x, y ];
			}
			
			return {
				paintCircle: function(paper, radius, color, opacity) {
					var coords = arc(config.CLOCK_CENTER, radius, 0, 360);
					config.clockCircles.push(function() {
						var path = paper.path(coords);
						path.attr("stroke-width", config.STROKE_WIDTH)
						path.attr("stroke", color);
						return path;
					}());
				}

			}

		}
	}

});
