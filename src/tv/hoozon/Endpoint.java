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
			else if (method.equals("getFramesByDesignationThresholdAndDelta"))
			{	
				String begin = request.getParameter("begin"); // required
				String end = request.getParameter("end"); // required
				if(begin == null || begin.isEmpty() || end == null || end.isEmpty()) // must always have the time range 
				{
					jsonresponse.put("message", "begin or end was empty.");
					jsonresponse.put("response_status", "error");
				}
				else // begin/end ok
				{	
					String designation = request.getParameter("designation"); 
					String threshold = request.getParameter("threshold"); 
					String delta = request.getParameter("delta");
					if(designation == null || designation.isEmpty() || threshold == null || threshold.isEmpty())
					{	
						jsonresponse.put("message", "designation or threshold was empty");
						jsonresponse.put("response_status", "error");
					}
					else // designation/threshold ok
					{
						double threshold_double = 0.0;
						double delta_double = -1;
						JSONArray designations = null; 
						try
						{
							threshold_double = (new Double(threshold)).doubleValue();
							if(delta != null)
							{
								delta_double = (new Double(delta)).doubleValue();
								designations = getDesignations("wkyt");
							}
							// threshold value OK
							ResultSet rs = null;
							Connection con = null;
							Statement stmt = null;
							try
							{
								con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
								stmt = con.createStatement();
								rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + " AND " + designation + "_avg > " + threshold_double + ")"); // get the frames in the time range
								rs.last();
								jsonresponse.put("frames_processed", rs.getRow());  // get a row count
								rs.beforeFirst(); // go back to the beginning for parsing
								JSONObject current_frame_jo = null;
								JSONArray frames_ja = new JSONArray();
								boolean all_others_below_delta = true;
								double control_avg = 0.0;
								String closest_designation = "";
								double closest_avg = 0;
								double challenge_avg = 0.0;
								while(rs.next())
								{
									if(delta_double >= 0.0)
									{
										all_others_below_delta = true;
										control_avg = rs.getDouble(designation + "_avg");
										closest_avg = 0;
										System.out.println("Delta supplied. Control avg=" + control_avg);
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
											current_frame_jo.put("closest_avg", closest_avg);
											current_frame_jo.put("closest_designation", closest_designation);
											//System.out.println("Adding a frame. " + rs.getString("image_url") + " " + rs.getInt("timestamp_in_seconds") + " " + rs.getDouble(designation + "_avg"));
											frames_ja.put(current_frame_jo);
										}
									}
									else
									{
										current_frame_jo = new JSONObject();
										current_frame_jo.put("image_url", rs.getString("image_url"));
										current_frame_jo.put("timestamp_in_seconds", rs.getInt("timestamp_in_seconds"));
										current_frame_jo.put("score_average", rs.getDouble(designation + "_avg"));
										//System.out.println("Adding a frame. " + rs.getString("image_url") + " " + rs.getInt("timestamp_in_seconds") + " " + rs.getDouble(designation + "_avg"));
										frames_ja.put(current_frame_jo);
									}
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
						catch(NumberFormatException nfe)
						{
							jsonresponse.put("message", "threshold was not a valid double value");
							jsonresponse.put("response_status", "error");
						}
					}
				}
			}
			else if (method.equals("getFramesByDesignationAndDynamicThreshold"))
			{	
				String begin = request.getParameter("begin"); // required
				String end = request.getParameter("end"); // required
				if(begin == null || begin.isEmpty() || end == null || end.isEmpty()) // must always have the time range 
				{
					jsonresponse.put("message", "begin or end was empty.");
					jsonresponse.put("response_status", "error");
				}
				else // begin/end ok
				{	
					String designation = request.getParameter("designation"); 
					String numstddev = request.getParameter("numstddev"); 
					String basement = request.getParameter("basement");
					if(designation == null || designation.isEmpty() || numstddev == null || numstddev.isEmpty())
					{	
						jsonresponse.put("message", "designation or numstddev was empty");
						jsonresponse.put("response_status", "error");
					}
					else // designation/numstddev ok
					{
						double numstddev_double = 0;
						double basement_double = 0.0;
						try
						{
							numstddev_double = (new Double(numstddev)).doubleValue();
							basement_double = (new Double(basement)).doubleValue();
							
							ResultSet rs = null;
							Connection con = null;
							Statement stmt = null;
							try
							{
								con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
								stmt = con.createStatement();
								rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + " AND " + designation + "_avg > " + basement_double + ")"); // get the frames in the time range
								rs.last();
								jsonresponse.put("frames_processed", rs.getRow());  // get a row count
								rs.beforeFirst(); // go back to the beginning for parsing
								JSONObject current_frame_jo = null;
								JSONArray frames_ja = new JSONArray();
								while(rs.next()) // get one row
								{
									if((rs.getDouble(designation + "_avg") > (rs.getDouble("average_score") + (numstddev_double * rs.getDouble("stddev"))) && (rs.getDouble(designation + "_avg") > basement_double)))
									{
										current_frame_jo = new JSONObject();
										current_frame_jo.put("image_url", rs.getString("image_url"));
										current_frame_jo.put("timestamp_in_seconds", rs.getInt("timestamp_in_seconds"));
										current_frame_jo.put("score_average", rs.getDouble(designation + "_avg"));
										current_frame_jo.put("average_of_all_scores", rs.getDouble("average_score"));
										current_frame_jo.put("stddev", rs.getDouble("stddev"));
										current_frame_jo.put("one_stddev_above_avg", rs.getDouble("average_score") + rs.getDouble("stddev"));
										current_frame_jo.put("two_stddev_above_avg", rs.getDouble("average_score") + rs.getDouble("stddev") + rs.getDouble("stddev"));
										current_frame_jo.put("three_stddev_above_avg", rs.getDouble("average_score") + rs.getDouble("stddev") + rs.getDouble("stddev") + rs.getDouble("stddev"));
										//System.out.println("Adding a frame. " + rs.getString("image_url") + " " + rs.getInt("timestamp_in_seconds") + " " + rs.getDouble(designation + "_avg"));
										frames_ja.put(current_frame_jo);
									}
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
						catch(NumberFormatException nfe)
						{
							jsonresponse.put("message", "threshold was not a valid double value");
							jsonresponse.put("response_status", "error");
						}
					}
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
	
}
