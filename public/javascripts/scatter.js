function plotall(dataset) {
	// Setup settings for graphic
	var canvas_width = 500;
	var canvas_height = 500;
	var padding = 30; // for chart edges

	// Create scale functions
	var xScale = d3.scale.linear() // xScale is width of graphic
	.domain([ 0, d3.max(dataset, function(d) {
		return d[0]; // input domain
	}) ]).range([ padding, canvas_width - padding * 2 ]); // output range

	var yScale = d3.scale.linear() // yScale is height of graphic
	.domain([ 0, d3.max(dataset, function(d) {
		return d[1]; // input domain
	}) ]).range([ canvas_height - padding, padding ]); // remember y starts on top going down so we flip

	// Define X axis
	var xAxis = d3.svg.axis().scale(xScale).orient("bottom").ticks(5);

	// Define Y axis
	var yAxis = d3.svg.axis().scale(yScale).orient("left").ticks(5);

	var svgElem = d3.select("svg")
	if (svgElem) {
		svgElem.remove()
	}
	// Create SVG element
	var svg = d3.select("h3") // This is where we put our vis
	.append("svg").attr("width", canvas_width).attr("height", canvas_height)

	// Create Circles
	svg.selectAll("circle").data(dataset).enter().append("circle") // Add circle svg
	.attr("cx", function(d) {
		return xScale(d[0]); // Circle's X
	}).attr("cy", function(d) { // Circle's Y
		return yScale(d[1]);
	}).attr("fill", function(d) {
		return d[2];
	}).attr("r", 8); // radius

	// Add to X axis
	svg.append("g").attr("class", "x axis").attr("transform",
			"translate(0," + (canvas_height - padding) + ")").call(xAxis);

	// Add to Y axis
	svg.append("g").attr("class", "y axis").attr("transform",
			"translate(" + padding + ",0)").call(yAxis);
}

plotall([])

var feed = new EventSource("/gameFeed");
function handler(msg) {
	var data = JSON.parse(msg.data);
	dataset = [];
	for (var i = 0; i < data.entities.length; i++) {
		var newNumber1 = data.entities[i].row
		var newNumber2 = data.entities[i].col
		var color = "black";
		if (data.entities[i].team == 0) {
			color = "green";
		} else if (data.entities[i].team == 1) {
			color = "red";
		} else {
			color = "black";
		}
		dataset.push([ newNumber1, newNumber2, color ]); // Add new number to array
	}
	plotall(dataset)
}
feed.addEventListener("message", handler, false);