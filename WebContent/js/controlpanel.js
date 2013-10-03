var endpoint = "https://www.huzon.tv/endpoint";
//var endpoint = "https://localhost:8443/huzontv/endpoint";

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

var devel = true;


//document.addEventListener('DOMContentLoaded', function () {
$(window).load(function () {	
	//alert("window loaded");
	var administering_station = docCookies.getItem("administering_station");
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	var station = ""; 
	if(location.protocol !== "https:")
	{
		//alert("protocol not https");
		$("#message_div").html("<span style=\"font-size:18;color:red\">This page must be accessed securely. Please visit <a href=\"https://www.huzon.tv/controlpanel.html\">https://www.huzon.tv/controlpanel.html</a> instead.</span>");
	}
	else if(twitter_handle === null)
	{
		//alert("twitter handle = null");
		$("#message_div").html("<span style=\"font-size:16;color:red\">Please log in with your Twitter account.</span>");
		$("#main_div").html("<a href=\"#\" id=\"sign_in_with_twitter_link\">Sign in with Twitter</a>");
		$("#sign_in_with_twitter_link").click(
				function (event) {
					var oauth_token = null;
					$("#message_div").html("<span style=\"font-size:16;color:blue\">Twitter authentication started. Please wait. </span>");
					$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "startTwitterAuthentication"
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        	{
				        		$("#message_div").html("<span style=\"font-size:16;color:red\">startTwitterAuthentication error: " + data.message + "</span>");
				        	}
				        	else
				        	{
				        		oauth_token = data.oauth_token;
				        		window.location.href = "https://api.twitter.com/oauth/authenticate?oauth_token=" + oauth_token;
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#message_div").html("<span style=\"font-size:16;color:red\">startTwitterAuthentication ajax error</span>");
				            console.log(textStatus, errorThrown);
				        }
					});
				return false;
				}
			);	
	}	
	else if(twitter_handle !== null)
	{
		if(twitter_access_token === null)
		{
			//alert("twitter_handle !== null but tat === null");
			docCookies.removeItem("twitter_handle");
			window.location.reload();
		}	
		else
		{
			
			$.ajax({
				type: 'GET',
				url: endpoint,
				data: {
		            method: "getStations",
		            twitter_handle: twitter_handle,
		            twitter_access_token: twitter_access_token
				},
		        dataType: 'json',
		        async: false,
		        success: function (data, status) {
		        	if(data.response_status === 'success')
		        	{
		        		/*var mds = "";
		        		mds = mds + "<div style=\"font-size:16;color:green\">Select a station:</div>";
		        		for(var x = 0; x < data.stations_ja.length; x++)
		        		{
		        			mds = mds + "<div style=\"font-size:16\"><a href=\"#\" id=\"" + data.stations_ja[x].call_letters  + "_link\">" + data.stations_ja[x].call_letters  + "</a></div>";
		        		}	
		        		$("#message_div").html(mds);
		        		for(var x = 0; x < data.stations_ja.length; x++)
		        		{
		        			$("#" + data.stations_ja[x].call_letters  + "_link").click({value: data.stations_ja[x].call_letters},
		        					function (event) {
		        						docCookies.setItem("administering_station", event.data.value, 3000000);
		        						window.location.reload();
		        					});
		        		}*/
		        		
		        		var station_select_str = "";
		        		station_select_str = station_select_str + "<select id=\"station_select\">";
		        		for(var x = 0; x < data.stations_ja.length; x++)
		        		{	
		        			station_select_str = station_select_str + "	<option id=\"station_select_" + data.stations_ja[x].call_letters + "\" value=\"" + data.stations_ja[x].call_letters + "\">" + data.stations_ja[x].call_letters + "</option>";
		        		}
		        		station_select_str = station_select_str + "</select> <span id=\"station_select_change_span\"></span>";
		        		$("#station_select_td").html(station_select_str);
		        		
		        		if(administering_station == null)
		        		{
		        			administering_station = "wkyt";
		        			docCookies.setItem("administering_station", "wkyt");
		        		}
		        		
		        		$("#station_select_" + administering_station).prop('selected', true);
			    		$("#station_select").change(function () {
			    			docCookies.setItem("administering_station", $("#station_select").val(), 30000000);
			    			$("#station_select_change_span").html("reloading page");
			    			window.location.reload();
		            	});	
		        	}
		        	else
		        	{
		        		$("#message_div").html("<span style=\"font-size:16;color:red\">Error: message=" + data.message  + "</span>");
		        	}
		        },
		        error: function (XMLHttpRequest, textStatus, errorThrown) {
		        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
		            console.log(textStatus, errorThrown);
		        }
			});
			
			//alert("twitter_handle !== null && tat !== null");
			$.ajax({
				type: 'GET',
				url: endpoint,
				data: {
		            method: "getSelf",
		            twitter_handle: twitter_handle,
		            twitter_access_token: twitter_access_token
				},
		        dataType: 'json',
		        async: true,
		        success: function (data, status) {
		        	if (data.response_status == "error")
		        	{
		        		$("#message_div").html("<span style=\"font-size:16;color:red\">getSelf error: " + data.message + "</span>");
		        		if(data.error_code && data.error_code == "07734") // the twitter cookie credentials were invalid. Delete them and reload page (start over).
		        		{
		        			docCookies.removeItem("twitter_access_token");
		        			docCookies.removeItem("twitter_handle");
		        			window.location.href = "https://www.huzon.tv/registration.html";
		        		}
		        	}
		        	else // getSelf was successful, meaning twitter_handle and twitter_access_token were OK
		        	{
		        		
		        		if(data.user_jo.stations_as_admin_ja.length <= 0)
		        		{
		        			$("#message_div").html("Sorry, you do not have admin privileges.");
		        		}
		        		else
		        		{	
		        		/*	if(data.user_jo.stations_as_admin_ja.length > 1)
			        		{
			        			//$("#message_div").html("Multiple stations as admin. Using administering_station cookie.");
			        			station = docCookies.getItem("administering_station");
			        		}	
		        			else
		        				station = data.user_jo.stations_as_admin_ja[0];*/
		        			
		        			
		        			$("#general_div").html("<a href=\"#\" id=\"gen_info_link\">Load general station information</a>");
		        			
		        			$("#gen_info_link").click(
		        					function() { getStationInformation(twitter_handle, twitter_access_token, administering_station); }
		        				);
		        			
		        			
		        			var numdays = docCookies.getItem("num_days_for_statistics");
		        			if(numdays == null)
		        				numdays = 14;
		        			else
		        				numdays = numdays * 1;
		        			
		        			$("#chartinfo_div").html("<a href=\"#\" id=\"fired_alert_stats_link\">Load and graph fired alert statistics</a>");
		        			$("#fired_alert_stats_link").click(
		        					function() { graphFiredAlertStatistics(numdays, twitter_handle, twitter_access_token, administering_station); }
		        				);
		        			
		        			$("#reporters_div").html("<a href=\"#\" id=\"reporter_info_link\">Load individual reporter information</a>");
		        			$("#reporter_info_link").click(
		        					function() { getActiveReporterDesignations(twitter_handle, twitter_access_token, administering_station); }
		        				);
	    	        		
		        			
	    	        		var d = new Date();
		        			var end = d.getTime();
		        			var endstring = end + "";
		        			var begin = end - (86400000*numdays);
		        			var beginstring = begin + "";
		        			showPieChartOfUltimateDestinations(beginstring, endstring, twitter_handle, twitter_access_token, administering_station);
		        			/*$("#piechart").html("<a href=\"#\" id=\"pie_chart_link\">Load ultimate destination pie chart</a>");
		        			$("#pie_chart_link").click(
		        					function() { showPieChartOfUltimateDestinations(beginstring, endstring, twitter_handle, twitter_access_token, administering_station); }
		        				);*/
		        			
		        			begin =  end - (86400000*2); // 2 days only for retrieving social objects and images
		        			var beginstring = begin + "";
		        			$("#alerts_div").html("<a href=\"#\" id=\"show_fired_alerts_link\">Load fired alerts</a>");
		        			$("#show_fired_alerts_link").click(
		        					function() { getFiredAlerts(beginstring, endstring, twitter_handle, twitter_access_token, administering_station); }
		        				);
	    	        		
		        		}
		        	}
		        },
		        error: function (XMLHttpRequest, textStatus, errorThrown) {
		        	$("#message_div").html("<span style=\"font-size:16;color:red\">getSelf ajax error</span>");
		            console.log(textStatus, errorThrown);
		        }
			});
		}
	}
	//alert("end window loaded");
});

function getStationInformation(twitter_handle, twitter_access_token, station)
{
	var general_string = "";
	$("#general_div").html("Loading general station information <img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\">");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
	        method: "getStation",
	        station: station,
	        twitter_handle: twitter_handle,
	        twitter_access_token: twitter_access_token
		},
	    dataType: 'json',
	    async: true,
	    success: function (data, status) {
	    	if (data.response_status == "error")
	    		general_string = general_string + "<div style=\"font-size:16;color:red;padding-left:15px;\">getStationInformation error: " + data.message + "</div>";
	    	else
	    	{
	    		general_string = general_string + "<div style=\"font-weight:bold;font-size:12px\">STATION INFO</div>";
	    		general_string = general_string + "<b>station:</b> " + data.station_jo.call_letters + " " + data.station_jo.city + ", " + data.station_jo.state;
	    		general_string = general_string + "<br><b>ma window:</b> " + data.station_jo.maw;
	    		general_string = general_string + "<br><b>ma modifier:</b> " + data.station_jo.mamodifier;
	    		general_string = general_string + "<br><b>delta:</b> " + data.station_jo.delta;
	    		general_string = general_string + "<br><b>NRPST:</b> " + data.station_jo.nrpst;
	    		general_string = general_string + "<br><b>frame rate:</b> " + data.station_jo.frame_rate;
	    		general_string = general_string + "<br><b>TW indiv alerts:</b>";
	    		general_string = general_string + "<select id=\"twitter_individual_select\">";
	    		general_string = general_string + "	<option SELECTED id=\"twitter_individual_on_option\" value=\"on\">on</option>";
	    		general_string = general_string + "	<option id=\"twitter_individual_off_option\" value=\"off\">off</option>";
	    		general_string = general_string + "</select> <span id=\"twitter_individual_change_span\"></span>";
	    		general_string = general_string + "<br><b>TW master alerts:</b>";
	    		general_string = general_string + "<select id=\"twitter_master_select\">";
	    		general_string = general_string + "	<option SELECTED id=\"twitter_master_on_option\" value=\"on\">on</option>";
	    		general_string = general_string + "	<option id=\"twitter_master_off_option\" value=\"off\">off</option>";
	    		general_string = general_string + "</select> <span id=\"twitter_master_change_span\"></span>";
	    		general_string = general_string + "<br><b>FB indiv alerts:</b>";
	    		general_string = general_string + "<select id=\"facebook_individual_select\">";
	    		general_string = general_string + "	<option SELECTED id=\"facebook_individual_on_option\" value=\"on\">on</option>";
	    		general_string = general_string + "	<option id=\"facebook_individual_off_option\" value=\"off\">off</option>";
	    		general_string = general_string + "</select> <span id=\"facebook_individual_change_span\"></span>";
	    		general_string = general_string + "<br><b>FB master alerts:</b>";
	    		general_string = general_string + "<select id=\"facebook_master_select\">";
	    		general_string = general_string + "	<option SELECTED id=\"facebook_master_on_option\" value=\"on\">on</option>";
	    		general_string = general_string + "	<option id=\"facebook_master_off_option\" value=\"off\">off</option>";
	    		general_string = general_string + "</select> <span id=\"facebook_master_change_span\"></span>";
	    		$("#general_div").html(general_string);
	    		if(data.station_jo.twitter_active_individual)
	    			$("#twitter_individual_on_option").prop('selected', true);
	    		else
	    			$("#twitter_individual_off_option").prop('selected', true);
	    		if(data.station_jo.twitter_active_master)
	    			$("#twitter_master_on_option").prop('selected', true);
	    		else
	    			$("#twitter_master_off_option").prop('selected', true);
	    		if(data.station_jo.facebook_active_individual)
	    			$("#facebook_individual_on_option").prop('selected', true);
	    		else
	    			$("#facebook_individual_off_option").prop('selected', true);
	    		if(data.station_jo.facebook_active_master)
	    			$("#facebook_master_on_option").prop('selected', true);
	    		else
	    			$("#facebook_master_off_option").prop('selected', true);
	    		$("#twitter_individual_select").change(function () {
					$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "setAlertMode",
				            which: "twitter_active_individual",
				            on_or_off: $("#twitter_individual_select").val(), 
				            station: station,
					        twitter_handle: twitter_handle,
					        twitter_access_token: twitter_access_token
				        },
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status === "error")
				        	{
				        		$("#twitter_individual_change_span").css("color", "red");
				        		$("#twitter_individual_change_span").html("error");
				        	}
				        	else
				        	{
				        		$("#twitter_individual_change_span").css("color", "green");
				        		$("#twitter_individual_change_span").html("updated");
				        	}
				        	var int=self.setInterval(function(){$("#twitter_individual_change_span").html("");},5000); // clear it out after 5 seconds
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#twitter_individual_change_span").html("ajax error");
				        	$("#twitter_individual_change_span").css("color", "red");
				            console.log(textStatus, errorThrown);
				        }
					});
            	});	
	    		$("#twitter_master_select").change(function () {
					$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "setAlertMode",
				            which: "twitter_active_master",
				            on_or_off: $("#twitter_master_select").val(), 
				            station: station,
					        twitter_handle: twitter_handle,
					        twitter_access_token: twitter_access_token
				        },
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status === "error")
				        	{
				        		$("#twitter_master_change_span").css("color", "red");
				        		$("#twitter_master_change_span").html("error");
				        	}
				        	else
				        	{
				        		$("#twitter_master_change_span").css("color", "green");
				        		$("#twitter_master_change_span").html("updated");
				        	}
				        	var int=self.setInterval(function(){$("#twitter_master_change_span").html("");},5000); // clear it out after 5 seconds
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#twitter_master_change_span").html("ajax error");
				        	$("#twitter_master_change_span").css("color", "red");
				            console.log(textStatus, errorThrown);
				        }
					});
            	});	
	    		$("#facebook_individual_select").change(function () {
					$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "setAlertMode",
				            which: "facebook_active_individual",
				            on_or_off: $("#facebook_individual_select").val(), 
				            station: station,
					        twitter_handle: twitter_handle,
					        twitter_access_token: twitter_access_token
				        },
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status === "error")
				        	{
				        		$("#facebook_individual_change_span").css("color", "red");
				        		$("#facebook_individual_change_span").html("error");
				        	}
				        	else
				        	{
				        		$("#facebook_individual_change_span").css("color", "green");
				        		$("#facebook_individual_change_span").html("updated");
				        	}
				        	var int=self.setInterval(function(){$("#facebook_individual_change_span").html("");},5000); // clear it out after 5 seconds
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#facebook_individual_change_span").html("ajax error");
				        	$("#facebook_individual_change_span").css("color", "red");
				            console.log(textStatus, errorThrown);
				        }
					});
            	});	
	    		$("#facebook_master_select").change(function () {
					$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "setAlertMode",
				            which: "facebook_active_master",
				            on_or_off: $("#facebook_master_select").val(), 
				            station: station,
					        twitter_handle: twitter_handle,
					        twitter_access_token: twitter_access_token
				        },
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status === "error")
				        	{
				        		$("#facebook_master_change_span").css("color", "red");
				        		$("#facebook_master_change_span").html("error");
				        	}
				        	else
				        	{
				        		$("#facebook_master_change_span").css("color", "green");
				        		$("#facebook_master_change_span").html("updated");
				        	}
				        	var int=self.setInterval(function(){$("#facebook_master_change_span").html("");},5000); // clear it out after 5 seconds
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#facebook_master_change_span").html("ajax error");
				        	$("#facebook_master_change_span").css("color", "red");
				            console.log(textStatus, errorThrown);
				        }
					});
            	});	
	    	}
	    }
	    ,
	    error: function (XMLHttpRequest, textStatus, errorThrown) {
	    	$("#results_div").html("ajax error");
	        console.log(textStatus, errorThrown);
	    }
	});
}

function graphFiredAlertStatistics(numdays, twitter_handle, twitter_access_token, station)
{
	var d = new Date();
	var end = d.getTime();
	var endstring = end + "";
	var begin = end - (86400000*numdays);
	var beginstring = begin + "";
	var alert_stats_string = "";
	$("#graph_div").html("Loading fired alert statistics <img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\">");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
	        method: "getFiredAlertStatistics",
	        station: station,
	        begin: beginstring,
	        end: endstring,
	        self_posted_only: true,
	        twitter_handle: twitter_handle,
	        twitter_access_token: twitter_access_token
		},
	    dataType: 'json',
	    async: true,
	    success: function (data, status) {
	    	if (data.response_status == "error")
	    	{
	    		alert_stats_string = "<div style=\"font-size:16;color:red\">getFiredAlertStatistics error: " + data.message + "</div>";
	    		$("#graph_div").html(alert_stats_string);
	    	}
	    	else
	    	{
	    		var fired_alert_counts = []; 
    			//var unabridged_redirect_counts = [];
    			var sansbot_redirect_counts = [];
    			// looping through all frames gathered to graph
    			var max_redirect_count = 0;
    			var xticks = [];
    			var days = [];
    			var total_fired_alerts = 0;
    			var total_sansbot_redirects = 0;
    			for(var x = 0; x < data.fired_alerts_ja.length; x++)
    			{
    				days.push((x+1)+"");
    				xticks.push(data.fired_alerts_ja[x].day+"");
    				fired_alert_counts.push(data.fired_alerts_ja[x].fired_alert_count);
    				sansbot_redirect_counts.push(data.fired_alerts_ja[x].sansbot_redirect_count);
    				//fired_alert_counts.push([data.fired_alerts_ja[x].day, data.fired_alerts_ja[x].fired_alert_count]);
    				//sansbot_redirect_counts.push([data.fired_alerts_ja[x].day, data.fired_alerts_ja[x].sansbot_redirect_count]);
    				//unabridged_redirect_counts.push([data.fired_alerts_ja[x].day, data.fired_alerts_ja[x].unabridged_redirect_count]);
    				if(data.fired_alerts_ja[x].sansbot_redirect_count > max_redirect_count)
    					max_redirect_count = data.fired_alerts_ja[x].sansbot_redirect_count;
    				total_sansbot_redirects = total_sansbot_redirects + data.fired_alerts_ja[x].sansbot_redirect_count;
    				total_fired_alerts = total_fired_alerts + data.fired_alerts_ja[x].fired_alert_count;
    			}
    			
    			var plot1 = $.jqplot ('chart1', [fired_alert_counts, sansbot_redirect_counts],{
    				title: 'Fired Alerts ' + xticks[0] + " - " + xticks[xticks.length-1],
    				//series:[{showMarker:false}],
    				axes: {
    					xaxis:{
    						ticks: days,
  	    		            label:'Days',
  	    		            labelRenderer: $.jqplot.CanvasAxisLabelRenderer
  	    		        },
    					yaxis: {
    						label:'# alerts and clicks',
    						labelRenderer: $.jqplot.CanvasAxisLabelRenderer,
    			            min:0,max: (max_redirect_count + 20) // spacer
    			        }
    				}
    				,
    				canvasOverlay: {
    					show: true
    			        /*objects: [
    			                  {horizontalLine: {
    		    			            name: 'pebbles',
    		    			            y: (reporter_homogeneity),
    		    			            lineWidth: 3,
    		    			            color: 'rgb(100, 55, 124)',
    		    			            shadow: true,
    		    			            lineCap: 'butt',
    		    			            xOffset: 0
    		    			          }},  
    			          {dashedHorizontalLine: {
    			            name: 'bam-bam',
    			            y: (reporter_homogeneity * mamodifier),
    			            lineWidth: 4,
    			            dashPattern: [8, 16],
    			            lineCap: 'round',
    			            xOffset: '25',
    			            color: 'rgb(66, 98, 144)',
    			            shadow: false
    			          }}
    			        ]*/
    			      }
    			    });
    			
    			var chartinfo_str = "<div style=\"font-weight:bold;font-size:12px\">STATISTICS</div>";
     			chartinfo_str = chartinfo_str + "<b>Total alerts:</b> " + total_fired_alerts;
     			chartinfo_str = chartinfo_str + "<br><b>Total human redirects:</b> " + total_sansbot_redirects;
     			chartinfo_str = chartinfo_str + "<br><b>Redirects per alert: </b> " + (Math.floor((total_sansbot_redirects/total_fired_alerts)*10)/10);
     			chartinfo_str = chartinfo_str + "<br><b># days to show:</b> ";
     			chartinfo_str = chartinfo_str + "<select id=\"days_select\">";
     			for(var x = 1; x <= 30; x++)
     			{	
     				chartinfo_str = chartinfo_str + "	<option id=\"days_select_" + x + "\" value=\"" + x + "\">" + x + "</option>";
     			}
 	    		chartinfo_str = chartinfo_str + "</select> <span id=\"days_select_change_span\"></span>";
 	    		
 	    		$("#chartinfo_div").html(chartinfo_str);
 	    		//alert("setting #days_select_" + numdays + " to true");
 	    		$("#days_select_" + numdays).prop('selected', true);
 	    		$("#days_select").change(function () {
 	    			docCookies.setItem("num_days_for_statistics", $("#days_select").val(), 30000000);
 	    			$("#chart1").html("");
 	    			$("#days_select_change_span").html("Reloading...");
 	    			$("#days_select_change_span").css("color", "blue");
 	    			graphFiredAlertStatistics($("#days_select").val(), twitter_handle, twitter_access_token, station);
             	});	
	    	}
	    }
	    ,
	    error: function (XMLHttpRequest, textStatus, errorThrown) {
	    	$("#results_div").html("ajax error");
	        console.log(textStatus, errorThrown);
	    }
	});
}

function getActiveReporterDesignations(twitter_handle, twitter_access_token, station)
{
	var reporters_string = "";
	$("#reporters_div").html("Loading reporter information <img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\">");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
	        method: "getActiveReporterDesignations",
	        station: station,
	        twitter_handle: twitter_handle,
	        twitter_access_token: twitter_access_token
		},
	    dataType: 'json',
	    async: true,
	    success: function (data, status) {
	    	if (data.response_status == "error")
	    		reporters_string = reporters_string + "<div style=\"font-size:16;color:red\">getActiveReporterDesignations error: " + data.message + "</div>";
	    	else
	    	{
	    		var reporters_ja = data.reporters_ja;
				reporters_string = reporters_string + "<table style=\"width:100%\">";
				reporters_string = reporters_string + "	<tr>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td colspan=8 style=\"background-color:#CCFFFF;text-align:center;font-weight:bold\">";
				reporters_string = reporters_string + "			Twitter settings";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td colspan=8 style=\"background-color:#A9CAEB;text-align:center;font-weight:bold\">";
				reporters_string = reporters_string + "			Facebook settings";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "	</tr>";
				reporters_string = reporters_string + "	<tr>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			h.tv designation";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Display name";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Roles";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			handle";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			link";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			on";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			CD";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			followers";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			30d";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			clx 30d";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			clx/alrt";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			link";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			pg link";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			on";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			CD";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			likes";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			30d";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			clx 30d";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			clx/alrt";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "	</tr>";
				var cooldown_in_hours = 0;
				for(var x=0; x < reporters_ja.length; x++)
				{
					var user = getUser(reporters_ja[x]);
					reporters_string = reporters_string + "	<tr>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_designation_td\">" + reporters_ja[x] + "</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_display_name_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_roles_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_handle_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_linked_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_active_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_cooldown_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_followers_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_alert_count_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_click_count_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_click_rate_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_linked_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_page_linked_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_active_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_cooldown_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_likes_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_alert_count_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_click_count_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_click_rate_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "	</tr>";
				}
				reporters_string = reporters_string + "</table>";
				$("#reporters_div").html(reporters_string);
				
				for(var x=0; x < reporters_ja.length; x++)
				{
					getUser(reporters_ja[x]);
				}
	    	}
	    }
	    ,
	    error: function (XMLHttpRequest, textStatus, errorThrown) {
	    	$("#results_div").html("ajax error");
	        console.log(textStatus, errorThrown);
	    }
	});
}

function getUser(designation)
{
	/*
	 * String return_tokens_param = request.getParameter("return_tokens");
									String return_tw_profile_param = request.getParameter("return_tw_profile");
									String return_fb_profile_param = request.getParameter("return_fb_profile");
									String return_fb_page_param = request.getParameter("return_fb_page");
									String return_alerts_param = request.getParameter("return_alerts");
	 */
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "getUser",
            designation: designation,
            return_tokens: true,
            return_tw_profile: true,
            return_fb_profile: true, 
            return_fb_page: true, 
            alert_history_in_hours: 720, // one month (-1 for none)
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: true,
        success: function (data, status) {
        	
        	if (data.response_status === "error")
        	{
        		$("#" + designation + "_display_name_td").html("error");
        	}
        	else if(data.response_status === "success")
        	{
        		$("#" + designation + "_display_name_td").html(data.user_jo.display_name);
        		
        		var rolestring = "";
        		if(data.user_jo.anchor === true)
        			rolestring = rolestring + "			A";
				if(data.user_jo.weather === true)
					rolestring = rolestring + "			W";
				if(data.user_jo.sports === true)
					rolestring = rolestring + "			S";
				if(data.user_jo.reporter === true)
					rolestring = rolestring + "			R";
				$("#" + designation + "_roles_td").html(rolestring);
        		
				$("#" + designation + "_twitter_handle_td").html(data.user_jo.twitter_handle);
        		
				if(data.user_jo.twitter_active)
        			$("#" + designation + "_twitter_active_td").html("yes");
        		else
        			$("#" + designation + "_twitter_active_td").html("no");
        		
        		var twitter_linked = "";
        		if(typeof data.user_jo.twitter_jo === undefined || data.user_jo.twitter_jo == null || (data.user_jo.twitter_jo.response_status != null && data.user_jo.twitter_jo.response_status === "error"))
        			twitter_linked = "<span style=\"color:red\">no</span>";
				else
					twitter_linked = "<span style=\"color:green\">yes</span>";
        		$("#" + designation + "_twitter_linked_td").html(twitter_linked);
        		
        		var twitter_followers = "";
        		if(typeof data.user_jo.twitter_jo === undefined || data.user_jo.twitter_jo == null || (data.user_jo.twitter_jo.response_status != null && data.user_jo.twitter_jo.response_status === "error"))
        			twitter_followers = "<span style=\"color:red\">---</span>";
				else
					twitter_followers = "<span style=\"color:green\">" + data.user_jo.twitter_jo.followers_count + "</span>";
        		$("#" + designation + "_twitter_followers_td").html(twitter_followers);
        		
        		$("#" + designation + "_twitter_cooldown_td").html(Math.round(data.user_jo.twitter_cooldown/360)/10);
        		
        		$("#" + designation + "_twitter_alert_count_td").html(data.user_jo.twitter_alert_history_ja.length);
        		
        		var tw_sansbot_clicks = 0;
        		for(var a = 0; a < data.user_jo.twitter_alert_history_ja.length; a++)
        		{	
        			tw_sansbot_clicks = tw_sansbot_clicks + data.user_jo.twitter_alert_history_ja[a].sansbot_redirect_count;
        		}
        		$("#" + designation + "_twitter_click_count_td").html(tw_sansbot_clicks);
        		var tw_click_rate = 0;
        		if(data.user_jo.twitter_alert_history_ja.length != 0 )
        		{
        			tw_click_rate = tw_sansbot_clicks / data.user_jo.twitter_alert_history_ja.length;
        			tw_click_rate = tw_click_rate * 10;
        			tw_click_rate = Math.floor(tw_click_rate);
        			tw_click_rate = tw_click_rate/10;
        		}
        		$("#" + designation + "_twitter_click_rate_td").html(tw_click_rate);
        		
        		if(data.user_jo.facebook_active)
        			$("#" + designation + "_facebook_active_td").html("yes");
        		else
        			$("#" + designation + "_facebook_active_td").html("no");
        		$("#" + designation + "_facebook_cooldown_td").html(Math.round(data.user_jo.facebook_cooldown/360)/10);
        		
        		var facebook_linked = "";
        		if(typeof data.user_jo.facebook_jo === undefined || data.user_jo.facebook_jo == null || data.user_jo.facebook_jo.error != null)
        			facebook_linked = "<span style=\"color:red\">no</span>";
				else
					facebook_linked = "<span style=\"color:green\">yes</span>";
        		$("#" + designation + "_facebook_linked_td").html(facebook_linked);
        		
        		var facebook_page_linked = "";
        		if(typeof data.user_jo.facebook_page_jo === undefined || data.user_jo.facebook_page_jo == null || data.user_jo.facebook_page_jo.error != null)
        			facebook_page_linked = "<span style=\"color:red\">no</span>";
				else
				{
					//alert('ispub=' + data.user_jo.facebook_page_jo.is_published);
					//alert(JSON.stringify(data.user_jo.facebook_page_jo)); 
					if(typeof data.user_jo.facebook_page_jo.is_published === undefined || data.user_jo.facebook_page_jo.is_published == null || data.user_jo.facebook_page_jo.is_published === "false")
						facebook_page_linked = "<span style=\"color:green\">yes <a href=\"#\" onclick=\"alert('Page is not published!');return false;\" style=\"color:red;font-weight:bold\">NP!</a></span>";
					else
						facebook_page_linked = "<span style=\"color:green\">yes</span>";
				}
        		$("#" + designation + "_facebook_page_linked_td").html(facebook_page_linked);
        	
        		var facebook_likes = "";
        		if(typeof data.user_jo.facebook_page_jo === undefined || data.user_jo.facebook_page_jo == null || typeof data.user_jo.facebook_page_jo.likes === undefined || data.user_jo.facebook_page_jo.likes == null)
        			facebook_likes = "<span style=\"color:red\">---</span>";
				else
					facebook_likes = "<span style=\"color:green\">" + data.user_jo.facebook_page_jo.likes + "</span>";
        		$("#" + designation + "_facebook_likes_td").html(facebook_likes);
        		
        		$("#" + designation + "_facebook_alert_count_td").html(data.user_jo.facebook_alert_history_ja.length);
        		
        		var fb_sansbot_clicks = 0;
        		for(var a = 0; a < data.user_jo.facebook_alert_history_ja.length; a++)
        		{	
        			fb_sansbot_clicks = fb_sansbot_clicks + data.user_jo.facebook_alert_history_ja[a].sansbot_redirect_count;
        		}
        		$("#" + designation + "_facebook_click_count_td").html(fb_sansbot_clicks);
        		var fb_click_rate = 0;
        		if(data.user_jo.facebook_alert_history_ja.length != 0 )
        		{
        			fb_click_rate = fb_sansbot_clicks / data.user_jo.facebook_alert_history_ja.length;
        			fb_click_rate = fb_click_rate * 10;
        			fb_click_rate = Math.floor(fb_click_rate);
        			fb_click_rate = fb_click_rate/10;
        		}
        		$("#" + designation + "_facebook_click_rate_td").html(fb_click_rate);
        	}
        	else
        	{
        		//alert("success/none verifying twitter creds for " + designation);
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#" + designation + "_twitter_valid_td").html("<span style=\"color:red\">AJAX ERROR</span>");
            console.log(textStatus, errorThrown);
        }
	});
}

function showPieChartOfUltimateDestinations(beginstring, endstring, twitter_handle, twitter_access_token, station)
{
	//$("#alerts_div").html("Loading recent alerts... <img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\">");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "getFiredAlerts",
            begin: beginstring,
            end: endstring,
            station: station,
            self_posted_only: true,
            get_social_objects: false,
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: true,
        success: function (data, status) {
        	if (data.response_status == "error")
        	{
        		$("#message_div").html("<span style=\"font-size:16;color:red\">gS Error: " + data.message + "</span>");
        	}
        	else // getMostRecentAlerts was successful, meaning twitter_handle and twitter_access_token were OK
        	{
        		var alerts_ja = data.fired_alerts_ja;
        		
        		var homepage_count = 0;
        		var android_app_count = 0;
        		var iphone_app_count = 0;
        		var livestream_eventual_count = 0;
        		var livestream_immediate_count = 0;
        		var clips_count = 0;
        		var recent_newscasts_count = 0;
        		var blank_count = 0;
        		for(var x = 0; x < alerts_ja.length; x++) // no more than 20
        		{
        			if(alerts_ja[x].ultimate_destination_stats.homepage)
        				homepage_count++;
        			if(alerts_ja[x].ultimate_destination_stats.android_app)
        				android_app_count++;
        			if(alerts_ja[x].ultimate_destination_stats.iphone_app)
        				iphone_app_count++;
        			if(alerts_ja[x].ultimate_destination_stats.livestream_eventual)
        				livestream_eventual_count++;
        			if(alerts_ja[x].ultimate_destination_stats.livestream_immediate)
        				livestream_immediate_count++;
        			if(alerts_ja[x].ultimate_destination_stats.clips)
        				clips_count++;
        			if(alerts_ja[x].ultimate_destination_stats.recent_newscasts)
        				recent_newscasts_count++;
        			if(alerts_ja[x].ultimate_destination_stats[''])
        				blank_count++;
        		}
        		/*alert('homepage=' + homepage_count);
        		alert('android_app=' + android_app_count);
        		alert('iphone_app=' + iphone_app_count);
        		alert('livestream_eventual=' + livestream_eventual_count);
        		alert('livestream_immediate=' + livestream_immediate_count);
        		alert('clips_count=' + clips_count);
        		alert('recent_newscasts_count=' + recent_newscasts_count);
        		alert('blank_count=' + blank_count);*/
        		
        		var data = [
        		            ['livestream immediate', livestream_immediate_count],['livestream eventual', livestream_eventual_count], ['recent newscasts', recent_newscasts_count],
     			     	    ['homepage', homepage_count],['android app', android_app_count], ['iphone app', iphone_app_count], ['blank (exited)', blank_count]
     			     	    
     			     	  ];
     			     	  var plot2 = jQuery.jqplot ('piechart', [data], 
     			     	    { 
     			     		title: "Ultimate destinations",
     			     	      seriesDefaults: {
     			     	        // Make this a pie chart.
     			     	        renderer: jQuery.jqplot.PieRenderer, 
     			     	        rendererOptions: {
     			     	          // Put data labels on the pie slices.
     			     	          // By default, labels show the percentage of the slice.
     			     	          showDataLabels: true
     			     	        }
     			     	      }, 
     			     	      legend: { show:true, location: 'e' }
     			     	    }
     			     	  );
     		
     			
        	  }
         }
	     ,
         error: function (XMLHttpRequest, textStatus, errorThrown) {
         	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
             console.log(textStatus, errorThrown);
         }
 	});
}

function getFiredAlerts(beginstring, endstring, twitter_handle, twitter_access_token, station)
{	
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "getFiredAlerts",
            begin: beginstring,
            end: endstring,
            station: station,
            self_posted_only: true,
            get_social_objects: true,
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: true,
        success: function (data, status) {
        	if (data.response_status == "error")
        	{
        		$("#message_div").html("<span style=\"font-size:16;color:red\">gS Error: " + data.message + "</span>");
        	}
        	else // getMostRecentAlerts was successful, meaning twitter_handle and twitter_access_token were OK
        	{
        		var alerts_ja = data.fired_alerts_ja;
        		
        		var mds = "";
        		mds = mds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:10px\">";
        		for(var x = 0; x < alerts_ja.length &&  x < 20; x++) // no more than 20
        		{	
        			if(x%2 == 0)
        				mds = mds + "	<tr>";
        			mds = mds + "		<td style=\"vertical-align:middle;text-align:center;font-size:10px\">";
        			//mds = mds + JSON.stringify(alerts_ja[x]);
	        		if(alerts_ja[x].social_type === "twitter")
	        		{
	        			if(typeof alerts_ja[x].tweet_jo === "undefined")
	        				mds = mds + "This tweet doesn't exist or has been deleted.";
	        			else if(alerts_ja[x].tweet_jo == null)
	        				mds = mds + "tweet_jo == null";
	        			else if(typeof alerts_ja[x].tweet_jo.entities === "undefined")
	        				mds = mds + "tweet_jo.entities is undefined";
	        			else if(alerts_ja[x].tweet_jo.entities == null)
	        				mds = mds + "tweet_jo.entities == null";
	        			else if(typeof alerts_ja[x].tweet_jo.entities.media[0] === "undefined")
	        				mds = mds + "tweet_jo.entities.media[0] is undefined";
	        			else if(alerts_ja[x].tweet_jo.entities.media[0] == null)
	        				mds = mds + "tweet_jo.entities.media[0] == null";
	        			else if(typeof alerts_ja[x].tweet_jo.entities.media[0].media_url === "undefined")
	        				mds = mds + "tweet_jo.entities.media[0].media_url is undefined";
	        			else if(alerts_ja[x].tweet_jo.entities.media[0].media_url == null)
	        				mds = mds + "tweet_jo.entities.media[0].media_url == null";
	        			else
	        				mds = mds + "			<img src=\"" + alerts_ja[x].tweet_jo.entities.media[0].media_url + "\" style=\"width:300px;height:169px\">";
	        		}
	        		else if(alerts_ja[x].social_type === "facebook")
	        		{
	        			if(typeof alerts_ja[x].fbpost_jo === "undefined")
	        				mds = mds + "This fbpost doesn't exist or has been deleted.";
	        			else if(alerts_ja[x].fbpost_jo == null)
	        				mds = mds + "fbpost_jo == null";
	        			else if(typeof alerts_ja[x].fbpost_jo.source === "undefined")
	        				mds = mds + "This fbpost doesn't exist or has been deleted.";
	        			else if(alerts_ja[x].fbpost_jo.source == null)
	        				mds = mds + "fbpost_jo.source == null";
	        			else
	        			{
	        				mds = mds + "<img src=\"" + alerts_ja[x].fbpost_jo.source + "\" style=\"width:300px;height:169px\">";
	        			}
	        		}
	        		else
	        		{
	        			mds = mds + "			<img src=\"" + alerts_ja[x].image_url + "\" style=\"width:300px;height:169px\">";
	        		}
	        		mds = mds + "		</td>";
	        		mds = mds + "		<td style=\"vertical-align:middle;text-align:center;font-size:14px\">";
	        		mds = mds + "			" + alerts_ja[x].local_timestamp_hr;
	        		mds = mds + "			<br><b>id:</b> " +  alerts_ja[x].designation;
	        		mds = mds + "			<br><b>social type:</b> " + alerts_ja[x].social_type;
	        		mds = mds + "			<br><b>posted acct:</b> " + alerts_ja[x].created_by;
	        		mds = mds + "			<br><b>human clicks:</b> " + alerts_ja[x].sansbot_redirect_count;
	        		mds = mds + "			<br><b>bot clicks:</b> " + (alerts_ja[x].unabridged_redirect_count - alerts_ja[x].sansbot_redirect_count);
	        		mds = mds + "			<br><b>total clicks:</b> " +  alerts_ja[x].unabridged_redirect_count;
	        		mds = mds + "			<br><a href=\"#\" id=\"delete_link_" + x + "\">DELETE</a>";
	        		mds = mds + "		</td>";
	        		if(x%2 == 1)
        				mds = mds + "	</tr>";
        		}
        		mds = mds + "</table>";
        		$("#alerts_div").html(mds);
        		for(var x = 0; x < alerts_ja.length; x++)
        		{	
        			$("#delete_link_" + x).click({value1: x},
        					function (event) {
	        					//alert("deleting index=" + event.data.value1);
	        					$.ajax({
	        						type: 'GET',
	        						url: endpoint,
	        						data: {
	        				            method: "deleteSocialItem",
	        				            id: alerts_ja[event.data.value1].id, 
	        				            twitter_handle: twitter_handle,
	        				            twitter_access_token: twitter_access_token
	        						},
	        				        dataType: 'json',
	        				        async: false,
	        				        success: function (data, status) {
	        				        	if (data.response_status == "error")
	        				        		$("#message_div").html("error message=" + data.message);
	        				        	else
	        				        	{
	        				        		if(data.social_response === true)
	        				        			getFiredAlerts(beginstring, endstring, twitter_handle, twitter_access_token, station);
	        				        	}
	        				        }
	        				        ,
	        				        error: function (XMLHttpRequest, textStatus, errorThrown) {
	        				        	$("#message_div").html("ajax error");
	        				            console.log(textStatus, errorThrown);
	        				        }
	        					});
	        					return false;
        					}
        				);
        		}
        		
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
            console.log(textStatus, errorThrown);
        }
	});
}


