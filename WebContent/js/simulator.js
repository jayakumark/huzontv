
document.addEventListener('DOMContentLoaded', function () {
	
	var designations = null;
	$.ajax({
		type: 'GET',
		url: "http://localhost:8080/hoozontv/endpoint",
		data: {
            method: "getDesignations",
            station: "wkyt"       
		},
        dataType: 'json',
        async: false,
        success: function (data, status) {
        	if (data.response_status == "error")
        		$("#results_div").html("error getting station designations");
        	else
        	{
        		designations = data.designations;
        		$("#results_div").html("Designations successfully retrieved.");
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#results_div").html("ajax error");
            console.log(textStatus, errorThrown);
        }
	});
	
	var mds = "";
	mds = mds + "<div style=\"font-size:16px\">";
	mds = mds + "	Available functions:";
	mds = mds + "</div>"
	mds = mds + "<table style=\"width:100%\">";
	mds = mds + "	<tr>";
	mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
	mds = mds + "	<b>Function 2:</b> Get all still frames in a certain calendar/date time (in seconds) range.<br>";
	mds = mds + "	Begin: <input type=\"text\" id=\"function2_begin_input\" size=20 value=\"20130317_230000\"><br>";
	mds = mds + "	End: <input type=\"text\" id=\"function2_end_input\" value=\"20130317_230100\" size=20><br>";
	mds = mds + "   <input id=\"function2_go_button\" type=button value=\"GO\">";
	mds = mds + "		</td>";
	mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
	mds = mds + "	<b>Function 6:</b> Get all frames for designation above calculated homogeneity threshold<br>";
	mds = mds + "	Designation: <select id=\"function6_designation_select\">";
	for(var a = 0; a < designations.length; a++)
	{
		mds = mds + "	<option value=\"" + designations[a] + "\">" + designations[a] + "</option>";
	}	
	mds = mds + "	</select><br>";
	mds = mds + "	Modifier: <input type=\"text\" id=\"function6_modifier_input\" value=\"1.0\" size=4><br>";
	mds = mds + "	Delta: <input type=\"text\" id=\"function6_delta_input\" value=\".1\" size=4><br>";
	mds = mds + "	Begin: <input type=\"text\" id=\"function6_begin_input\" size=20 value=\"20130319_160000\"><br>";
	mds = mds + "	End: <input type=\"text\" id=\"function6_end_input\" value=\"20130319_160100\" size=20><br>";
	mds = mds + "   <input id=\"function6_go_button\" type=button value=\"GO\">";
	mds = mds + "		</td>";
	mds = mds + "	</tr>";
	mds = mds + "	<tr>";
	mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
	mds = mds + "			<b>Function 7:</b> Graph one designee over time.<br>";
	mds = mds + "			Designation: <select id=\"function7_designation_select\">";
	for(var a = 0; a < designations.length; a++)
	{
		mds = mds + "	<option value=\"" + designations[a] + "\">" + designations[a] + "</option>";
	}	
	mds = mds + "			</select><br>";
	mds = mds + "			Single Thresh Modifier: <input type=\"text\" id=\"function7_singlemodifier_input\" value=\"1\" size=4><br>";
	mds = mds + "			MA Thresh Modifier: <input type=\"text\" id=\"function7_mamodifier_input\" value=\".63\" size=4><br>";
	mds = mds + "			Secs2avg: <input type=\"text\" id=\"function7_seconds2average_input\" value=\"5\" size=4><br>";
	mds = mds + "			Alert waiting period: <input type=\"text\" id=\"function7_awp_input\" value=\"60\" size=4><br>";
	mds = mds + "			Begin: <input type=\"text\" id=\"function7_begin_input\" size=20 value=\"20130319_170000\"><br>";
	mds = mds + "			End: <input type=\"text\" id=\"function7_end_input\" value=\"20130319_173000\" size=20><br>";
	mds = mds + "   		<input id=\"function7_go_button\" type=button value=\"GO\">";
	mds = mds + "		</td>";
	mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
	mds = mds + "			<b>Function 8:</b> Simulate single frame<br>";
	mds = mds + "			Single Thresh Modifier: <input type=\"text\" id=\"function8_singlemodifier_input\" value=\".97\" size=4><br>";
	mds = mds + "			MA Thresh Modifier: <input type=\"text\" id=\"function8_mamodifier_input\" value=\".67\" size=4><br>";
	mds = mds + "			Moving Avg Window: <input type=\"text\" id=\"function8_maw_input\" value=\"5\" size=4><br>";
	mds = mds + "			Alert waiting period: <input type=\"text\" id=\"function8_awp_input\" value=\"60\" size=4><br>";
	mds = mds + "			End: <input type=\"text\" id=\"function8_end_input\" value=\"20130320_163309\" size=20><br>";
	mds = mds + "   		<input id=\"function8_go_button\" type=button value=\"GO\">";
	mds = mds + "		</td>";
	mds = mds + "	</tr>";	
	
	mds = mds + "</table>";
	$("#main_div").html(mds);
	
	$("#function2_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function2_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function2_end_input').val();
			    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: "http://localhost:8080/hoozontv/endpoint",
						data: {
				            method: "getFrames",
				            begin: begin,             
				            end: end  
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error");
				        	else
				        	{
				        		if(data.frames.length > 100000)
				        		{
				        			$("#results_div").html("too many results. Try again.");
				        		}	
				        		else
				        		{
				        			for(var x = 0; x < data.frames.length; x++)
				        			{
				        				rds = rds + "<div style=\"border: 1px black solid;width:160px;display:inline-block;\"><img src=" + data.frames[x].image_url + " style=\"width:160px;height:90px\"></div>";
				        			}
				        			$("#results_div").html(rds);
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#results_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});
	
	$("#function6_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function6_begin_input').val();
				
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				
				var begin = d.getTime()/1000;
				
				datestring = $('#function6_end_input').val();
				d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: "http://localhost:8080/hoozontv/endpoint",
						data: {
				            method: "getFramesByDesignationAndHomogeneityThreshold",
				            begin: begin,             
				            end: end,
				            designation: $('#function6_designation_select').val(),
				            modifier: $('#function6_modifier_input').val(),
				            delta: $('#function6_delta_input').val()
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error: " + data.message);
				        	else
				        	{
				        		if(data.frames.length > 100000)
				        		{
				        			$("#results_div").html("too many results. Try again.");
				        		}	
				        		else
				        		{
				        			if(data.frames.length > 0)
				        			{	
				        				for(var x = 0; x < data.frames.length; x++)
				        				{
				        					d = new Date(data.frames[x].timestamp_in_seconds *1000);
				        					if(data.frames[x].streak > 4)
				        						rds = rds + "<div style=\"border: 2px red solid;width:160px;display:inline-block;\">";
				        					else
				        						rds = rds + "<div style=\"border: 1px black solid;width:160px;display:inline-block;\">";
				        					rds = rds + "<img src=" + data.frames[x].image_url + " style=\"width:160px;height:90px\">";
				        					rds = rds + "<br>" + d.toString() + "<br>avg4des:"+ data.frames[x].score_average;
				        					rds = rds + "<br>h-score:" + data.frames[x].homogeneity_score;
				        					rds = rds + "<br>threshold:" + data.frames[x].threshold;
				        					rds = rds + "<br>closest_desg:" + data.frames[x].closest_designation;
				        					rds = rds + "<br>closest_avg:" + data.frames[x].closest_avg;
				        					rds = rds + "<br>closest_delta:" + (data.frames[x].score_average - data.frames[x].closest_avg);
				        					rds = rds + "<br>streak:" + data.frames[x].streak + "</div>";
				        				}
				        			}
				        			else
				        				rds = "no matches";
				        			
				        			rds = "frames processed:" + data.frames_processed + "<br>delta suppressions:" + data.delta_suppressions + "<br>" + rds;
				        			$("#results_div").html(rds);
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#results_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});
	
	$("#function7_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function7_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function7_end_input').val();
				d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: "http://localhost:8080/hoozontv/endpoint",
						data: {
				            method: "getFramesByDesignation",
				            begin: begin,             
				            end: end,
				            designation: $('#function7_designation_select').val(),
				            ma_modifier: $('#function7_mamodifier_input').val(),
				            single_modifier: $('#function7_singlemodifier_input').val(),
				            alert_waiting_period: $('#function7_awp_input').val(),
				            seconds_to_average: $('#function7_seconds2average_input').val()
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error");
				        	else
				        	{
				        		if(data.frames.length > 100000)
				        		{
				        			$("#results_div").html("too many results. Try again.");
				        		}	
				        		else
				        		{
				        			//rds = rds + "<table style=\"border-spacing:0px\"><tr>";
				        			//var lineheight = 200;
				        			var scores = []; var moving_avg = [];
				        			for(var x = 0; x < data.frames.length; x++)
				        			{
				        				scores.push(data.frames[x].designation_score);
				        				moving_avg.push(data.frames[x].moving_average);
				        				//lineheight = Math.floor(200 * data.frames[x].designation_score);
				        				//rds = rds + "<td style=\"vertical-align:bottom\"><img src=\"images/vertical_line_200px.png\" style=\"width:1px;height:" + lineheight + "px\"></td>";
				        				//rds = rds + "<div style=\"border: 1px black solid;width:160px;display:inline-block;\"><img src=" + data.frames[x].image_url + " style=\"width:160px;height:90px\"></div>";
				        			}
				        			//rds = rds + "</tr></table>";
				        			//$("#results_div").html(rds);
				        			//alert(scores);
				        			//$.jqplot.config.enablePlugins = true;
				        			//alert(data.frames[0].homogeneity_score +" " + (data.frames[0].homogeneity_score * $('#function7_modifier_input').val()));
				        			var plot1 = $.jqplot ('chart1', [scores, moving_avg],{
				        				axes: {
				        					yaxis: {
				        			            min:0,max:1
				        			        }
				        				},
				        				canvasOverlay: {
				        					show: true,
				        			        objects: [
				        			       /*   {horizontalLine: {
				        			            name: 'pebbles',
				        			            y: 0,
				        			            lineWidth: 3,
				        			            color: 'rgb(100, 55, 124)',
				        			            shadow: true,
				        			            lineCap: 'butt',
				        			            xOffset: 0
				        			          }},*/
				        			          {dashedHorizontalLine: {
				        			            name: 'bam-bam',
				        			            y: (data.frames[0].homogeneity_score * $('#function7_mamodifier_input').val()),
				        			            lineWidth: 4,
				        			            dashPattern: [8, 16],
				        			            lineCap: 'round',
				        			            xOffset: '25',
				        			            color: 'rgb(66, 98, 144)',
				        			            shadow: false
				        			          }}
				        			        ]
				        			      }
				        			    });
				        			
				        			if(data.alertframes.length > 0)
				        			{	
				        				rds = rds + "MA thresh:" + data.alertframes[0].ma_threshold;
				        				rds = rds + "<br>Single thresh:" + data.alertframes[0].single_threshold;
				        				rds = rds + "<br>Homogeneity:" + data.alertframes[0].homogeneity_score;
				        				rds = rds + "<br>";
				        			}
				        			for(x = 0; x < data.alertframes.length; x++)
				        			{
				        				d = new Date(data.alertframes[x].timestamp_in_seconds *1000);
				        				rds = rds + "<div style=\"border: 1px black solid;width:160px;display:inline-block;\">";
			        					rds = rds + "<img src=" + data.alertframes[x].image_url + " style=\"width:160px;height:90px\">";
			        					rds = rds + "<br>" + d.toString();
			        					rds = rds + "<br>avg4des:"+ data.alertframes[x].designation_score;
			        					rds = rds + "<br>h-score:" + data.alertframes[x].homogeneity_score;
			        					rds = rds + "<br>moving_avg:" + data.alertframes[x].moving_average;
			        					//rds = rds + "<br>closest_desg:" + data.frames[x].closest_designation;
			        				//	rds = rds + "<br>closest_avg:" + data.frames[x].closest_avg;
			        				//	rds = rds + "<br>closest_delta:" + (data.frames[x].score_average - data.frames[x].closest_avg);
			        				//	rds = rds + "<br>streak:" + data.frames[x].streak;
			        					rds = rds + "</div>";
			        				}	
				        			$("#results_div").html(rds);
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#results_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});
	
	$("#function8_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function8_end_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				//alert($('#function8_maw_input').val());
				$.ajax({
						type: 'GET',
						url: "http://localhost:8080/hoozontv/endpoint",
						data: {
				            method: "simulateNewFrame",
				            end: end,
				            ma_modifier: $('#function8_mamodifier_input').val(),
				            single_modifier: $('#function8_singlemodifier_input').val(),
				            alert_waiting_period: $('#function8_awp_input').val(),
				            moving_average_window: $('#function8_maw_input').val()
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error");
				        	else
				        	{
				        		if(data.frames.length > 100000)
				        		{
				        			$("#results_div").html("too many results. Try again.");
				        		}	
				        		else
				        		{
				        			var max_score = 0;
				        			var max_designation = "";
				        			for(var c = 0; c < data.designation_averages.length; c++)
				        			{
				        				if(data.designation_averages[c].average > max_score)
				        				{
				        					max_score = data.designation_averages[c].average;
				        					max_designation = data.designation_averages[c].designation;
				        				}
				        			}	
				        			rds = rds + "max designation for this frame:" + max_designation;
		        					rds = rds + "<br>max designation score for this frame:" + max_score;
		        					rds = rds + "<br>alert_fired:" + data.alert_fired;
		        					rds = rds + "<br><br>";
				        			for(var x = 0; x < data.frames.length; x++)
				        			{
				        				d = new Date(data.frames[x].timestamp_in_seconds *1000);
				        				rds = rds + "<div style=\"border: 1px black solid;width:160px;display:inline-block;\">";
			        					rds = rds + "<img src=" + data.frames[x].image_url + " style=\"width:160px;height:90px\">";
			        					rds = rds + "<br>" + d.toString();
			        					rds = rds + "</div>";
				        			}
				        			$("#results_div").html(rds);
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#results_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});
	
});