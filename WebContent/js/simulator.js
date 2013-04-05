/*
 * Date Format 1.2.3
 * (c) 2007-2009 Steven Levithan <stevenlevithan.com>
 * MIT license
 *
 * Includes enhancements by Scott Trenda <scott.trenda.net>
 * and Kris Kowal <cixar.com/~kris.kowal/>
 *
 * Accepts a date, a mask, or a date and a mask.
 * Returns a formatted version of the given date.
 * The date defaults to the current date/time.
 * The mask defaults to dateFormat.masks.default.
 */

var dateFormat = function () {
	var	token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
		timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
		timezoneClip = /[^-+\dA-Z]/g,
		pad = function (val, len) {
			val = String(val);
			len = len || 2;
			while (val.length < len) val = "0" + val;
			return val;
		};

	// Regexes and supporting functions are cached through closure
	return function (date, mask, utc) {
		var dF = dateFormat;

		// You can't provide utc if you skip other args (use the "UTC:" mask prefix)
		if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
			mask = date;
			date = undefined;
		}

		// Passing date through Date applies Date.parse, if necessary
		date = date ? new Date(date) : new Date;
		if (isNaN(date)) throw SyntaxError("invalid date");

		mask = String(dF.masks[mask] || mask || dF.masks["default"]);

		// Allow setting the utc argument via the mask
		if (mask.slice(0, 4) == "UTC:") {
			mask = mask.slice(4);
			utc = true;
		}

		var	_ = utc ? "getUTC" : "get",
			d = date[_ + "Date"](),
			D = date[_ + "Day"](),
			m = date[_ + "Month"](),
			y = date[_ + "FullYear"](),
			H = date[_ + "Hours"](),
			M = date[_ + "Minutes"](),
			s = date[_ + "Seconds"](),
			L = date[_ + "Milliseconds"](),
			o = utc ? 0 : date.getTimezoneOffset(),
			flags = {
				d:    d,
				dd:   pad(d),
				ddd:  dF.i18n.dayNames[D],
				dddd: dF.i18n.dayNames[D + 7],
				m:    m + 1,
				mm:   pad(m + 1),
				mmm:  dF.i18n.monthNames[m],
				mmmm: dF.i18n.monthNames[m + 12],
				yy:   String(y).slice(2),
				yyyy: y,
				h:    H % 12 || 12,
				hh:   pad(H % 12 || 12),
				H:    H,
				HH:   pad(H),
				M:    M,
				MM:   pad(M),
				s:    s,
				ss:   pad(s),
				l:    pad(L, 3),
				L:    pad(L > 99 ? Math.round(L / 10) : L),
				t:    H < 12 ? "a"  : "p",
				tt:   H < 12 ? "am" : "pm",
				T:    H < 12 ? "A"  : "P",
				TT:   H < 12 ? "AM" : "PM",
				Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
				o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
				S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
			};

		return mask.replace(token, function ($0) {
			return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
		});
	};
}();

// Some common format strings
dateFormat.masks = {
	"default":      "ddd mmm dd yyyy HH:MM:ss",
	shortDate:      "m/d/yy",
	mediumDate:     "mmm d, yyyy",
	longDate:       "mmmm d, yyyy",
	fullDate:       "dddd, mmmm d, yyyy",
	shortTime:      "h:MM TT",
	mediumTime:     "h:MM:ss TT",
	longTime:       "h:MM:ss TT Z",
	isoDate:        "yyyy-mm-dd",
	isoTime:        "HH:MM:ss",
	isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
	isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
	dayNames: [
		"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	],
	monthNames: [
		"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
	]
};

// For convenience...
Date.prototype.format = function (mask, utc) {
	return dateFormat(this, mask, utc);
};


document.addEventListener('DOMContentLoaded', function () {
	
	var designations = null;
	$.ajax({
		type: 'GET',
		url: "http://hoozontv.elasticbeanstalk.com/endpoint",
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
	mds = mds + "<div style=\"font-size:16px;font-weight:bold\">";
	mds = mds + "	Available functions:";
	mds = mds + "</div>"
	mds = mds + "<table style=\"width:100%;border-spacing:10px\">";
	mds = mds + "	<tr>";
	mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
	mds = mds + "			<table style=\"border-spacing:3px\">";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=3>";
	mds = mds + "						<b>Function 1:</b> Get all still frames in a certain calendar/date time (in seconds) range.";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Begin: <input type=\"text\" id=\"function1_begin_input\" size=13 value=\"20130320_050000\"> ";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						End: <input type=\"text\" id=\"function1_end_input\" value=\"20130320_050500\" size=13> ";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "  						<input id=\"function1_go_button\" type=button value=\"GO\">";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "			</table>";
	mds = mds + "		</td>";
	mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
	mds = mds + "			<table style=\"border-spacing:3px\">";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=3>";
	mds = mds + "						<b>Function 2:</b> Get all frames for designation above auto-generated homogeneity threshold";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Designation: <select id=\"function2_designation_select\">";
	for(var a = 0; a < designations.length; a++)
	{
		mds = mds + "							<option value=\"" + designations[a] + "\">" + designations[a] + "</option>";
	}	
	mds = mds + "	</select> ";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Thresh modifier: <input type=\"text\" id=\"function2_modifier_input\" value=\"1\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Delta: <input type=\"text\" id=\"function2_delta_input\" value=\".1\" size=4> ";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Begin: <input type=\"text\" id=\"function2_begin_input\" size=13 value=\"20130320_050000\"> ";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						End: <input type=\"text\" id=\"function2_end_input\" value=\"20130320_060000\" size=13> ";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "   					<input id=\"function2_go_button\" type=button value=\"GO\">";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "			</table>";
	mds = mds + "		</td>";
	mds = mds + "	</tr>";
	mds = mds + "	<tr>";
	mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
	mds = mds + "			<table style=\"border-spacing:3px\">";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=4>";
	mds = mds + "						<b>Function 3:</b> Graph one designee over time.";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Designation: <select id=\"function3_designation_select\">";
	for(var a = 0; a < designations.length; a++)
	{
		mds = mds + "						<option value=\"" + designations[a] + "\">" + designations[a] + "</option>";
	}	
	mds = mds + "							</select>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Single Thresh Modifier: <input type=\"text\" id=\"function3_singlemodifier_input\" value=\"1\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						MA Thresh Modifier: <input type=\"text\" id=\"function3_mamodifier_input\" value=\".67\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Secs2avg: <input type=\"text\" id=\"function3_seconds2average_input\" value=\"5\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Alert waiting period: <input type=\"text\" id=\"function3_awp_input\" value=\"3600\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Begin: <input type=\"text\" id=\"function3_begin_input\" size=13 value=\"20130320_050000\">";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						End: <input type=\"text\" id=\"function3_end_input\" value=\"20130320_060000\" size=13>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "   					<input id=\"function3_go_button\" type=button value=\"GO\">";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "			</table>";
	mds = mds + "		</td>";
	mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
	mds = mds + "			<table style=\"border-spacing:3px\">";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=4>";
	mds = mds + "						<b>Function 4:</b> Simulate alerts for a given time range:";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Single Thresh Modifier: <input type=\"text\" id=\"function4_singlemodifier_input\" value=\"1.0\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						MA Thresh Modifier: <input type=\"text\" id=\"function4_mamodifier_input\" value=\".67\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Moving Avg Window: <input type=\"text\" id=\"function4_maw_input\" value=\"4\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Alert waiting period: <input type=\"text\" id=\"function4_awp_input\" value=\"7200\" size=4>";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "				<tr>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						Begin: <input type=\"text\" id=\"function4_begin_input\" size=13 value=\"20130320_050000\"><br>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "						End: <input type=\"text\" id=\"function4_end_input\" value=\"20130320_233500\" size=13><br>";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "   					<input id=\"function4_go_button\" type=button value=\"GO\">";
	mds = mds + "					</td>";
	mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
	mds = mds + "					</td>";
	mds = mds + "				</tr>";
	mds = mds + "			</table>";
	mds = mds + "		</td>";
	mds = mds + "	</tr>";	
	mds = mds + "</table>";
	$("#main_div").html(mds);
	
	$("#function1_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function1_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function1_end_input').val();
			    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: "http://hoozontv.elasticbeanstalk.com/endpoint",
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
				        				rds = rds + "<div style=\"border: 1px black solid;width:200px;display:inline-block;\"><img src=http://192.168.2.101/hoozon_wkyt/" + data.frames[x].image_name + " style=\"width:200px;height:113px\"></div>";
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
						url: "http://hoozontv.elasticbeanstalk.com/endpoint",
						data: {
				            method: "getFramesByDesignationAndHomogeneityThreshold",
				            begin: begin,             
				            end: end,
				            designation: $('#function2_designation_select').val(),
				            modifier: $('#function2_modifier_input').val(),
				            delta: $('#function2_delta_input').val()
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
				        					rds = rds + "<div style=\"border: 1px black solid;width:200px;display:inline-block;\">";
				        					rds = rds + "<img src=\"http://192.168.2.101/hoozon_wkyt/" + data.frames[x].image_name + "\" style=\"width:200px;height:113px\">";
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
	
	$("#function3_go_button").click(
			function () {
				$("#results_div").html("<span style=\"font-size:20px;font-weight:bold\">BLUE = frame scores<br>ORANGE = moving average<br>DASHED LINE = alert threshold (for moving average)</span>");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function3_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function3_end_input').val();
				d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: "http://hoozontv.elasticbeanstalk.com/endpoint",
						data: {
				            method: "getFramesByDesignation",
				            begin: begin,             
				            end: end,
				            designation: $('#function3_designation_select').val(),
				            ma_modifier: $('#function3_mamodifier_input').val(),
				            single_modifier: $('#function3_singlemodifier_input').val(),
				            alert_waiting_period: $('#function3_awp_input').val(),
				            seconds_to_average: $('#function3_seconds2average_input').val()
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
				        				//rds = rds + "<div style=\"border: 1px black solid;width:200px;display:inline-block;\"><img src=\"http://192.168.2.101/hoozon_wkyt/" + data.frames[data.frames.length -1].image_name + "\" style=\"width:200px;height:113px\"></div>";
				        			}
				        			//rds = rds + "</tr></table>";
				        			//$("#results_div").html(rds);
				        			//alert(scores);
				        			//$.jqplot.config.enablePlugins = true;
				        			//alert(data.frames[0].homogeneity_score +" " + (data.frames[0].homogeneity_score * $('#function3_modifier_input').val()));
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
				        			            y: (data.frames[0].homogeneity_score * $('#function3_mamodifier_input').val()),
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
				        			/*
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
				        				rds = rds + "<div style=\"border: 1px black solid;width:200px;display:inline-block;\">";
			        					rds = rds + "<img src=\"http://192.168.2.101/hoozon_wkyt/" + data.frames[data.frames.length -1].image_name + "\" style=\"width:200px;height:113px\">";
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
				        			$("#results_div").html(rds);*/
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
	
	/*
	$("#function8_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var datestring = $('#function8_ts_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var ts = d.getTime()/1000;
				// end, ma_modifier, single_modifier, alert_waiting_period, moving_average_window
				simulateNewFrame(ts,  $('#function8_mamodifier_input').val(), $('#function8_singlemodifier_input').val(), $('#function8_awp_input').val(), $('#function8_maw_input').val());
			});*/
	
	$("#function4_go_button").click(
			function () {
				$("#alerts_div").html("");
				$("#results_div").html("");
				$("#chart1").html("");
				resetAllLastAlerts("wkyt");
				var rds = "";
				var datestring = $('#function4_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function4_end_input').val();
				d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				// end, ma_modifier, single_modifier, alert_waiting_period, moving_average_window
				var current_ts = begin;
				var x = 0;
				var alert_triggered = false;
				while(current_ts <= end)
				{
					alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
					if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
						current_ts = alert_triggered; // alert_triggered is the next valid frame
					if(alert_triggered == true)
					{
						setTimeout(function(){
							while(current_ts <= end)
							{
								alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
								if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
									current_ts = alert_triggered; // alert_triggered is the next valid frame
								if(alert_triggered == true)
								{
									setTimeout(function(){
										while(current_ts <= end)
										{
											alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
											if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
												current_ts = alert_triggered; // alert_triggered is the next valid frame
											if(alert_triggered == true)
											{
												setTimeout(function(){
													while(current_ts <= end)
													{
														alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
														if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
															current_ts = alert_triggered; // alert_triggered is the next valid frame
														if(alert_triggered == true)
														{
															setTimeout(function(){
																while(current_ts <= end)
																{
																	alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																	if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																		current_ts = alert_triggered; // alert_triggered is the next valid frame
																	if(alert_triggered == true)
																	{
																		setTimeout(function(){
																			while(current_ts <= end)
																			{
																				alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																				if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																					current_ts = alert_triggered; // alert_triggered is the next valid frame
																				if(alert_triggered == true)
																				{
																					setTimeout(function(){
																						while(current_ts <= end)
																						{
																							alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																							if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																								current_ts = alert_triggered; // alert_triggered is the next valid frame
																							if(alert_triggered == true)
																							{
																								setTimeout(function(){
																									while(current_ts <= end)
																									{
																										alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																										if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																											current_ts = alert_triggered; // alert_triggered is the next valid frame
																										if(alert_triggered == true)
																										{
																											setTimeout(function(){
																												while(current_ts <= end)
																												{
																													alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																													if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																														current_ts = alert_triggered; // alert_triggered is the next valid frame
																													if(alert_triggered == true)
																													{
																														setTimeout(function(){
																															while(current_ts <= end)
																															{
																																alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																	current_ts = alert_triggered; // alert_triggered is the next valid frame
																																if(alert_triggered == true)
																																{
																																	setTimeout(function(){
																																		while(current_ts <= end)
																																		{
																																			alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																			if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																				current_ts = alert_triggered; // alert_triggered is the next valid frame
																																			if(alert_triggered == true)
																																			{
																																				setTimeout(function(){
																																					while(current_ts <= end)
																																					{
																																						alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																						if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																							current_ts = alert_triggered; // alert_triggered is the next valid frame
																																						if(alert_triggered == true)
																																						{
																																							setTimeout(function(){
																																								while(current_ts <= end)
																																								{
																																									alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																									if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																										current_ts = alert_triggered; // alert_triggered is the next valid frame
																																									if(alert_triggered == true)
																																									{
																																										setTimeout(function(){
																																											while(current_ts <= end)
																																											{
																																												alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																												if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																													current_ts = alert_triggered; // alert_triggered is the next valid frame
																																												if(alert_triggered == true)
																																												{
																																													setTimeout(function(){
																																														while(current_ts <= end)
																																														{
																																															alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																															if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																current_ts = alert_triggered; // alert_triggered is the next valid frame
																																															if(alert_triggered == true)
																																															{
																																																setTimeout(function(){
																																																	while(current_ts <= end)
																																																	{
																																																		alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																		if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																			current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																		if(alert_triggered == true)
																																																		{
																																																			setTimeout(function(){
																																																				while(current_ts <= end)
																																																				{
																																																					alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																					if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																						current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																					if(alert_triggered == true)
																																																					{
																																																						setTimeout(function(){
																																																							while(current_ts <= end)
																																																							{
																																																								alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																								if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																									current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																								if(alert_triggered == true)
																																																								{
																																																									setTimeout(function(){
																																																										while(current_ts <= end)
																																																										{
																																																											alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																											if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																												current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																											if(alert_triggered == true)
																																																											{
																																																												setTimeout(function(){
																																																													while(current_ts <= end)
																																																													{
																																																														alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																														if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																															current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																														if(alert_triggered == true)
																																																														{
																																																															setTimeout(function(){
																																																																while(current_ts <= end)
																																																																{
																																																																	alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																	if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																		current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																	if(alert_triggered == true)
																																																																	{
																																																																		setTimeout(function(){
																																																																			while(current_ts <= end)
																																																																			{
																																																																				alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																				if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																					current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																				if(alert_triggered == true)
																																																																				{
																																																																					setTimeout(function(){
																																																																						while(current_ts <= end)
																																																																						{
																																																																							alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																							if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																								current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																							if(alert_triggered == true)
																																																																							{
																																																																								setTimeout(function(){
																																																																									while(current_ts <= end)
																																																																									{
																																																																										alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																										if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																											current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																										if(alert_triggered == true)
																																																																										{
																																																																											setTimeout(function(){
																																																																												while(current_ts <= end)
																																																																												{
																																																																													alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																													if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																														current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																													if(alert_triggered == true)
																																																																													{
																																																																														setTimeout(function(){
																																																																															while(current_ts <= end)
																																																																															{
																																																																																alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																	current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																if(alert_triggered == true)
																																																																																{
																																																																																	setTimeout(function(){
																																																																																		while(current_ts <= end)
																																																																																		{
																																																																																			alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																			if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																				current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																			if(alert_triggered == true)
																																																																																			{
																																																																																				setTimeout(function(){
																																																																																					while(current_ts <= end)
																																																																																					{
																																																																																						alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																						if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																							current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																						if(alert_triggered == true)
																																																																																						{
																																																																																							setTimeout(function(){
																																																																																								while(current_ts <= end)
																																																																																								{
																																																																																									alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																									if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																										current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																									if(alert_triggered == true)
																																																																																									{
																																																																																										setTimeout(function(){
																																																																																											while(current_ts <= end)
																																																																																											{
																																																																																												alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																												if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																													current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																												if(alert_triggered == true)
																																																																																												{
																																																																																													setTimeout(function(){
																																																																																														while(current_ts <= end)
																																																																																														{
																																																																																															alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																															if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																															if(alert_triggered == true)
																																																																																															{
																																																																																																setTimeout(function(){
																																																																																																	while(current_ts <= end)
																																																																																																	{
																																																																																																		alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																																		if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																			current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																		if(alert_triggered == true)
																																																																																																		{
																																																																																																			setTimeout(function(){
																																																																																																				while(current_ts <= end)
																																																																																																				{
																																																																																																					alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																																					if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																						current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																					if(alert_triggered == true)
																																																																																																					{
																																																																																																						setTimeout(function(){
																																																																																																							while(current_ts <= end)
																																																																																																							{
																																																																																																								alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																																								if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																									current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																								if(alert_triggered == true)
																																																																																																								{
																																																																																																									setTimeout(function(){
																																																																																																										while(current_ts <= end)
																																																																																																										{
																																																																																																											alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																																											if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																												current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																											if(alert_triggered == true)
																																																																																																											{
																																																																																																												setTimeout(function(){
																																																																																																													while(current_ts <= end)
																																																																																																													{
																																																																																																														alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																																														if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																															current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																														if(alert_triggered == true)
																																																																																																														{
																																																																																																															setTimeout(function(){
																																																																																																																while(current_ts <= end)
																																																																																																																{
																																																																																																																	alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																																																	if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																																		current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																																	if(alert_triggered == true)
																																																																																																																	{
																																																																																																																		setTimeout(function(){
																																																																																																																			while(current_ts <= end)
																																																																																																																			{
																																																																																																																				alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																																																				if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																																					current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																																				if(alert_triggered == true)
																																																																																																																				{
																																																																																																																					setTimeout(function(){
																																																																																																																						while(current_ts <= end)
																																																																																																																						{
																																																																																																																							alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_maw_input').val());
																																																																																																																							if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																																								current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																																							if(alert_triggered == true)
																																																																																																																							{
																																																																																																																								setTimeout(function(){
																																																																																																																									alert("demo alert limit reached");
																																																																																																																									},1000);
																																																																																																																								break;
																																																																																																																							}	
																																																																																																																							current_ts++;
																																																																																																																							x++;
																																																																																																																						}	
																																																																																																																						},1000);
																																																																																																																					break;
																																																																																																																				}	
																																																																																																																				current_ts++;
																																																																																																																				x++;
																																																																																																																			}	
																																																																																																																			},1000);
																																																																																																																		break;
																																																																																																																	}	
																																																																																																																	current_ts++;
																																																																																																																	x++;
																																																																																																																}	
																																																																																																																},1000);
																																																																																																															break;
																																																																																																														}	
																																																																																																														current_ts++;
																																																																																																														x++;
																																																																																																													}	
																																																																																																													},1000);
																																																																																																												break;
																																																																																																											}	
																																																																																																											current_ts++;
																																																																																																											x++;
																																																																																																										}	
																																																																																																										},1000);
																																																																																																									break;
																																																																																																								}	
																																																																																																								current_ts++;
																																																																																																								x++;
																																																																																																							}	
																																																																																																							},1000);
																																																																																																						break;
																																																																																																					}	
																																																																																																					current_ts++;
																																																																																																					x++;
																																																																																																				}	
																																																																																																				},1000);
																																																																																																			break;
																																																																																																		}	
																																																																																																		current_ts++;
																																																																																																		x++;
																																																																																																	}	
																																																																																																	},1000);
																																																																																																break;
																																																																																															}	
																																																																																															current_ts++;
																																																																																															x++;
																																																																																														}	
																																																																																														},1000);
																																																																																													break;
																																																																																												}	
																																																																																												current_ts++;
																																																																																												x++;
																																																																																											}	
																																																																																											},1000);
																																																																																										break;
																																																																																									}	
																																																																																									current_ts++;
																																																																																									x++;
																																																																																								}	
																																																																																								},1000);
																																																																																							break;
																																																																																						}	
																																																																																						current_ts++;
																																																																																						x++;
																																																																																					}	
																																																																																					},1000);
																																																																																				break;
																																																																																			}	
																																																																																			current_ts++;
																																																																																			x++;
																																																																																		}	
																																																																																		},1000);
																																																																																	break;
																																																																																}	
																																																																																current_ts++;
																																																																																x++;
																																																																															}	
																																																																															},1000);
																																																																														break;
																																																																													}	
																																																																													current_ts++;
																																																																													x++;
																																																																												}	
																																																																												},1000);
																																																																											break;
																																																																										}	
																																																																										current_ts++;
																																																																										x++;
																																																																									}	
																																																																									},1000);
																																																																								break;
																																																																							}	
																																																																							current_ts++;
																																																																							x++;
																																																																						}	
																																																																						},1000);
																																																																					break;
																																																																				}	
																																																																				current_ts++;
																																																																				x++;
																																																																			}	
																																																																			},1000);
																																																																		break;
																																																																	}	
																																																																	current_ts++;
																																																																	x++;
																																																																}	
																																																																},1000);
																																																															break;
																																																														}	
																																																														current_ts++;
																																																														x++;
																																																													}	
																																																													},1000);
																																																												break;
																																																											}	
																																																											current_ts++;
																																																											x++;
																																																										}	
																																																										},1000);
																																																									break;
																																																								}	
																																																								current_ts++;
																																																								x++;
																																																							}	
																																																							},1000);
																																																						break;
																																																					}	
																																																					current_ts++;
																																																					x++;
																																																				}	
																																																				},1000);
																																																			break;
																																																		}	
																																																		current_ts++;
																																																		x++;
																																																	}	
																																																	},1000);
																																																break;
																																															}	
																																															current_ts++;
																																															x++;
																																														}	
																																														},1000);
																																													break;
																																												}	
																																												current_ts++;
																																												x++;
																																											}	
																																											},1000);
																																										break;
																																									}	
																																									current_ts++;
																																									x++;
																																								}	
																																								},1000);
																																							break;
																																						}	
																																						current_ts++;
																																						x++;
																																					}	
																																					},1000);
																																				break;
																																			}	
																																			current_ts++;
																																			x++;
																																		}	
																																		},1000);
																																	break;
																																}	
																																current_ts++;
																																x++;
																															}	
																															},1000);
																														break;
																													}	
																													current_ts++;
																													x++;
																												}	
																												},1000);
																											break;
																										}	
																										current_ts++;
																										x++;
																									}	
																									},1000);
																								break;
																							}	
																							current_ts++;
																							x++;
																						}	
																						},1000);
																					break;
																				}	
																				current_ts++;
																				x++;
																			}	
																			},1000);
																		break;
																	}	
																	current_ts++;
																	x++;
																}	
																},1000);
															break;
														}	
														current_ts++;
														x++;
													}	
													},1000);
												break;
											}	
											current_ts++;
											x++;
										}	
										},1000);
									break;
								}	
								current_ts++;
								x++;
							}	
							},1000);
						break;
					}	
					current_ts++;
					x++;
				}	
			});
});

function simulateNewFrame(ts, ma_modifier, single_modifier, awp, maw)
{
	var alert_triggered = false;
	$("#alerts_div").html(ts);
	$.ajax({
			type: 'GET',
			url: "http://hoozontv.elasticbeanstalk.com/endpoint",
			data: {
	            method: "simulateNewFrame",
	            ts: ts,
	            ma_modifier: ma_modifier,
	            single_modifier: single_modifier,
	            alert_waiting_period: awp,
	            moving_average_window: maw
			},
	        dataType: 'json',
	        async: false,
	        success: function (data, status) {
	        	if (data.response_status == "error")
	        	{
	        		//$("#results_div").html("error: "  + data.message);
	        		if(data.next_frame)
	        		{ 
	        			//alert("next_frame " + data.next_frame);
	        			alert_triggered = data.next_frame;
	        		}	
	        	}
	        	else
	        	{
	        		if(data.frames.length > 100000)
	        		{
	        			alert("too many results");
	        			$("#results_div").html("too many results. Try again.");
	        		}	
	        		else
	        		{
	        			/*var max_score = 0;
	        			var max_designation = "";
	        			for(var c = 0; c < data.designation_averages.length; c++)
	        			{
	        				if(data.designation_averages[c].average > max_score)
	        				{
	        					max_score = data.designation_averages[c].average;
	        					max_designation = data.designation_averages[c].designation;
	        				}
	        			}	
	        			for(var x = 0; x < data.frames.length; x++)
	        			{
	        				d = new Date(data.frames[x].timestamp_in_seconds *1000);
	        				rds = rds + "<div style=\"border: 1px black solid;width:200px;display:inline-block;\">";
        					rds = rds + "<img src=\"http://192.168.2.101/hoozon_wkyt/" + data.frames[data.frames.length -1].image_name + "\" style=\"width:200px;height:113px\">";
        					rds = rds + "<br>" + d.toString();
        					rds = rds + "</div>";
	        			}
	        			rds = rds + "<div style=\"border: 1px black solid;width:200px;display:inline-block;\">";
	        			rds = rds + "<br>max designation for this frame:" + max_designation;
    					rds = rds + "<br>max designation score for this frame:" + max_score;
    					rds = rds + "<br>alert_triggered:" + data.alert_triggered;
    					rds = rds + "</div>";
	        			$("#results_div").html(rds);*/
	        			if(data.alert_triggered === "yes")
	        			{
	        				//alert("alert triggered");
	        				var alertstring = "";
	        				d = new Date(data.frames[data.frames.length -1].timestamp_in_seconds *1000);
	        				alertstring = alertstring + "<div style=\"border: 1px black solid;width:200px;display:inline-block;\">";
	        				alertstring = alertstring + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:3px\"><tr><td style=\"text-align:right;vertical-align:middle\"><img src=\"images/twitter_logo_30x26.jpg\" style=\"width:30px;height26px;\"></td><td style=\"text-align:left;vertical-align:middle;font-size:20px;font-weight:bold\">Alert fired!</td></tr></table>";
	        				alertstring = alertstring + "<br><img src=\"http://192.168.2.101/hoozon_wkyt/" + data.frames[data.frames.length -1].image_name + "\" style=\"width:200px;height:113px\">";
	        				alertstring = alertstring + "<br>" + d.format();
	        				alertstring = alertstring + "<br>des:" + data.designation;
	        				alertstring = alertstring + "<br>frame score:" + data.score;
	        				alertstring = alertstring + "<br>ma score:" + data.moving_average;
	        				alertstring = alertstring + "<br>des h-score:" + data.homogeneity_score;
	        				alertstring = alertstring + "<br>des ma thres:" + data.ma_threshold;
	        				alertstring = alertstring + "<br>des single thresh:" + data.single_threshold;
	        				alertstring = alertstring + "<br>alert_triggered:" + data.alert_triggered;
	        				alertstring = alertstring + "</div>";
	        				$("#results_div").append(alertstring);
	        				alert_triggered = true;
	        			}	
	        		}
	        	}
	        }
	        ,
	        error: function (XMLHttpRequest, textStatus, errorThrown) {
	        	$("#results_div").html("ajax error");
	            console.log(textStatus, errorThrown);
	        }
		});
	return alert_triggered;
}

function resetAllLastAlerts(station)
{
	$.ajax({
			type: 'GET',
			url: "http://hoozontv.elasticbeanstalk.com/endpoint",
			data: {
	            method: "resetAllLastAlerts",
	            station: station
			},
	        dataType: 'json',
	        async: false,
	        success: function (data, status) {
	        	if (data.response_status == "error")
	        		$("#results_div").html("error: "  + data.message);
	        	else
	        	{
	        		//$("#results_div").html("success resetting all last alerts");
	        	}
	        }
	        ,
	        error: function (XMLHttpRequest, textStatus, errorThrown) {
	        	$("#results_div").html("ajax error");
	            console.log(textStatus, errorThrown);
	        }
		});
	return;
}

