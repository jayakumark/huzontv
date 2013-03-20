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
						double total = 0.0;
						if(!rs.next())
						{	
							rs.moveToInsertRow();
							JSONArray ja = jo.getJSONArray("reporter_scores");
							for(int x = 0; x < ja.length(); x++)
							{
								total = 0.0;
								for(int i = 0; i < ja.getJSONObject(x).getJSONArray("scores").length(); i++)
								{
									total = total + ja.getJSONObject(x).getJSONArray("scores").getDouble(i); 
								}
								currentavgscore = total / ja.getJSONObject(x).getJSONArray("scores").length();
								rs.updateString(ja.getJSONObject(x).getString("designation")+"_scores", ja.getJSONObject(x).getJSONArray("scores").toString());
								rs.updateDouble(ja.getJSONObject(x).getString("designation")+"_avg", currentavgscore);
								rs.updateInt(ja.getJSONObject(x).getString("designation")+"_num", ja.getJSONObject(x).getJSONArray("scores").length());
							}
							System.out.println("url: http://localhost/hoozon_finished/" + jo.getInt("timestamp") + ".jpg");
							rs.updateString("image_url", "http://localhost/hoozon_finished/" + jo.getInt("timestamp") + ".jpg");
							rs.updateLong("timestamp_in_seconds", jo.getInt("timestamp"));
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
			else if (method.equals("getFramesByDesignationAndThreshold"))
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
					if(designation == null || designation.isEmpty() || threshold == null || threshold.isEmpty())
					{	
						jsonresponse.put("message", "designation or threshold was empty");
						jsonresponse.put("response_status", "error");
					}
					else // designation/threshold ok
					{
						double threshold_double = 0.0;
						try
						{
							threshold_double = (new Double(threshold)).doubleValue();
							
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
								while(rs.next())
								{
									current_frame_jo = new JSONObject();
									current_frame_jo.put("image_url", rs.getString("image_url"));
									current_frame_jo.put("timestamp_in_seconds", rs.getInt("timestamp_in_seconds"));
									current_frame_jo.put("score_average", rs.getDouble(designation + "_avg"));
									//System.out.println("Adding a frame. " + rs.getString("image_url") + " " + rs.getInt("timestamp_in_seconds") + " " + rs.getDouble(designation + "_avg"));
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
					ResultSet rs = null;
					Connection con = null;
					Statement stmt = null;
					try
					{
						con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
						stmt = con.createStatement();
						rs = stmt.executeQuery("SELECT designation FROM people_" + station); 
						JSONArray designations_ja = new JSONArray();
						while(rs.next())
						{
							designations_ja.put(rs.getString("designation"));
						}
						jsonresponse.put("response_status", "success");
						jsonresponse.put("designations", designations_ja);
					}
					catch(SQLException sqle)
					{
						jsonresponse.put("message", "Error getting designations from DB. sqle.getMessage()=" + sqle.getMessage());
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
}
