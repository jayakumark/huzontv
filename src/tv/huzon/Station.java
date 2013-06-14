package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Station implements java.lang.Comparable<Station> {

	/**
	 * @param args
	 */
	
	private String call_letters;
	private String city;
	private String state;
	private int dma_2013;
	private TreeSet<String> administrators;
	private TreeSet<String> reporters;
	private boolean active;
	private boolean valid;
	private int frame_rate; // milliseconds per frame
	private String s3_bucket_public_hostname;
	
	public Station(String inc_call_letters)
	{
		valid = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM stations WHERE call_letters='" + inc_call_letters + "'");
			if(rs.next())
			{
				call_letters = rs.getString("call_letters");
				city = rs.getString("city");
				state = rs.getString("state");
				dma_2013 = rs.getInt("dma_2013");
				frame_rate = rs.getInt("frame_rate");
				reporters = new TreeSet<String>();
				StringTokenizer st = new StringTokenizer(rs.getString("reporters")," ");
				while(st.hasMoreTokens())
				{
					reporters.add(st.nextToken()); // = rs.getString("station");
				}
				administrators = new TreeSet<String>();
				st = new StringTokenizer(rs.getString("administrators")," ");
				while(st.hasMoreTokens())
				{
					administrators.add(st.nextToken()); // = rs.getString("station");
				}
				active = rs.getBoolean("active");
				s3_bucket_public_hostname = rs.getString("s3_bucket_public_hostname");
				valid = true;
			}
			else
				valid = false;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Station constructor", "valid set to false. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
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
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Station constructor", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		
	}
	
	public boolean isValid()
	{
		return valid;
	}
	
	public String getCallLetters()
	{
		return call_letters;
	}
	
	public TreeSet<String> getAdministrators()
	{
		return administrators;
	}
	
	public TreeSet<String> getReporters()
	{
		return reporters;
	}

	public boolean isActive()
	{
		return active;
	}
	
	public String getCity()
	{
		return city;
	}
	
	public String getState()
	{
		return state;
	}
	
	public int getDMA()
	{
		return dma_2013;
	}
	
	public int getFrameRate()
	{
		return frame_rate;
	}
	
	public String getS3BucketPublicHostname()
	{
		return s3_bucket_public_hostname;
	}
	
	public TreeSet<Frame> getFrames(long begin_in_ms, long end_in_ms)
	{
		TreeSet<Frame> returnset = new TreeSet<Frame>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			System.out.println("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")");
			rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")"); // get the frames in the time range
		
			//System.out.println("Does the resultset have any rows?");
			if(rs.next()) // at least one row exists
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				int columncount = rsmd.getColumnCount();
				int reportercount = 0;
				int x = 1; 
				//System.out.println("Starting loop through " + columncount + " columns to find how many reporters are there...");
				while(x <= columncount)
				{
					if(rsmd.getColumnName(x).endsWith("_avg"))
					{
						reportercount++;
					}
					x++;
				}
				//System.out.println("Found " + reportercount + " columns. Initalizing arrays.");
				String reporter_designations[] = new String[reportercount];
				double reporter_avgs[] = new double[reportercount];
				String reporter_score_arrays[] = new String[reportercount];
				int reporter_nums[] = new int[reportercount];
				
				rs.beforeFirst();
				//System.out.println("Starting loop through resultset of frames...");
				while(rs.next())
				{
					int reporter_index = 0;
					x=1; 
					//System.out.println("Starting loop through " + columncount + " columns to fill reporter arrays...");
					while(x <= columncount)
					{
						//System.out.println("Reading columname: " + rsmd.getColumnName(x));
						if(rsmd.getColumnName(x).endsWith("_avg"))
						{
							reporter_designations[reporter_index] = rsmd.getColumnName(x).substring(0,rsmd.getColumnName(x).indexOf("_avg"));
							reporter_avgs[reporter_index] = rs.getDouble(x);
						}
						else if(rsmd.getColumnName(x).endsWith("_scores"))
						{
							reporter_score_arrays[reporter_index] = rs.getString(x);
						}
						else if(rsmd.getColumnName(x).endsWith("_num"))
						{
							reporter_nums[reporter_index] = rs.getInt(x);
							reporter_index++;
						}
						else
						{
							//System.out.println("Skipping a non-score-related row.");
						}
						x++;
					}
					//System.out.println("Adding Frame object to treeset and going to next...");
					returnset.add(new Frame(rs.getLong("timestamp_in_ms"), rs.getString("image_name"), rs.getString("s3_location"),
							rs.getString("url"), rs.getInt("frame_rate"), getCallLetters(), reporter_designations, 
							reporter_avgs, reporter_score_arrays, reporter_nums));
					//System.out.println("... frame added");
				}
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
				sqle.printStackTrace();
			}
		}  
		return returnset; 
	}
	
	public JSONArray getFramesAsJSONArray(long begin_in_ms, long end_in_ms, boolean get_score_data)
	{
		JSONArray frames_ja = new JSONArray();
		TreeSet<Frame> frameset = getFrames(begin_in_ms, end_in_ms);
		Iterator<Frame> it = frameset.iterator();
		Frame currentframe = null;
		while(it.hasNext())
		{
			currentframe = it.next();
			//System.out.println("putting frame " + currentframe.getTimestampInMillis() + " into jsonarray");
			frames_ja.put(currentframe.getAsJSONObject(get_score_data));
		}
		return frames_ja;
	}
	
	public JSONArray getFramesByDesignationAndHomogeneityThreshold(long begin_in_ms, long end_in_ms, String designation, double single_modifier_double, double delta_double)
	{
		JSONArray return_ja = new JSONArray();
		try
		{
			User reporter = new User(designation, "designation");
			double homogeneity_double = reporter.getHomogeneity();
			//double modifier_double = (new Double(request.getParameter("singlemodifier"))).doubleValue();
			double threshold = homogeneity_double * single_modifier_double;
			//double delta_double = (new Double(request.getParameter("delta"))).doubleValue();
			
				ResultSet rs = null;
				Connection con = null;
				Statement stmt = null;
				try
				{
					con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
					stmt = con.createStatement();
					rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters()  + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_avg > " + threshold + ")"); // get the frames in the time range
					rs.last();
					rs.beforeFirst(); // go back to the beginning for parsing
					JSONObject current_frame_jo = null;
					boolean all_others_below_delta = true;
					double control_avg = 100;
					double closest_avg = 0;
					JSONArray designations = new JSONArray(getReporters()); 
					double challenge_avg = 0;
					String closest_designation = "";
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
								//System.out.println("\t\tChallenge avg=" + challenge_avg + " (" + designations.getString(d) + ")");
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
							current_frame_jo.put("image_name", rs.getString("image_name"));
							current_frame_jo.put("url", rs.getString("url"));
							current_frame_jo.put("timestamp_in_ms", rs.getLong("timestamp_in_ms"));
							current_frame_jo.put("score_average", rs.getDouble(designation + "_avg"));
							current_frame_jo.put("homogeneity_score", homogeneity_double);
							current_frame_jo.put("threshold", threshold);
							current_frame_jo.put("closest_avg", closest_avg);
							current_frame_jo.put("closest_designation", closest_designation);
							return_ja.put(current_frame_jo);
						}
						else
						{
							//delta_suppressions++;
						}
					}
				}
				catch(SQLException sqle)
				{
					sqle.printStackTrace();
					SimpleEmailer se = new SimpleEmailer();
					try {
						se.sendMail("SQLException in Station.getFramesByDesignationAndHomogeneityThreshold", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
					} catch (MessagingException e) {
						e.printStackTrace();
					}
				}
				finally
				{
					try
					{
						if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
					}
					catch(SQLException sqle)
					{ 
						SimpleEmailer se = new SimpleEmailer();
						try {
							se.sendMail("SQLException in Station.getFramesByDesignationAndHomogeneityThreshold", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
						} catch (MessagingException e) {
							e.printStackTrace();
						}
					}
				}   	
			
		}
		catch(JSONException jsone)
		{
			
		}
		return return_ja;
	}
	
	public JSONArray getFramesByDesignation(long begin_in_ms, long end_in_ms, String designation, double single_modifier_double, double ma_modifier_double, int mawindow_int)
	{
		JSONArray return_ja = new JSONArray();
		User reporter = new User(designation, "designation");
		double homogeneity_double = reporter.getHomogeneity();
		//double ma_modifier_double = (new Double(request.getParameter("mamodifier"))).doubleValue();
		//double single_modifier_double = (new Double(request.getParameter("singlemodifier"))).doubleValue();
	//	int mawindow_int = Integer.parseInt(mawindow);
		int num_frames_in_mawindow = 0;
		try
		{
			ResultSet rs = null;
			Connection con = null;
			Statement stmt = null;
			Statement stmt2 = null;
			ResultSet rs2 = null;
			try
			{
				con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
				stmt = con.createStatement();
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")"); // get the frames in the time range
				rs.last();
				rs.beforeFirst(); // go back to the beginning for parsing
				JSONObject current_frame_jo = null;
				double total = 0.0;
				double ma_over_window = 0.0;
				while(rs.next())
				{
					current_frame_jo = new JSONObject();
					current_frame_jo.put("image_name", rs.getString("image_name"));
					current_frame_jo.put("timestamp_in_ms", rs.getLong("timestamp_in_ms"));
					current_frame_jo.put("designation_score", rs.getDouble(designation + "_avg"));
					current_frame_jo.put("homogeneity_score", homogeneity_double);
					current_frame_jo.put("ma_threshold", homogeneity_double * ma_modifier_double);
					current_frame_jo.put("single_threshold", homogeneity_double * single_modifier_double);
					stmt2 = con.createStatement();
					rs2 = stmt2.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms > " + (rs.getLong("timestamp_in_ms") - (mawindow_int*1000)) + " AND timestamp_in_ms <= " + rs.getLong("timestamp_in_ms") + ")");
					total = 0;
					num_frames_in_mawindow = 0;
					while(rs2.next())
					{
						total = total + rs2.getDouble(designation + "_avg");
						num_frames_in_mawindow++;
					}
					//ma_over_window = (total - lowest_score_in_window) / (mawindow_int - 1);
					ma_over_window = total / num_frames_in_mawindow;
					current_frame_jo.put("moving_average", ma_over_window);
					current_frame_jo.put("num_frames_in_mawindow", num_frames_in_mawindow);
					return_ja.put(current_frame_jo);
				}
			}
			catch(SQLException sqle)
			{
				sqle.printStackTrace();
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Endpoint getFramesByDesignationAndHomogeneityThreshold", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
			finally
			{
				try
				{
					if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
				}
				catch(SQLException sqle)
				{ 
					SimpleEmailer se = new SimpleEmailer();
					try {
						se.sendMail("SQLException in Endpoint getFramesByDesignationAndHomogeneityThreshold", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
					} catch (MessagingException e) {
						e.printStackTrace();
					}
				}
			}   		
		}
		catch(JSONException jsone)
		{
			
		}
		return return_ja;
	}
	
	
	public JSONObject getAsJSONObject()
	{
		JSONObject return_jo = new JSONObject();
		try
		{
			return_jo.put("call_letters", getCallLetters());
			return_jo.put("city", getCity());
			return_jo.put("state", getState());
			return_jo.put("frame_rate", getFrameRate());
			return_jo.put("dma2013", getDMA());
			return_jo.put("active", isActive());
			return_jo.put("reporters", new JSONArray(getReporters()).toString());
			return_jo.put("administrators", new JSONArray(getAdministrators()).toString());
		}	
		catch (JSONException e) {
			return_jo = null;
			e.printStackTrace();
		}
		return return_jo;
	}

	public int compareTo(Station o) // this sorts by call_letters alphabetically
	{
	    String othercall = ((Station)o).getCallLetters();
	    int x = othercall.compareTo(call_letters);
	    if(x >= 0) // this is to prevent equals
	    	return 1;
	    else
	    	return -1;
	}
	
	private String getLouisvilleDatestringFromTimestamp(long timestamp, String seconds_or_ms)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
		if(seconds_or_ms.equals("seconds"))
			cal.setTimeInMillis(timestamp * 1000);
		else if(seconds_or_ms.equals("ms"))
			cal.setTimeInMillis(timestamp);
		String year = new Integer(cal.get(Calendar.YEAR)).toString();
		String month = new Integer(cal.get(Calendar.MONTH) + 1).toString();
		if(month.length() == 1) { month = "0" + month; }
		String day = new Integer(cal.get(Calendar.DAY_OF_MONTH)).toString();
		if(day.length() == 1) { day = "0" + day;} 
		String hour24 = new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString();
		if(hour24.length() == 1) { hour24 = "0" + hour24;} 
		String minute = new Integer(cal.get(Calendar.MINUTE)).toString();
		if(minute.length() == 1) { minute = "0" + minute;} 
		String second = new Integer(cal.get(Calendar.SECOND)).toString();
		if(second.length() == 1) { second = "0" + second;} 
		String datestring = year  + month + day + "_" + hour24 + minute + second;
		return datestring;
	}

}
