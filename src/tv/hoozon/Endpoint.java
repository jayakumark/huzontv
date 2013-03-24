package tv.hoozon;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Endpoint extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException
	{
		System.err.println("Endpoint init()");
		 try {
		        Class.forName("com.mysql.jdbc.Driver");
		    } catch (ClassNotFoundException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    } 
		super.init(config);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		System.out.println("tv.hoozon.Endpoint.doPost(): entering...");
		response.setContentType("application/json; charset=UTF-8;");
		PrintWriter out = response.getWriter();
				JSONObject jsonresponse = new JSONObject();
		Calendar tempcal = Calendar.getInstance();
		long timestamp_at_entry = tempcal.getTimeInMillis();
		try
		{
			String method = request.getParameter("method");
			if(method == null)
			{
				jsonresponse.put("message", "Method not specified. This should probably produce HTML output reference information at some point.");
				jsonresponse.put("response_status", "error");
			}
			else if (method.equals("commitFrameData"))
			{
				String jsonpostbody = request.getParameter("jsonpostbody");
				if(jsonpostbody == null)
				{
					jsonresponse.put("message", "jsonpostbody was null. Couldn't find the parameter.");
					jsonresponse.put("response_status", "error");
				}
				else if(jsonpostbody.isEmpty())
				{
					jsonresponse.put("message", "jsonpostbody was empty.");
					jsonresponse.put("response_status", "error");
				}	
				else
				{
					JSONObject jo = new JSONObject(jsonpostbody);
					jsonresponse.put("response_status", "success");
					jsonresponse.put("postbody_as_received", jo);
					
					ResultSet rs = null;
					Connection con = null;
					Statement stmt = null;
					try
					{
						con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
						stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
						rs = stmt.executeQuery("SELECT * FROM frames_" + jo.getString("station") + " WHERE timestamp_in_seconds='" + jo.getInt("timestamp") + "' limit 0,1");
						double currentavgscore = 0.0;
						double reporter_total = 0.0;
						JSONArray all_scores_ja = null;
						JSONArray all_deviations_ja = null;
						JSONArray all_deviation_squares_ja = null;
						double sum_of_deviation_squares = 0.0;
						double stddev_double = 0.0;
						double total_score = 0.0;
						double average_of_all_scores = 0.0;
						if(!rs.next())
						{	
							rs.moveToInsertRow();
							all_scores_ja = new JSONArray(); // empty the scores array as we're starting to analyze a new row
							JSONArray ja = jo.getJSONArray("reporter_scores");
							for(int x = 0; x < ja.length(); x++)
							{
								reporter_total = 0.0;
								for(int i = 0; i < ja.getJSONObject(x).getJSONArray("scores").length(); i++)
								{
									reporter_total = reporter_total + ja.getJSONObject(x).getJSONArray("scores").getDouble(i); 
									all_scores_ja.put(ja.getJSONObject(x).getJSONArray("scores").getDouble(i));
									total_score = total_score + ja.getJSONObject(x).getJSONArray("scores").getDouble(i); 
								}
								currentavgscore = reporter_total / ja.getJSONObject(x).getJSONArray("scores").length();
								rs.updateString(ja.getJSONObject(x).getString("designation")+"_scores", ja.getJSONObject(x).getJSONArray("scores").toString());
								rs.updateDouble(ja.getJSONObject(x).getString("designation")+"_avg", currentavgscore);
								rs.updateInt(ja.getJSONObject(x).getString("designation")+"_num", ja.getJSONObject(x).getJSONArray("scores").length());
							}
							System.out.println("url: http://localhost/hoozon_finished/" + jo.getInt("timestamp") + ".jpg");
							rs.updateString("image_url", "http://localhost/hoozon_finished/" + jo.getInt("timestamp") + ".jpg");
							rs.updateLong("timestamp_in_seconds", jo.getInt("timestamp"));
							
							System.out.println("total of all scores=" + total_score);
							average_of_all_scores = total_score / all_scores_ja.length();
							System.out.println("average score of each comparison=" + average_of_all_scores);
																
							all_deviations_ja = new JSONArray();
							all_deviation_squares_ja = new JSONArray();
							// 2. get all deviations
							sum_of_deviation_squares = 0.0;
							for(int s = 0; s < all_scores_ja.length(); s++)
							{
								all_deviations_ja.put(all_scores_ja.getDouble(s) - average_of_all_scores);
								all_deviation_squares_ja.put(all_deviations_ja.getDouble(s) * all_deviations_ja.getDouble(s));
								sum_of_deviation_squares = sum_of_deviation_squares + all_deviation_squares_ja.getDouble(s);
							}
							double temp = sum_of_deviation_squares / (all_scores_ja.length() -1);
							stddev_double = Math.sqrt(temp);							  
							
							System.out.println("**** stddev=" + stddev_double);
							rs.updateDouble("average_score", average_of_all_scores);
							rs.updateDouble("stddev", stddev_double);
							
							rs.insertRow();
							rs.close();
							stmt.close();
							con.close();
							jsonresponse.put("response_status", "success");
							jsonresponse.put("postbody_as_received", jo);
							jsonresponse.put("message", "Scores should be entered into the database now.");
						}
						else
						{
							jsonresponse.put("message", "A frame for that timestamp already exists. Skipping insertion.");
							jsonresponse.put("response_status", "error");
						}
					}
					catch(SQLException sqle)
					{
						jsonresponse.put("message", "There was a problem attempting to insert the scores into the database. sqle.getMessage()=" + sqle.getMessage());
						jsonresponse.put("response_status", "error");
						sqle.printStackTrace();
					}
					finally
					{
						try
						{
							if (rs  != null)
								rs.close();
							if (stmt  != null)
								stmt.close();
							if (con  != null)
								con.close();
						}
						catch(SQLException sqle)
						{
							jsonresponse.put("warning", "There was a problem closing the resultset, statement and/or connection to the database.");
						}
					}   	
				}
			}
			else
			{
				jsonresponse.put("message", "Unknown method.");
				jsonresponse.put("response_status", "error");
			}
			tempcal = Calendar.getInstance();
			long timestamp_at_exit = tempcal.getTimeInMillis();
			long elapsed = timestamp_at_exit - timestamp_at_entry;
			jsonresponse.put("elapsed", elapsed);
			System.out.println("Endpoint.doGet(): final jsonresponse=" + jsonresponse);	// respond with object, success response, or error
			out.println(jsonresponse);
		}
		catch(JSONException jsone)
		{
			out.println("{ \"error\": { \"message\": \"JSONException caught in Endpoint\" } }");
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		}	
		return;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		System.out.println("tv.hoozon.Endpoint.doGet(): entering...");
		response.setContentType("application/json; charset=UTF-8;");
		PrintWriter out = response.getWriter();
		JSONObject jsonresponse = new JSONObject();
		Calendar tempcal = Calendar.getInstance();
		long timestamp_at_entry = tempcal.getTimeInMillis();
		try
		{
			String method = request.getParameter("method");
			if(method == null)
			{
				jsonresponse.put("message", "Method not specified. This should probably produce HTML output reference information at some point.");
				jsonresponse.put("response_status", "error");
			}
			else if (method.equals("getFrames"))
			{
				String begin = request.getParameter("begin"); // required
				String end = request.getParameter("end"); // required
				if(begin == null || begin.isEmpty() || end == null || end.isEmpty()) // must always have the time range 
				{
					jsonresponse.put("message", "begin or end was empty.");
					jsonresponse.put("response_status", "error");
				}
				else
				{
					ResultSet rs = null;
					Connection con = null;
					Statement stmt = null;
					int x = 0;
					double total = 0;
					try
					{
						con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
						stmt = con.createStatement();
						rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + ")"); // get the frames in the time range
						rs.last();
						jsonresponse.put("frames_processed", rs.getRow());  // get a row count
						rs.beforeFirst(); // go back to the beginning for parsing
						JSONObject current_frame_jo = null;
						JSONArray frames_ja = new JSONArray();
						while(rs.next())
						{
							current_frame_jo = new JSONObject();
							current_frame_jo.put("image_url", rs.getString("image_url"));
							current_frame_jo.put("timestamp_in_seconds", rs.getInt("timestamp_in_seconds"));
							frames_ja.put(current_frame_jo);
						}
						jsonresponse.put("response_status", "success");
						jsonresponse.put("frames", frames_ja);
					}
					catch(SQLException sqle)
					{
						jsonresponse.put("message", "Error getting frames from DB. sqle.getMessage()=" + sqle.getMessage());
						jsonresponse.put("response_status", "error");
						sqle.printStackTrace();
					}
					finally
					{
						try
						{
							if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
						}
						catch(SQLException sqle)
						{ jsonresponse.put("warning", "Problem closing resultset, statement and/or connection to the database."); }
					}   	
				}
			}
			else if (method.equals("getFramesByDesignation"))
			{
				String begin = request.getParameter("begin"); // required
				String end = request.getParameter("end"); // required
				String designation = request.getParameter("designation");
				double ma_modifier_double = (new Double(request.getParameter("ma_modifier"))).doubleValue();
				double single_modifier_double = (new Double(request.getParameter("single_modifier"))).doubleValue();
				int alert_waiting_period = (new Integer(request.getParameter("alert_waiting_period"))).intValue();
				String seconds_to_average = request.getParameter("seconds_to_average");
				if(begin == null || begin.isEmpty() || end == null || end.isEmpty()) // must always have the time range 
				{
					jsonresponse.put("message", "begin or end was empty.");
					jsonresponse.put("response_status", "error");
				}
				else
				{
					ResultSet rs = null;
					Connection con = null;
					Statement stmt = null;
					try
					{
						con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
						stmt = con.createStatement();
						rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + ")"); // get the frames in the time range
						rs.last();
						jsonresponse.put("frames_processed", rs.getRow());  // get a row count
						rs.beforeFirst(); // go back to the beginning for parsing
						JSONObject current_frame_jo = null;
						JSONArray frames_ja = new JSONArray();
						JSONArray alertframes_ja = new JSONArray();
						int x = 0;
						int y = 0;
						double sum_of_last_few_seconds = 0.0;
						double homogeneity_double = getHomogeneityScore("wkyt",designation);
						int seconds_to_average_int = Integer.parseInt(seconds_to_average);
						int frames_since_last_alert = alert_waiting_period + 1; // this makes it so an alert can go out immediately
						while(rs.next())
						{
							current_frame_jo = new JSONObject();
							current_frame_jo.put("image_url", rs.getString("image_url"));
							current_frame_jo.put("timestamp_in_seconds", rs.getInt("timestamp_in_seconds"));
							current_frame_jo.put("designation_score", rs.getDouble(designation + "_avg"));
							current_frame_jo.put("homogeneity_score", homogeneity_double);
							current_frame_jo.put("ma_threshold", homogeneity_double * ma_modifier_double);
							current_frame_jo.put("single_threshold", homogeneity_double * single_modifier_double);
							y=0; sum_of_last_few_seconds = 0.0;
							while(y < seconds_to_average_int)
							{
								if(y == 0)
									rs.getDouble(designation + "_avg");
								else
								{	
									if((x-y) >= 0)
									{	
										sum_of_last_few_seconds = sum_of_last_few_seconds + frames_ja.getJSONObject(x-y).getDouble("designation_score");
									}
								}
								y++;
							}
							current_frame_jo.put("moving_average", sum_of_last_few_seconds / seconds_to_average_int);
							frames_ja.put(current_frame_jo);
							
							if((sum_of_last_few_seconds / seconds_to_average_int) > (homogeneity_double * ma_modifier_double) && // the moving average is greater than the moving average threshold
									frames_since_last_alert > alert_waiting_period &&  // it has been at least alert_waiting_period second since last alert
									rs.getDouble(designation + "_avg") > (homogeneity_double * single_modifier_double)) // this frame's raw singular average is greater than single threshold
							{
								alertframes_ja.put(current_frame_jo);
								frames_since_last_alert = 0;
							}
							frames_since_last_alert++;
							x++;
						}
						jsonresponse.put("response_status", "success");
						jsonresponse.put("frames", frames_ja);
						jsonresponse.put("alertframes", alertframes_ja);
					}
					catch(SQLException sqle)
					{
						jsonresponse.put("message", "Error getting frames from DB. sqle.getMessage()=" + sqle.getMessage());
						jsonresponse.put("response_status", "error");
						sqle.printStackTrace();
					}
					finally
					{
						try
						{
							if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
						}
						catch(SQLException sqle)
						{ jsonresponse.put("warning", "Problem closing resultset, statement and/or connection to the database."); }
					}   	
				}
			}
			else if (method.equals("getFramesByDesignationAndHomogeneityThreshold"))
			{	
				String begin = request.getParameter("begin"); // required
				String end = request.getParameter("end"); // required
				if(begin == null || begin.isEmpty() || end == null || end.isEmpty()) // must always have the time range 
				{
					jsonresponse.put("message", "begin and/or end was null and/or empty.");
					jsonresponse.put("response_status", "error");
				}
				else // begin/end ok
				{	
					String designation = request.getParameter("designation"); 
					if(designation == null || designation.isEmpty())
					{	
						jsonresponse.put("message", "designation was null or empty");
						jsonresponse.put("response_status", "error");
					}
					else // designation/numstddev ok
					{
						double homogeneity_double = getHomogeneityScore("wkyt",designation);
						double modifier_double = (new Double(request.getParameter("modifier"))).doubleValue();
						double threshold = homogeneity_double * modifier_double;
						double delta_double = (new Double(request.getParameter("delta"))).doubleValue();
						try
						{
							ResultSet rs = null;
							Connection con = null;
							Statement stmt = null;
							try
							{
								
								con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
								stmt = con.createStatement();
								rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + " AND " + designation + "_avg > " + threshold + ")"); // get the frames in the time range
								rs.last();
								jsonresponse.put("frames_processed", rs.getRow());  // get a row count
								rs.beforeFirst(); // go back to the beginning for parsing
								JSONObject current_frame_jo = null;
								JSONArray frames_ja = new JSONArray();
								boolean all_others_below_delta = true;
								double control_avg = 100;
								double closest_avg = 0;
								JSONArray designations = getDesignations("wkyt");
								double challenge_avg = 0;
								String closest_designation = "";
								int delta_suppressions = 0;
								while(rs.next()) // get one row
								{
									all_others_below_delta = true;
									control_avg = rs.getDouble(designation + "_avg");
									closest_avg = 0;
									for(int d = 0; d < designations.length(); d++)
									{	
										if(!designations.getString(d).equals(designation)) // skip comparing this against itself
										{	
											challenge_avg = rs.getDouble(designations.getString(d) + "_avg");
											if(challenge_avg > closest_avg)
											{
												closest_avg = challenge_avg;
												closest_designation = designations.getString(d);
											}
											System.out.println("\t\tChallenge avg=" + challenge_avg + " (" + designations.getString(d) + ")");
											if((control_avg - challenge_avg) < delta_double) // this one did not satisfy the delta requirement
											{
												all_others_below_delta = false;
												// could include a break here to save cycles, but want to get closest for informational purposes
											}
											
										}
									}
									if(all_others_below_delta)
									{
										current_frame_jo = new JSONObject();
										current_frame_jo.put("image_url", rs.getString("image_url"));
										current_frame_jo.put("timestamp_in_seconds", rs.getInt("timestamp_in_seconds"));
										current_frame_jo.put("score_average", rs.getDouble(designation + "_avg"));
										current_frame_jo.put("homogeneity_score", homogeneity_double);
										current_frame_jo.put("threshold", threshold);
										current_frame_jo.put("closest_avg", closest_avg);
										current_frame_jo.put("closest_designation", closest_designation);
										//System.out.println("Adding a frame. " + rs.getString("image_url") + " " + rs.getInt("timestamp_in_seconds") + " " + rs.getDouble(designation + "_avg"));
										
										// if the previous frame's timestamp is one less than this one
										if(frames_ja.length() > 0 && frames_ja.getJSONObject(frames_ja.length()-1).getInt("timestamp_in_seconds") == (rs.getInt("timestamp_in_seconds") - 1))
										{
											// then add to whatever streak number is there
											current_frame_jo.put("streak", frames_ja.getJSONObject(frames_ja.length()-1).getInt("streak")+1);
										}
										else
										{
											// else set the streak to 0
											current_frame_jo.put("streak", 1);
										}
										frames_ja.put(current_frame_jo);
									}
									else
									{
										delta_suppressions++;
									}
								}
								jsonresponse.put("delta_suppressions", delta_suppressions);
								jsonresponse.put("response_status", "success");
								jsonresponse.put("frames", frames_ja);
							}
							catch(SQLException sqle)
							{
								jsonresponse.put("message", "Error getting frames from DB. sqle.getMessage()=" + sqle.getMessage());
								jsonresponse.put("response_status", "error");
								sqle.printStackTrace();
							}
							finally
							{
								try
								{
									if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
								}
								catch(SQLException sqle)
								{ jsonresponse.put("warning", "Problem closing resultset, statement and/or connection to the database."); }
							}   	
							
						}
						catch(NumberFormatException nfe)
						{
							jsonresponse.put("message", "threshold was not a valid double value");
							jsonresponse.put("response_status", "error");
						}
					}
				}
			}
			else if (method.equals("simulateNewFrame"))
			{
				String ts = request.getParameter("ts"); // required
				int moving_average_window_int = Integer.parseInt(request.getParameter("moving_average_window"));
				int alert_waiting_period = Integer.parseInt(request.getParameter("alert_waiting_period"));
				double ma_modifier_double = (new Double(request.getParameter("ma_modifier"))).doubleValue();
				double single_modifier_double = (new Double(request.getParameter("single_modifier"))).doubleValue();
				if(ts == null || ts.isEmpty()) // must always have the time range 
				{
					jsonresponse.put("message", "begin and/or end was null and/or empty.");
					jsonresponse.put("response_status", "error");
				}
				else // begin/end ok
				{	
					long ts_long = Long.parseLong(ts);
					JSONArray dnh_ja = getDesignationsAndHomogeneities("wkyt");
					try
					{
						ResultSet rs = null;
						Connection con = null;
						Statement stmt = null;
						try
						{
							con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
							stmt = con.createStatement();
							rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds > " + (ts_long - moving_average_window_int) + " AND timestamp_in_seconds <= " + ts + ") ORDER BY timestamp_in_seconds ASC"); 
							//AND timestamp_in_seconds > " + begin + " AND " + designation + "_avg > " + threshold + ")"); // get the frames in the time range
							rs.last();
							jsonresponse.put("frames_processed", rs.getRow());  // get a row count
							if(rs.getRow() == 0)
							{
								jsonresponse.put("message", "No frames for this entire window. Returning with next_frame.");
								jsonresponse.put("response_status", "error");
								long next_frame = getNextFrame(ts_long);
								jsonresponse.put("next_frame", next_frame);
							}
							else if(rs.getRow() < moving_average_window_int)
							{
								jsonresponse.put("message", "missing a frame in the target window, cant draw safe conclusions");
								jsonresponse.put("response_status", "error");
							}
							else
							{	
								rs.beforeFirst(); // go back to the beginning for parsing
								JSONArray frames_ja = new JSONArray();
								JSONObject current_frame_jo = null;
								JSONObject current_frame_scores_jo = null;
								/*
								
								{
									"response_status": "success",
									"frames":
										[
											{
												"timestamp_in_seconds": 12312312,
												"image_url": "url",
												"scores":
													{
														"person_a": .984332,
														"person_b": .3423423
														...
													}
											}
											...											
										],
									"averages":
										[
											{
												"designation": "person_a",
												"average": .9754
											},
											{
												"designation": "person_b",
												"average": .5432
											},
											...
										]
								}
								
								*/
								while(rs.next()) // get one row
								{
									current_frame_jo = new JSONObject();
									current_frame_jo.put("image_url", rs.getString("image_url"));
									current_frame_jo.put("timestamp_in_seconds", rs.getLong("timestamp_in_seconds"));
									current_frame_scores_jo = new JSONObject();
									for(int x = 0; x < dnh_ja.length(); x++)
									{
										current_frame_scores_jo.put(dnh_ja.getJSONObject(x).getString("designation"), rs.getDouble(dnh_ja.getJSONObject(x).getString("designation") + "_avg"));
									}
									current_frame_jo.put("scores", current_frame_scores_jo);
									frames_ja.put(current_frame_jo);
								}
								
								double person_total = 0.0;
								double person_avg = 0.0;
								JSONArray designation_averages_ja = new JSONArray();
								JSONObject jo = new JSONObject();
								double max_avg = 0.0;
								String max_designation = "";
								for(int x = 0; x < dnh_ja.length(); x++)
								{
									jo = new JSONObject();
									person_total = 0.0;
									for(int j = 0; j < frames_ja.length(); j++)
									{	
										person_total = person_total + frames_ja.getJSONObject(j).getJSONObject("scores").getDouble(dnh_ja.getJSONObject(x).getString("designation"));
									}
									person_avg = person_total/frames_ja.length();
									jo.put("designation", dnh_ja.getJSONObject(x).getString("designation"));
									jo.put("average", person_avg);
									if(person_avg > max_avg)
									{
										max_avg = person_avg;
										max_designation = dnh_ja.getJSONObject(x).getString("designation");
									}
									designation_averages_ja.put(jo);
								}
								double max_homogeneity_double = getHomogeneityScore("wkyt", max_designation);
								if(max_avg > (max_homogeneity_double * ma_modifier_double)) // the moving average is greater than the moving average threshold
								{
									if(frames_ja.getJSONObject(frames_ja.length() -1).getJSONObject("scores").getDouble(max_designation) > (max_homogeneity_double * single_modifier_double)) // this frame's raw singular average is greater than single threshold
									{
										long last_alert = getLastAlert("wkyt", max_designation);
										if((ts_long - last_alert) > alert_waiting_period)
										{
											jsonresponse.put("alert_fired", "yes");
											jsonresponse.put("score", frames_ja.getJSONObject(frames_ja.length() -1).getJSONObject("scores").getDouble(max_designation));
											jsonresponse.put("designation", max_designation);
											jsonresponse.put("twitter_handle", getTwitterHandle("wkyt",max_designation));
											jsonresponse.put("moving_average", max_avg);
											jsonresponse.put("homogeneity_score", max_homogeneity_double);
											jsonresponse.put("ma_threshold", max_homogeneity_double * ma_modifier_double);
											jsonresponse.put("single_threshold", max_homogeneity_double * single_modifier_double);
											setLastAlert("wkyt", max_designation, ts_long);
										}
										else
										{
											jsonresponse.put("alert_fired", "no");
										}
									}
									else
									{
										jsonresponse.put("alert_fired", "no");
									}
								}
								else
								{
									jsonresponse.put("alert_fired", "no");
								}
								jsonresponse.put("designation_averages", designation_averages_ja);
								jsonresponse.put("response_status", "success");
								jsonresponse.put("frames", frames_ja);
							}
						}
						catch(SQLException sqle)
						{
							jsonresponse.put("message", "Error getting frames from DB. sqle.getMessage()=" + sqle.getMessage());
							jsonresponse.put("response_status", "error");
							sqle.printStackTrace();
						}
						finally
						{
							try
							{
								if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
							}
							catch(SQLException sqle)
							{ jsonresponse.put("warning", "Problem closing resultset, statement and/or connection to the database."); }
						}   	
						
					}
					catch(NumberFormatException nfe)
					{
						jsonresponse.put("message", "threshold was not a valid double value");
						jsonresponse.put("response_status", "error");
					}
				}
			}
			else if (method.equals("resetAllLastAlerts"))
			{	
				String station = request.getParameter("station"); // required
				if(station == null || station.isEmpty()) // must always have the time range 
				{
					jsonresponse.put("message", "station was null or empty.");
					jsonresponse.put("response_status", "error");
				}
				else // station ok
				{	
					boolean successful = resetAllLastAlerts(station);
					jsonresponse.put("response_status", "success");
				}
			}
			else if (method.equals("getDesignations"))
			{	
				String station = request.getParameter("station"); // required
				if(station == null || station.isEmpty()) // must always have the time range 
				{
					jsonresponse.put("message", "station was null or empty.");
					jsonresponse.put("response_status", "error");
				}
				else // station ok
				{	
					JSONArray designations_ja = getDesignations(station);
					if(designations_ja == null)
					{
						jsonresponse.put("message", "Error getting designations from DB.");
						jsonresponse.put("response_status", "error");
					}
					else
					{
						jsonresponse.put("response_status", "success");
						jsonresponse.put("designations", designations_ja);
					}
				}
			}
			else
			{
				jsonresponse.put("message", "Unknown method.");
				jsonresponse.put("response_status", "error");
			}
			tempcal = Calendar.getInstance();
			long timestamp_at_exit = tempcal.getTimeInMillis();
			long elapsed = timestamp_at_exit - timestamp_at_entry;
			jsonresponse.put("elapsed", elapsed);
			System.out.println("Endpoint.doGet(): final jsonresponse=" + jsonresponse);	// respond with object, success response, or error
			out.println(jsonresponse);
		}
		catch(JSONException jsone)
		{
			out.println("{ \"error\": { \"message\": \"JSONException caught in Endpoint\" } }");
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		}	
		return;
	}
	
	long getNextFrame(long inc_ts)
	{
		long next_frame = -1L;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement();
			// get next frame in database, up to one day from now.
			rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds>" + inc_ts + " AND timestamp_in_seconds<" + (inc_ts + 86400) + ") limit 1"); 
			if(rs.next())
			{
				next_frame = rs.getLong("timestamp_in_seconds");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return next_frame; // -1L indicates no frame found
	}
	
	
	JSONArray getDesignations(String station)
	{
		JSONArray designations_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT designation FROM people_" + station); 
			while(rs.next())
			{
				designations_ja.put(rs.getString("designation"));
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			return null;
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
				return null;
			}
		}  	
		return designations_ja;
	}
	
	JSONArray getDesignationsAndHomogeneities(String station)
	{
		JSONArray designations_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT designation,homogeneity FROM people_" + station); 
			JSONObject jo = null;
			while(rs.next())
			{
				jo = new JSONObject();
				try {
					jo.put("designation", rs.getString("designation"));
					jo.put("homogeneity", rs.getDouble("homogeneity"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				designations_ja.put(jo);
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			return null;
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
				return null;
			}
		}  	
		return designations_ja;
	}
	
	String getTwitterHandle(String station, String designation)
	{
		String returnval = "unknown";
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			if(rs.next())
			{
				returnval = rs.getString("twitter_handle");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	double getHomogeneityScore(String station, String designation)
	{
		double returnval = 0.0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			while(rs.next())
			{
				returnval = rs.getDouble("homogeneity");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	long getLastAlert(String station, String designation)
	{
		long returnval = 0L;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			while(rs.next())
			{
				returnval = rs.getLong("last_alert");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	boolean setLastAlert(String station, String designation, long alert_ts)
	{
		boolean returnval;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			while(rs.next())
			{
				rs.updateLong("last_alert", alert_ts);
				rs.updateRow();
			}
			returnval = true;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			returnval = false;
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	boolean resetAllLastAlerts(String station)
	{
		boolean returnval;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people_" + station + ""); 
			while(rs.next())
			{
				rs.updateLong("last_alert", 0);
				rs.updateRow();
			}
			returnval = true;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			returnval = false;
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
}
