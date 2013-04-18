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

var endpoint = "https://www.hoozon.tv/endpoint";
//var endpoint = "http://localhost:8080/hoozontv/endpoint";


var docCookies = {
		  getItem: function (sKey) {
		    if (!sKey || !this.hasItem(sKey)) { return null; }
		    return unescape(document.cookie.replace(new RegExp("(?:^|.*;\\s*)" + escape(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=\\s*((?:[^;](?!;))*[^;]?).*"), "$1"));
		  },
		  setItem: function (sKey, sValue, vEnd, sPath, sDomain, bSecure) {
		    if (!sKey || /^(?:expires|max\-age|path|domain|secure)$/i.test(sKey)) { return; }
		    var sExpires = "";
		    if (vEnd) {
		      switch (vEnd.constructor) {
		        case Number:
		          sExpires = vEnd === Infinity ? "; expires=Tue, 19 Jan 2038 03:14:07 GMT" : "; max-age=" + vEnd;
		          break;
		        case String:
		          sExpires = "; expires=" + vEnd;
		          break;
		        case Date:
		          sExpires = "; expires=" + vEnd.toGMTString();
		          break;
		      }
		    }
		    document.cookie = escape(sKey) + "=" + escape(sValue) + sExpires + (sDomain ? "; domain=" + sDomain : "") + (sPath ? "; path=" + sPath : "") + (bSecure ? "; secure" : "");
		  },
		  removeItem: function (sKey, sPath) {
		    if (!sKey || !this.hasItem(sKey)) { return; }
		    document.cookie = escape(sKey) + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT" + (sPath ? "; path=" + sPath : "");
		  },
		  hasItem: function (sKey) {
		    return (new RegExp("(?:^|;\\s*)" + escape(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=")).test(document.cookie);
		  }
		};


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
	
	var hoozon_admin_auth = docCookies.getItem("hoozon_admin_auth");
	if(location.protocol !== "https:")
	{
		$("#main_div").html("<span style=\"font-size:16;color:red\">This page must be accessed securely. Please visit <a href=\"https://www.hoozon.tv/simulator.html\">https://www.hoozon.tv/simulator.html</a> instead.</span>");
	}
	else if(typeof hoozon_admin_auth === undefined || hoozon_admin_auth === null || hoozon_admin_auth === "")
	{
		var mds = "<input type=password id=\"hoozon_admin_auth_input\"> <input type=button id=\"hoozon_admin_auth_go_button\" value=\"go\">";
		$("#main_div").html(mds);
		$("#hoozon_admin_auth_go_button").click(function () {
				docCookies.setItem("hoozon_admin_auth", $("#hoozon_admin_auth_input").val(), 31536e3);
				window.location.reload();
				return false;
				}
			);
	}
	else
	{
		var designations = null;
		$.ajax({
			type: 'GET',
			url: endpoint,
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
		mds = mds + "						<b>Function 1:</b> Get all still frames in a range (inclusive).";
		mds = mds + "					</td>";
		mds = mds + "				</tr>";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Begin: <input type=\"text\" id=\"function1_begin_input\" size=13 value=\"20130409_050000\"> ";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						End: <input type=\"text\" id=\"function1_end_input\" value=\"20130409_060000\" size=13> ";
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
		mds = mds + "						<b>Function 2:</b> Get all frames for designation above auto-generated homogeneity threshold (inclusive)";
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
		mds = mds + "						Single modifier: <input type=\"text\" id=\"function2_singlemodifier_input\" value=\"1\" size=4>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Delta: <input type=\"text\" id=\"function2_delta_input\" value=\".1\" size=4> ";
		mds = mds + "					</td>";
		mds = mds + "				</tr>";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Begin: <input type=\"text\" id=\"function2_begin_input\" size=13 value=\"20130409_050000\"> ";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						End: <input type=\"text\" id=\"function2_end_input\" value=\"20130409_060000\" size=13> ";
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
		mds = mds + "						<b>Function 3:</b> Graph one designee over time (inclusive)";
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
		mds = mds + "					</td>";
		mds = mds + "				</tr>";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Moving Avg Window: <input type=\"text\" id=\"function3_mawindow_input\" value=\"4\" size=4>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Begin: <input type=\"text\" id=\"function3_begin_input\" size=13 value=\"20130409_050000\">";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						End: <input type=\"text\" id=\"function3_end_input\" value=\"20130409_060000\" size=13>";
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
		mds = mds + "						<b>Function 4:</b> Simulate alerts for a given time range (inclusive)";
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
		mds = mds + "						Moving Avg Window: <input type=\"text\" id=\"function4_mawindow_input\" value=\"4\" size=4>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Alert waiting period: <input type=\"text\" id=\"function4_awp_input\" value=\"7200\" size=4>";
		mds = mds + "					</td>";
		mds = mds + "				</tr>";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Begin: <input type=\"text\" id=\"function4_begin_input\" size=13 value=\"20130409_050000\"><br>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						End: <input type=\"text\" id=\"function4_end_input\" value=\"20130409_060000\" size=13><br>";
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
		mds = mds + "	<tr>";
		mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
		mds = mds + "			<table style=\"border-spacing:3px\">";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=3>";
		mds = mds + "						<b>Function 5:</b> Get list of missing frames (inclusive)";
		mds = mds + "					</td>";
		mds = mds + "				</tr>";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Begin: <input type=\"text\" id=\"function5_begin_input\" size=13 value=\"20130409_050000\">";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						End: <input type=\"text\" id=\"function5_end_input\" value=\"20130409_060000\" size=13>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "   					<input id=\"function5_go_button\" type=button value=\"GO\">";
		mds = mds + "					</td>";
		mds = mds + "				</tr>";
		mds = mds + "			</table>";
		mds = mds + "		</td>";
		mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
		mds = mds + "			<table style=\"border-spacing:3px\">";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=4>";
		mds = mds + "						<b>Function 6:</b> Get alerts for a given timeframe (inclusive)";
		mds = mds + "					</td>";
		mds = mds + "				</tr>";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Single Thresh Modifier: <input type=\"text\" id=\"function6_singlemodifier_input\" value=\"1.0\" size=4>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						MA Thresh Modifier: <input type=\"text\" id=\"function6_mamodifier_input\" value=\".67\" size=4>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Moving Avg Window: <input type=\"text\" id=\"function6_mawindow_input\" value=\"4\" size=4>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Alert waiting period: <input type=\"text\" id=\"function6_awp_input\" value=\"7200\" size=4>";
		mds = mds + "					</td>";
		mds = mds + "				</tr>";
		mds = mds + "				<tr>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Delta: <input type=\"text\" id=\"function6_delta_input\" value=\".1\" size=4> ";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						Begin: <input type=\"text\" id=\"function6_begin_input\" size=13 value=\"20130409_050000\"><br>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "						End: <input type=\"text\" id=\"function6_end_input\" value=\"20130409_060000\" size=13><br>";
		mds = mds + "					</td>";
		mds = mds + "					<td style=\"vertical-align:middle;text-align:left\">";
		mds = mds + "   					<input id=\"function6_go_button\" type=button value=\"GO\">";
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
							url: endpoint,
							data: {
					            method: "getFrames",
					            begin: begin,             
					            end: end,
					            hoozon_admin_auth: hoozon_admin_auth
							},
					        dataType: 'json',
					        async: true,
					        success: function (data, status) {
					        	if (data.response_status == "error")
					        		$("#results_div").html("error message=" + data.message);
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
					        				rds = rds + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
					        				rds = rds + "<img src=http://192.168.2.101/hoozon_wkyt/" + data.frames[x].image_name + " style=\"width:250px;height:141px\">";
					        				rds = rds + "<br>" + data.frames[x].datestring;
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
							url: endpoint,
							data: {
					            method: "getFramesByDesignationAndHomogeneityThreshold",
					            begin: begin,             
					            end: end,
					            designation: $('#function2_designation_select').val(),
					            singlemodifier: $('#function2_singlemodifier_input').val(),
					            delta: $('#function2_delta_input').val(),
					            hoozon_admin_auth: hoozon_admin_auth
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
					        					rds = rds + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
					        					rds = rds + "<img src=\"http://192.168.2.101/hoozon_wkyt/" + data.frames[x].image_name + "\" style=\"width:250px;height:141px\">";
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
					var datestring = $('#function3_begin_input').val();
					var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
					var begin = d.getTime()/1000;
					datestring = $('#function3_end_input').val();
					d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
					var end = d.getTime()/1000;
					
					$.ajax({
							type: 'GET',
							url: endpoint,
							data: {
					            method: "getFramesByDesignation",
					            begin: begin,             
					            end: end,
					            designation: $('#function3_designation_select').val(),
					            mamodifier: $('#function3_mamodifier_input').val(),
					            singlemodifier: $('#function3_singlemodifier_input').val(),
					            mawindow: $('#function3_mawindow_input').val(),
					            hoozon_admin_auth: hoozon_admin_auth
							},
					        dataType: 'json',
					        async: true,
					        success: function (data, status) {
					        	if (data.response_status == "error")
					        		$("#results_div").html("error");
					        	else if (data.response_status == "success")
					        	{
					        		//alert("success");
					        		if(data.frames.length > 100000)
					        		{
					        			$("#results_div").html("too many results. Try again.");
					        		}	
					        		else
					        		{
					        		
					        			var scores = []; var moving_avg = [];
					        			for(var x = 0; x < data.frames.length; x++)
					        			{
					        				scores.push(data.frames[x].designation_score);
					        				moving_avg.push(data.frames[x].moving_average);
					        			}
					        			var plot1 = $.jqplot ('chart1', [scores, moving_avg],{
					        				axes: {
					        					yaxis: {
					        			            min:0,max:1
					        			        }
					        				},
					        				canvasOverlay: {
					        					show: true,
					        			        objects: [
					        			                  {horizontalLine: {
								        			            name: 'pebbles',
								        			            y: data.frames[0].homogeneity_score * $('#function3_singlemodifier_input').val(),
								        			            lineWidth: 3,
								        			            color: 'rgb(100, 55, 124)',
								        			            shadow: true,
								        			            lineCap: 'butt',
								        			            xOffset: 0
								        			          }},  
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
						//alert("simulating new frame with current_ts=" + current_ts + " end=" + end);
						alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
						if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
							current_ts = alert_triggered; // alert_triggered is the next valid frame
						if(alert_triggered == true)
						{
							setTimeout(function(){
								while(current_ts <= end)
								{
									//alert("simulating new frame with current_ts=" + current_ts + " end=" + end);
									alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
									if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
										current_ts = alert_triggered; // alert_triggered is the next valid frame
									if(alert_triggered == true)
									{
										setTimeout(function(){
											while(current_ts <= end)
											{
												//alert("simulating new frame with current_ts=" + current_ts + " end=" + end);
												alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
												if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
													current_ts = alert_triggered; // alert_triggered is the next valid frame
												if(alert_triggered == true)
												{
													setTimeout(function(){
														while(current_ts <= end)
														{
															//alert("simulating new frame with current_ts=" + current_ts + " end=" + end);
															alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
															if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																current_ts = alert_triggered; // alert_triggered is the next valid frame
															if(alert_triggered == true)
															{
																setTimeout(function(){
																	while(current_ts <= end)
																	{
																		//alert("simulating new frame with current_ts=" + current_ts + " end=" + end);
																		alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																		if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																			current_ts = alert_triggered; // alert_triggered is the next valid frame
																		if(alert_triggered == true)
																		{
																			setTimeout(function(){
																				while(current_ts <= end)
																				{
																					//alert("simulating new frame with current_ts=" + current_ts + " end=" + end);
																					alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																					if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																						current_ts = alert_triggered; // alert_triggered is the next valid frame
																					if(alert_triggered == true)
																					{
																						setTimeout(function(){
																							while(current_ts <= end)
																							{
																								alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																								if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																									current_ts = alert_triggered; // alert_triggered is the next valid frame
																								if(alert_triggered == true)
																								{
																									setTimeout(function(){
																										while(current_ts <= end)
																										{
																											alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																											if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																												current_ts = alert_triggered; // alert_triggered is the next valid frame
																											if(alert_triggered == true)
																											{
																												setTimeout(function(){
																													while(current_ts <= end)
																													{
																														alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																														if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																															current_ts = alert_triggered; // alert_triggered is the next valid frame
																														if(alert_triggered == true)
																														{
																															setTimeout(function(){
																																while(current_ts <= end)
																																{
																																	alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																	if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																		current_ts = alert_triggered; // alert_triggered is the next valid frame
																																	if(alert_triggered == true)
																																	{
																																		setTimeout(function(){
																																			while(current_ts <= end)
																																			{
																																				alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																				if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																					current_ts = alert_triggered; // alert_triggered is the next valid frame
																																				if(alert_triggered == true)
																																				{
																																					setTimeout(function(){
																																						while(current_ts <= end)
																																						{
																																							alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																							if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																								current_ts = alert_triggered; // alert_triggered is the next valid frame
																																							if(alert_triggered == true)
																																							{
																																								setTimeout(function(){
																																									while(current_ts <= end)
																																									{
																																										alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																										if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																											current_ts = alert_triggered; // alert_triggered is the next valid frame
																																										if(alert_triggered == true)
																																										{
																																											setTimeout(function(){
																																												while(current_ts <= end)
																																												{
																																													alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																													if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																														current_ts = alert_triggered; // alert_triggered is the next valid frame
																																													if(alert_triggered == true)
																																													{
																																														setTimeout(function(){
																																															while(current_ts <= end)
																																															{
																																																alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																	current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																if(alert_triggered == true)
																																																{
																																																	setTimeout(function(){
																																																		while(current_ts <= end)
																																																		{
																																																			alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																			if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																				current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																			if(alert_triggered == true)
																																																			{
																																																				setTimeout(function(){
																																																					while(current_ts <= end)
																																																					{
																																																						alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																						if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																							current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																						if(alert_triggered == true)
																																																						{
																																																							setTimeout(function(){
																																																								while(current_ts <= end)
																																																								{
																																																									alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																									if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																										current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																									if(alert_triggered == true)
																																																									{
																																																										setTimeout(function(){
																																																											while(current_ts <= end)
																																																											{
																																																												alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																												if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																													current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																												if(alert_triggered == true)
																																																												{
																																																													setTimeout(function(){
																																																														while(current_ts <= end)
																																																														{
																																																															alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																															if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																															if(alert_triggered == true)
																																																															{
																																																																setTimeout(function(){
																																																																	while(current_ts <= end)
																																																																	{
																																																																		alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																		if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																			current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																		if(alert_triggered == true)
																																																																		{
																																																																			setTimeout(function(){
																																																																				while(current_ts <= end)
																																																																				{
																																																																					alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																					if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																						current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																					if(alert_triggered == true)
																																																																					{
																																																																						setTimeout(function(){
																																																																							while(current_ts <= end)
																																																																							{
																																																																								alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																								if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																									current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																								if(alert_triggered == true)
																																																																								{
																																																																									setTimeout(function(){
																																																																										while(current_ts <= end)
																																																																										{
																																																																											alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																											if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																												current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																											if(alert_triggered == true)
																																																																											{
																																																																												setTimeout(function(){
																																																																													while(current_ts <= end)
																																																																													{
																																																																														alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																														if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																															current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																														if(alert_triggered == true)
																																																																														{
																																																																															setTimeout(function(){
																																																																																while(current_ts <= end)
																																																																																{
																																																																																	alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																	if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																		current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																	if(alert_triggered == true)
																																																																																	{
																																																																																		setTimeout(function(){
																																																																																			while(current_ts <= end)
																																																																																			{
																																																																																				alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																				if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																					current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																				if(alert_triggered == true)
																																																																																				{
																																																																																					setTimeout(function(){
																																																																																						while(current_ts <= end)
																																																																																						{
																																																																																							alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																							if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																								current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																							if(alert_triggered == true)
																																																																																							{
																																																																																								setTimeout(function(){
																																																																																									while(current_ts <= end)
																																																																																									{
																																																																																										alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																										if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																											current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																										if(alert_triggered == true)
																																																																																										{
																																																																																											setTimeout(function(){
																																																																																												while(current_ts <= end)
																																																																																												{
																																																																																													alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																													if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																														current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																													if(alert_triggered == true)
																																																																																													{
																																																																																														setTimeout(function(){
																																																																																															while(current_ts <= end)
																																																																																															{
																																																																																																alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																																if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																	current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																if(alert_triggered == true)
																																																																																																{
																																																																																																	setTimeout(function(){
																																																																																																		while(current_ts <= end)
																																																																																																		{
																																																																																																			alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																																			if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																				current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																			if(alert_triggered == true)
																																																																																																			{
																																																																																																				setTimeout(function(){
																																																																																																					while(current_ts <= end)
																																																																																																					{
																																																																																																						alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																																						if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																							current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																						if(alert_triggered == true)
																																																																																																						{
																																																																																																							setTimeout(function(){
																																																																																																								while(current_ts <= end)
																																																																																																								{
																																																																																																									alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																																									if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																										current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																									if(alert_triggered == true)
																																																																																																									{
																																																																																																										setTimeout(function(){
																																																																																																											while(current_ts <= end)
																																																																																																											{
																																																																																																												alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																																												if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																													current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																												if(alert_triggered == true)
																																																																																																												{
																																																																																																													setTimeout(function(){
																																																																																																														while(current_ts <= end)
																																																																																																														{
																																																																																																															alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																																															if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																																current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																															if(alert_triggered == true)
																																																																																																															{
																																																																																																																setTimeout(function(){
																																																																																																																	while(current_ts <= end)
																																																																																																																	{
																																																																																																																		alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																																																		if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																																			current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																																		if(alert_triggered == true)
																																																																																																																		{
																																																																																																																			setTimeout(function(){
																																																																																																																				while(current_ts <= end)
																																																																																																																				{
																																																																																																																					alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
																																																																																																																					if(alert_triggered != true && alert_triggered != false && alert_triggered != -1) // this means that it's a next_frame value and not null
																																																																																																																						current_ts = alert_triggered; // alert_triggered is the next valid frame
																																																																																																																					if(alert_triggered == true)
																																																																																																																					{
																																																																																																																						setTimeout(function(){
																																																																																																																							while(current_ts <= end)
																																																																																																																							{
																																																																																																																								alert_triggered = simulateNewFrame(current_ts,  $('#function4_mamodifier_input').val(), $('#function4_singlemodifier_input').val(), $('#function4_awp_input').val(), $('#function4_mawindow_input').val());
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
		
		$("#function5_go_button").click(
				function () {
					$("#results_div").html("");
					$("#chart1").html("");
					var rds = "";
					var datestring = $('#function5_begin_input').val();
					var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
					var begin = d.getTime()/1000;
					datestring = $('#function5_end_input').val();
				    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
					var end = d.getTime()/1000;
					$.ajax({
							type: 'GET',
							url: endpoint,
							data: {
					            method: "getMissingFrames",
					            begin: begin,             
					            end: end  ,
					            hoozon_admin_auth: hoozon_admin_auth
							},
					        dataType: 'json',
					        async: false,
					        success: function (data, status) {
					        	if (data.response_status == "error")
					        		$("#results_div").html("error");
					        	else
					        	{
					        		if(data.missing_frames_timestamps.length > 100000)
					        		{
					        			$("#results_div").html("too many results. Try again.");
					        		}	
					        		else
					        		{
					        			
					        			for(var x = 0; x < data.missing_frames_timestamps.length; x++)
					        			{
					        				rds = rds + data.missing_frames_timestamps[x] + " " + data.missing_frames_datestrings[x] + "<br>";
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
					resetAllLastAlerts("wkyt");
					var datestring = $('#function6_begin_input').val();
					var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
					var begin = d.getTime()/1000;
					datestring = $('#function6_end_input').val();
				    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
					var end = d.getTime()/1000;
					
					$.ajax({
							type: 'GET',
							url: endpoint,
							data: {
					            method: "getAlertsForTimePeriod",
					            begin: begin,             
					            end: end,
					            mamodifier: $('#function6_mamodifier_input').val(),
					            singlemodifier: $('#function6_singlemodifier_input').val(),
					            awp: $('#function6_awp_input').val(),
					            mawindow: $('#function6_mawindow_input').val(),
					            delta:  $('#function6_delta_input').val(),
					            hoozon_admin_auth: hoozon_admin_auth
							},
					        dataType: 'json',
					        async: false,
					        success: function (data, status) {
					        	if (data.response_status == "error")
					        		$("#results_div").html("error");
					        	else
					        	{
					        		//alert("response_status=success");
					        		if(data.alert_frames)
				        			{	
					        			data.alert_frames.sort(function(a,b){
					        				a = a.timestamp_in_seconds;
					        				b = b.timestamp_in_seconds;
					        				return a - b;
					        			});
					        			
				        				for(var x = 0; x < data.alert_frames.length; x++)
				        				{
				        					rds = rds + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
					        				rds = rds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:3px\"><tr><td style=\"text-align:right;vertical-align:middle\"><img src=\"images/twitter_logo_30x26.jpg\" style=\"width:30px;height26px;\"></td><td style=\"text-align:left;vertical-align:middle;font-size:20px;font-weight:bold\">Alert fired!</td></tr></table>";
					        				rds = rds + "<br><img src=\"http://192.168.2.101/hoozon_wkyt/" + data.alert_frames[x].image_name + "\" style=\"width:250px;height:141px\">";
					        				rds = rds + "<br>datestring:" + data.alert_frames[x].datestring;
					        				rds = rds + "<br>designation:" + data.alert_frames[x].designation;
					        				rds = rds + "<br>score for des:" + data.alert_frames[x].score;
					        				rds = rds + "<br>ma[0] score:" + data.alert_frames[x].moving_average;
					        				rds = rds + "<br>max_moving_average:" + data.alert_frames[x].max_moving_average;
					        				rds = rds + "<br>mma index:" + data.alert_frames[x].max_moving_average_index;
					        				rds = rds + "<br>des homogeneity:" + data.alert_frames[x].homogeneity_score;
					        				rds = rds + "<br>des single thresh:" + data.alert_frames[x].single_threshold;
					        				rds = rds + "<br>des ma thres:" + data.alert_frames[x].ma_threshold;
					        				rds = rds + "<br>2nd pl des:" + data.alert_frames[x].secondplace_designation;
					        				rds = rds + "<br>2nd pl score:" + data.alert_frames[x].secondplace_score;
					        				rds = rds + "<br>score_of_last frame_in_window:" + data.alert_frames[x].score_of_last_frame_in_window;
					        				rds = rds + "<br>window_index_of_max_score:" + data.alert_frames[x].window_index_of_max_score;
					        				rds = rds + "</div>";
				        				}
				        			}
					        		if(data.delta_suppressed_frames)
					        		{
					        			data.delta_suppressed_frames.sort(function(a,b){
					        				a = a.timestamp_in_seconds;
					        				b = b.timestamp_in_seconds;
					        				return a - b;
					        			});
				        				for(var x = 0; x < data.delta_suppressed_frames.length; x++)
				        				{
				        					rds = rds + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
					        				//rds = rds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:3px\"><tr><td style=\"text-align:right;vertical-align:middle\"><img src=\"images/twitter_logo_30x26.jpg\" style=\"width:30px;height26px;\"></td><td style=\"text-align:left;vertical-align:middle;font-size:20px;font-weight:bold\">Alert fired!</td></tr></table>";
					        				rds = rds + "<br><img src=\"http://192.168.2.101/hoozon_wkyt/" + data.delta_suppressed_frames[x].image_name + "\" style=\"width:250px;height:141px\">";
					        				rds = rds + "<br>datestring:" + data.delta_suppressed_frames[x].datestring;
					        				rds = rds + "<br>designation:" + data.delta_suppressed_frames[x].designation;
					        				rds = rds + "<br>score for des:" + data.delta_suppressed_frames[x].score;
					        				rds = rds + "<br>ma[0] score:" + data.delta_suppressed_frames[x].moving_average;
					        				rds = rds + "<br>max_moving_average:" + data.delta_suppressed_frames[x].max_moving_average;
					        				rds = rds + "<br>mma index:" + data.delta_suppressed_frames[x].max_moving_average_index;
					        				rds = rds + "<br>des homogeneity:" + data.delta_suppressed_frames[x].homogeneity_score;
					        				rds = rds + "<br>des single thresh:" + data.delta_suppressed_frames[x].single_threshold;
					        				rds = rds + "<br>des ma thres:" + data.delta_suppressed_frames[x].ma_threshold;
					        				rds = rds + "<br>2nd pl des:" + data.delta_suppressed_frames[x].secondplace_designation;
					        				rds = rds + "<br>2nd pl score:" + data.delta_suppressed_frames[x].secondplace_score;
					        				rds = rds + "<br>score_of_last frame_in_window:" + data.delta_suppressed_frames[x].score_of_last_frame_in_window;
					        				rds = rds + "<br>window_index_of_max_score:" + data.delta_suppressed_frames[x].window_index_of_max_score;
					        				rds = rds + "</div>";
				        				}
					        		}
				        			/*for(var x = 0; x < data.frames_processed.length; x++)
				        			{
				        				rds = rds + data.frames_processed[x] + "<br>";
				        			}*/
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
				});
	}
	
});

function simulateNewFrame(ts, mamodifier, singlemodifier, awp, mawindow)
{
	var hoozon_admin_auth = docCookies.getItem("hoozon_admin_auth"); // this should always be here since this function can't be called without it. 
	var alert_triggered = false;
	$("#alerts_div").html(ts);
	$.ajax({
			type: 'GET',
			url: endpoint,
			data: {
	            method: "simulateNewFrame",
	            ts: ts,
	            mamodifier: mamodifier,
	            singlemodifier: singlemodifier,
	            awp: awp,
	            mawindow: mawindow,
	            hoozon_admin_auth: hoozon_admin_auth
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
	        		
	        			/*jsonresponse.put("alert_triggered", "yes");
						jsonresponse.put("designation", max_designation);
						jsonresponse.put("designation_moving_average_over_window", max_avg);
						jsonresponse.put("designation_score_for_last_frame_in_window", designation_score_for_last_frame_in_window);
						jsonresponse.put("designation_highest_frame_score_in_window", max_frame_score_for_designation_with_max_average);
						jsonresponse.put("index_of_designation_highest_frame_score_in_window", window_index_of_max_score_for_designation_with_max_average);
						twitter_handle = getTwitterHandle("wkyt",max_designation);
						if(twitter_handle != null)
						{
							jsonresponse.put("designation_twitter_handle", getTwitterHandle("wkyt",max_designation));
							JSONObject twitter_stuff = getUserTwitterAccessTokenAndSecret("wkyt","hoozon_master"); // FIXME
							if(twitter_stuff.has("response_status") && twitter_stuff.getString("response_status").equals("success")
									&& twitter_stuff.has("twitter_access_token") && !twitter_stuff.getString("twitter_access_token").isEmpty()
									&& twitter_stuff.has("twitter_access_token_secret") && !twitter_stuff.getString("twitter_access_token_secret").isEmpty())
							{
								jsonresponse.put("twitter_access_token",twitter_stuff.getString("twitter_access_token"));
								jsonresponse.put("twitter_access_token_secret",twitter_stuff.getString("twitter_access_token_secret"));
								long twitter_alert_id = createAlertInDB("wkyt", "twitter", max_designation ,image_name_for_frame_with_highest_score_across_window); 
								jsonresponse.put("twitter_alert_id", twitter_alert_id);
								//jsonresponse.put("twitter_message_firstperson", getMessage("wkyt", frame_processing_jo.getString("designation"), "twitter", "firstperson", jo.getLong("timestamp_in_seconds")));
							}
						}
						 
						JSONObject facebook_stuff = getSelectedFacebookAccount("wkyt", "hoozon_master"); //FIXME
						if(facebook_stuff != null)
						{
							jsonresponse.put("facebook_account_id",facebook_stuff.getLong("facebook_account_id"));
							jsonresponse.put("facebook_account_access_token",facebook_stuff.getString("facebook_account_access_token"));
							jsonresponse.put("facebook_account_name",facebook_stuff.getString("facebook_account_name"));
							long fb_alert_id = createAlertInDB("wkyt", "facebook", max_designation, image_name_for_frame_with_highest_score_across_window); 
							jsonresponse.put("facebook_alert_id", fb_alert_id);
						}
						
						jsonresponse.put("designation_display_name", getDisplayName("wkyt",max_designation));
						jsonresponse.put("designation_homogeneity_score", max_homogeneity_double);
						jsonresponse.put("designation_moving_average_threshold", max_homogeneity_double * ma_modifier_double);
						jsonresponse.put("designation_single_threshold", max_homogeneity_double * single_modifier_double);
						jsonresponse.put("datestring_of_last_frame_in_window", getDatestringFromTimestampInSeconds(ts_long));
						jsonresponse.put("datestring_of_frame_with_highest_score_in_window", getDatestringFromTimestampInSeconds(timestamp_in_seconds_for_frame_with_highest_score_across_window_for_designation_with_max_average));
						jsonresponse.put("image_name_of_last_frame_in_window", getDatestringFromTimestampInSeconds(ts_long) + ".jpg");
						jsonresponse.put("image_name_of_frame_with_highest_score_in_window", image_name_for_frame_with_highest_score_across_window);
*/
	        			alert("retrieving info for ts:" + ts);
	        			if(data.alert_triggered === "yes")
	        			{
	        				alert("alert triggered");
	        				var alertstring = "";
	        				alertstring = alertstring + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
	        				alertstring = alertstring + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:3px\"><tr><td style=\"text-align:right;vertical-align:middle\"><img src=\"images/twitter_logo_30x26.jpg\" style=\"width:30px;height26px;\"></td><td style=\"text-align:left;vertical-align:middle;font-size:20px;font-weight:bold\">Alert fired!</td></tr></table>";
	        				alertstring = alertstring + "<br><img src=\"http://192.168.2.101/hoozon_wkyt/" + data.image_name_of_frame_with_highest_score_in_window + "\" style=\"width:250px;height:141px\">";
	        				alertstring = alertstring + "<br>datestring_of_frame_with_highest_score_in_window: " + data.datestring_of_frame_with_highest_score_in_window;
	        				alertstring = alertstring + "<br>designation: " + data.designation;
	        				alertstring = alertstring + "<br>designation_moving_average_over_window: " + data.designation_moving_average_over_window;
	        				alertstring = alertstring + "<br>designation_score_for_last_frame_in_window: " + data.designation_score_for_last_frame_in_window;
	        				alertstring = alertstring + "<br>designation_highest_frame_score_in_window: " + data.designation_highest_frame_score_in_window;
	        				alertstring = alertstring + "<br>index_of_designation_highest_frame_score_in_window: " + data.index_of_designation_highest_frame_score_in_window;
	        				if(data.designation_twitter_handle)
	        				{
	        					alertstring = alertstring + "<br>designation_twitter_handle: " + data.designation_twitter_handle;
	        					//alertstring = alertstring + "<br>twitter_access_token:" + data.twitter_access_token;
	        					//alertstring = alertstring + "<br>twitter_access_token_secret:" + data.twitter_access_token_secret;
	        					alertstring = alertstring + "<br>twitter_redirect_id: " + data.twitter_redirect_id;
	        				}
	        				if(data.facebook_account_id)
	        				{
	        					alertstring = alertstring + "<br>facebook_account_id: " + data.facebook_account_id;
	        					//alertstring = alertstring + "<br>facebook_account_access_token:" + data.facebook_account_access_token;
	        					alertstring = alertstring + "<br>facebook_account_name: " + data.facebook_account_name;
	        					alertstring = alertstring + "<br>facebook_redirect_id: " + data.facebook_redirect_id;
	        				}	
	        				alertstring = alertstring + "<br>designation_display_name: " + data.designation_display_name;
	        				alertstring = alertstring + "<br>designation_homogeneity_score: " + data.designation_homogeneity_score;
	        				alertstring = alertstring + "<br>designation_moving_average_threshold: " + data.designation_moving_average_threshold;
	        				alertstring = alertstring + "<br>designation_single_threshold: " + data.designation_single_threshold;
	        				alertstring = alertstring + "<br>datestring_of_last_frame_in_window: " + data.datestring_of_last_frame_in_window;
	        				alertstring = alertstring + "<br>datestring_of_frame_with_highest_score_in_window: " + data.datestring_of_frame_with_highest_score_in_window;
	        				alertstring = alertstring + "<br>image_name_of_last_frame_in_window: " + data.image_name_of_last_frame_in_window;
	        				alertstring = alertstring + "<br>image_name_of_frame_with_highest_score_in_window: " + data.image_name_of_frame_with_highest_score_in_window;
	        				
	        				alertstring = alertstring + "</div>";
	        				$("#results_div").append(alertstring);
	        				alert_triggered = true;
	        			}	
	        			else
	        			{
	        				alert("no alert triggered");
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
	var hoozon_admin_auth = docCookies.getItem("hoozon_admin_auth"); // this should always be here since this function can't be called without it. 
	$.ajax({
			type: 'GET',
			url: endpoint,
			data: {
	            method: "resetAllLastAlerts",
	            station: station,
	            hoozon_admin_auth: hoozon_admin_auth
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

