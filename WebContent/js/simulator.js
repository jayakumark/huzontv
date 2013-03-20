
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
	/*mds = mds + "<div style=\"text-align:left\">";
	mds = mds + "	<b>Function 1:</b> Get all still frames in a certain epoch time (in seconds) range.<br>";
	mds = mds + "	Begin: <input type=\"text\" id=\"function_begin_input\" size=20 value=\"1363723213\"><br>";
	mds = mds + "	End: <input type=\"text\" id=\"function_end_input\" value=\"1363723300\" size=20><br>";
	mds = mds + "   <input id=\"function_go_button\" type=button value=\"GO\">";
	mds = mds + "</div>"*/
	mds = mds + "<div style=\"text-align:left\">";
	mds = mds + "	<b>Function 2:</b> Get all still frames in a certain calendar/date time (in seconds) range.<br>";
	mds = mds + "	Begin: <input type=\"text\" id=\"function2_begin_input\" size=20 value=\"20130317_230000\"><br>";
	mds = mds + "	End: <input type=\"text\" id=\"function2_end_input\" value=\"20130317_230100\" size=20><br>";
	mds = mds + "   <input id=\"function2_go_button\" type=button value=\"GO\">";
	mds = mds + "</div>"
	mds = mds + "<div style=\"text-align:left\">";
	mds = mds + "	<b>Function 3:</b> Get all frames for designation above an average score threshold over a period of time<br>";
	mds = mds + "	Designation: <select id=\"function3_designation_select\">";
	for(var a = 0; a < designations.length; a++)
	{
		mds = mds + "	<option value=\"" + designations[a] + "\">" + designations[a] + "</option>";
	}	
	mds = mds + "	</select><br>";
	mds = mds + "	Threshold: <input type=\"text\" id=\"function3_threshold_input\" value=\".9\" size=20><br>";
	mds = mds + "	Begin: <input type=\"text\" id=\"function3_begin_input\" size=20 value=\"20130319_160000\"><br>";
	mds = mds + "	End: <input type=\"text\" id=\"function3_end_input\" value=\"20130319_160100\" size=20><br>";
	mds = mds + "   <input id=\"function3_go_button\" type=button value=\"GO\">";
	mds = mds + "</div>"
	mds = mds + "	<b>Function 4:</b> Get all frames for designation above an average score threshold over a period of time WITH delta<br>";
	mds = mds + "	Designation: <select id=\"function4_designation_select\">";
	for(var a = 0; a < designations.length; a++)
	{
		mds = mds + "	<option value=\"" + designations[a] + "\">" + designations[a] + "</option>";
	}	
	mds = mds + "	</select><br>";
	mds = mds + "	Threshold: <input type=\"text\" id=\"function4_threshold_input\" value=\".9\" size=4><br>";
	mds = mds + "   Delta: <input type=\"text\" id=\"function4_delta_input\" value=\".1\" size=4><br>";
	mds = mds + "	Begin: <input type=\"text\" id=\"function4_begin_input\" size=20 value=\"20130319_160000\"><br>";
	mds = mds + "	End: <input type=\"text\" id=\"function4_end_input\" value=\"20130320_160100\" size=20><br>";
	mds = mds + "   <input id=\"function4_go_button\" type=button value=\"GO\">";
	mds = mds + "</div>"
/*	mds = mds + "<div style=\"text-align:left\">";
	mds = mds + "	<b>Function 4:</b> Get all frames for designation above an average score threshold for consecutive frames over a period of time<br>";
	mds = mds + "	Designation: <input type=\"text\" id=\"function4_designation_input\" size=20 value=\"chris_bailey\"><br>";
	mds = mds + "	Threshold: <input type=\"text\" id=\"function4_threshold_input\" value=\".9\" size=20><br>";
	mds = mds + "	Number of cons. frames: <input type=\"text\" id=\"function4_consecutiveframes_input\" value=\"3\" size=20><br>";
	mds = mds + "	Begin: <input type=\"text\" id=\"function4_begin_input\" size=20 value=\"20130319_160000\"><br>";
	mds = mds + "	End: <input type=\"text\" id=\"function4_end_input\" value=\"20130319_160100\" size=20><br>";
	mds = mds + "   <input id=\"function4_go_button\" type=button value=\"GO\">";
	mds = mds + "</div>"*/
	$("#main_div").html(mds);
	
	/*$("#function_go_button").click(
			function () {
				var rds = "";
				 $.ajax({
						type: 'GET',
						url: "http://localhost:8080/hoozontv/endpoint",
						data: {
				            method: "getFrames",
				            begin: $('#function_begin_input').val(),             
				            end:$('#function_end_input').val()  
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error");
				        	else
				        	{
				        		for(var x = 0; x < data.frames.length; x++)
				        		{
				        			rds = rds + "<div style=\"border: 1px black solid;width:160px;display:inline-block;\"><img src=" + data.frames[x].image_url + " style=\"width:160px;height:90px\"></div>";
				        		}
				        		$("#results_div").html(rds);
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#results_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});*/
	
	$("#function2_go_button").click(
			function () {
				var rds = "";
				var datestring = $('#function2_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function2_end_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
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
	
	$("#function3_go_button").click(
			function () {
				var rds = "";
				var datestring = $('#function3_begin_input').val();
				
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				
				var begin = d.getTime()/1000;
				
				datestring = $('#function3_end_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: "http://localhost:8080/hoozontv/endpoint",
						data: {
				            method: "getFramesByDesignationThresholdAndDelta",
				            begin: begin,             
				            end: end,
				            designation: $('#function3_designation_select').val(),
				            threshold: $('#function3_threshold_input').val()
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
				        			if(data.frames.length > 0)
				        			{	
				        				for(var x = 0; x < data.frames.length; x++)
				        				{
				        					d = new Date(data.frames[x].timestamp_in_seconds *1000);
				        					rds = rds + "<div style=\"border: 1px black solid;width:160px;display:inline-block;\"><img src=" + data.frames[x].image_url + " style=\"width:160px;height:90px\"><br>" + d.toString() + "<br>"+ data.frames[x].score_average + "</div>";
				        				}
				        			}
				        			else
				        				rds = "no matches";
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
	
	$("#function4_go_button").click(
			function () {
				var rds = "";
				var datestring = $('#function4_begin_input').val();
				
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				
				var begin = d.getTime()/1000;
				
				datestring = $('#function4_end_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: "http://localhost:8080/hoozontv/endpoint",
						data: {
				            method: "getFramesByDesignationThresholdAndDelta",
				            begin: begin,             
				            end: end,
				            designation: $('#function4_designation_select').val(),
				            threshold: $('#function4_threshold_input').val(),
				            delta:  $('#function4_delta_input').val()
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
				        			if(data.frames.length > 0)
				        			{	
				        				for(var x = 0; x < data.frames.length; x++)
				        				{
				        					d = new Date(data.frames[x].timestamp_in_seconds *1000);
				        					rds = rds + "<div style=\"border: 1px black solid;width:160px;display:inline-block;\"><img src=" + data.frames[x].image_url + " style=\"width:160px;height:90px\"><br>" + d.toString() + "<br>"+ data.frames[x].score_average + "</div>";
				        				}
				        			}
				        			else
				        				rds = "no matches";
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