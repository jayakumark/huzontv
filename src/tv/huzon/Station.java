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
			rs.close();
			stmt.close();
			con.close();
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
	
	TreeSet<Frame> getFramesFromResultSet(ResultSet rs)
	{
		TreeSet<Frame> returnframes = new TreeSet<Frame>();
		try
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
			//String reporter_designations[] = new String[reportercount];
			//double reporter_avgs[] = new double[reportercount];
			//JSONArray reporter_score_arrays[] = new JSONArray[reportercount];
			//int reporter_nums[] = new int[reportercount];
			String reporter_designations[] = null;
			double reporter_avgs[] = null;
			JSONArray reporter_score_arrays[] = null;
			int reporter_nums[] = null;
			rs.beforeFirst();
			//System.out.println("Starting loop through resultset of frames...");
			while(rs.next())
			{
				reporter_designations = new String[reportercount];
				reporter_avgs = new double[reportercount];
				reporter_score_arrays = new JSONArray[reportercount];
				reporter_nums = new int[reportercount];
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
						if(rs.getString(x).isEmpty())
							reporter_score_arrays[reporter_index] = new JSONArray();
						else
							reporter_score_arrays[reporter_index] = new JSONArray(rs.getString(x));
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
				returnframes.add(new Frame(rs.getLong("timestamp_in_ms"), rs.getString("image_name"), rs.getString("s3_location"),
						rs.getString("url"), rs.getInt("frame_rate"), getCallLetters(), reporter_designations, 
						reporter_avgs, reporter_score_arrays, reporter_nums));
				//System.out.println("... frame added");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}  
		return returnframes;
	}
	
	public TreeSet<Frame> getFrames(long begin_in_ms, long end_in_ms, String designation, double single_modifier_double) // INCLUSIVE
	{
		TreeSet<Frame> returnset = new TreeSet<Frame>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			if(designation == null) // get all frames
			{	
				System.out.println("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")");
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")"); 
			}
			else if(designation != null) // get all frames where designation is above single thresh
			{	
				User reporter = new User(designation, "designation");
				double homogeneity_double = reporter.getHomogeneity();
				double threshold = homogeneity_double * single_modifier_double;
				System.out.println("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_avg > " + threshold + ")"); 
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_avg > " + threshold + ")"); 
			}
		
			//System.out.println("Does the resultset have any rows?");
			if(rs.next()) // at least one row exists
			{
				returnset = getFramesFromResultSet(rs);
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
		TreeSet<Frame> frameset = getFrames(begin_in_ms, end_in_ms, null, 0);
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
	
	public JSONArray getFramesAboveDesignationHomogeneityThresholdAsJSONArray(long begin_in_ms, long end_in_ms, String designation, double single_modifier_double, boolean get_score_data)
	{
		JSONArray frames_ja = new JSONArray();
		TreeSet<Frame> frameset = getFrames(begin_in_ms, end_in_ms, designation, single_modifier_double);
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
		
	/*
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
	}*/
	
	/*
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
	*/
	
	/*
	boolean testFrameForMovingAverage(long ts, int maw_int, String current_designation, double current_homogeneity, double ma_modifier_double)
	{
		Connection con = null;
		Statement stmt = null;
		ResultSet rs2 = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs2 = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms > " + (ts - maw_int*1000) + " AND timestamp_in_ms < " + ts + ")");
			
			// 6/12/2013 simplified this function. To see old vers2ion, check github prior to this date.
			
			// so what we're doing here is we've got a single frame with a single score above the single thresh.
			// we want to check the moving average of this frame (going back maw_int*1000 milliseconds) to see if the ma is above its required thresh, too
			
			rs2.last();
			int num_frames_in_window = rs2.getRow();
			rs2.beforeFirst();
			int i = 0; 
			double total = 0;
			double ma_over_window = 0;
			
			
			if(num_frames_in_window < maw_int) // only process this frame if there were enough prior frames to warrn
			{
				// NOT ENOUGH FRAMES (i.e. less than 1 per second)
				System.out.println("Endpoint.getAlertFrames(): not enough frames in this moving average window (" + num_frames_in_window + " < " + maw_int + ")");
			}
			else // there were enough frames
			{
			
				while(rs2.next()) // looping through all the frames in the moving average window before the current frame
				{
					total = total + rs2.getDouble(current_designation + "_avg"); // the running total of the last maw_int frames
					i++;
				}
				ma_over_window = total / i; // i should = num_frames_in_window
				System.out.println("Endpoint.getAlertFrames(): there were enough frames. ma_over_window=" + ma_over_window);
			}
			
			if(ma_over_window > (current_homogeneity * ma_modifier_double))
				return true;
			else
				return false;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint testFrameForMovingAverage", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				if (rs2  != null){ rs2.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Endpoint testFrameForMovingAverage", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}   
		return false; // if it reaches here, something has failed.
	}
	

	double getMovingAverage(long ts, int maw_int, String current_designation)
	{
		Connection con = null;
		Statement stmt = null;
		ResultSet rs2 = null;
		double ma_over_window = 0;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs2 = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms > " + (ts - maw_int*1000) + " AND timestamp_in_ms <= " + ts + ")");
			
			// 6/12/2013 simplified this function. To see old vers2ion, check github prior to this date.
			
			// so what we're doing here is we've got a single frame with a single score above the single thresh.
			// we want to check the moving average of this frame (going back maw_int*1000 milliseconds) to see if the ma is above its required thresh, too
			
			rs2.last();
			int num_frames_in_window = rs2.getRow();
			rs2.beforeFirst();
			int i = 0; 
			double total = 0;
			
			if(num_frames_in_window < maw_int) // only process this frame if there were enough prior frames to warrn
			{
				// NOT ENOUGH FRAMES (i.e. less than 1 per second)
				System.out.println("Endpoint.getAlertFrames(): not enough frames in this moving average window (" + num_frames_in_window + " < " + maw_int + ")");
			}
			else // there were enough frames
			{
			
				while(rs2.next()) // looping through all the frames in the moving average window before the current frame
				{
					total = total + rs2.getDouble(current_designation + "_avg"); // the running total of the last maw_int frames
					i++;
				}
				ma_over_window = total / i; // i should = num_frames_in_window
				System.out.println("Endpoint.getAlertFrames(): there were enough frames. ma_over_window=" + ma_over_window);
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Endpoint testFrameForMovingAverage", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				if (rs2  != null){ rs2.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Endpoint testFrameForMovingAverage", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}   
		return ma_over_window; // if it reaches here, something has failed.
	}
*/
	
	JSONArray getAlertFrames(long begin_long, long end_long, int maw_int, double ma_modifier_double, double single_modifier_double, double delta_double)
	{
		System.out.println("Endpoint.getAlertFrames() begin");
		JSONArray alert_frames_ja = new JSONArray();
		JSONObject current_frame_jo = new JSONObject();
		try
		{
			TreeSet<String> reporters = getReporters();
			Connection con = null;
			Statement stmt = null;
			ResultSet rs = null;
			Statement stmt2 = null;
			ResultSet rs2 = null;
			long ts = 0L;
			double total = 0.0;
			double current_homogeneity;
			String current_designation;
			double ma_over_window = 0.0;
			try
			{
				Iterator<String> it = reporters.iterator();
				User currentreporter = null;
				while(it.hasNext()) // looping through reporters
				{
					currentreporter = new User(it.next(), "designation");
					System.out.println("Endpoint.getAlertFrames(): looping reporters. " + currentreporter.getDesignation());
					current_homogeneity = currentreporter.getHomogeneity();
					current_designation = currentreporter.getDesignation();
					con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
					stmt = con.createStatement();
					// get frames where this designation crosses the single frame threshold
					rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms >= " + (1000*begin_long) + " AND timestamp_in_ms <= " + (1000*end_long) + " AND " + current_designation + "_avg > " + (current_homogeneity * single_modifier_double) + ") ORDER BY timestamp_in_ms ASC");
					rs.last();
					System.out.println("Endpoint.getAlertFrames() found " + rs.getRow()  + " frames over threshold (" + current_homogeneity + " * " + single_modifier_double + "=" +  (current_homogeneity * single_modifier_double) + ") for " + currentreporter.getDesignation());
					rs.beforeFirst();
					TreeSet<Frame> frames_past_single_thresh = getFramesFromResultSet(rs);
					Iterator<Frame> frames_past_single_thresh_it = frames_past_single_thresh.iterator();
					Frame currentframe = null;
					Frame subsequentframe = null;
					Frame frame_that_passed = null;
					double moving_average = 0.0;
					while(frames_past_single_thresh_it.hasNext())
					{
						currentframe = frames_past_single_thresh_it.next();
						moving_average = currentframe.getMovingAverage(maw_int, current_designation);
						if(moving_average > (current_homogeneity * ma_modifier_double) && 
								moving_average == subsequentframe.getHighestMovingAverage(maw_int))
						{
							frame_that_passed = currentframe;
						}
						else // initial frame didn't pass, look for subsequent frames that pass the ma threshold.
						{
							System.out.println("Endpoint.getAlertFrames(): moving average DID NOT pass req threshold. ma_over_window=" + ma_over_window + " thresh=" + (current_homogeneity * ma_modifier_double) + " checking next mawindow_int -1 frames");
							stmt2 = con.createStatement();
							// get frames after the current ts within the maw_int window
							ts = currentframe.getTimestampInMillis();
							System.out.println("executing SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms > " + ts + " AND timestamp_in_ms <= " + (ts + 1000*maw_int) + ") ORDER BY timestamp_in_ms ASC");
							rs2 = stmt2.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms > " + ts + " AND timestamp_in_ms <= " + (ts + 1000*maw_int) + ") ORDER BY timestamp_in_ms ASC");
							rs2.last();
							System.out.println("Got " + rs2.getRow() + " subsequent frames.");
							rs2.beforeFirst();
							TreeSet<Frame> subsequent_frames = getFramesFromResultSet(rs2);
							Iterator<Frame> subsequent_frames_it = subsequent_frames.iterator();
							while(subsequent_frames_it.hasNext())
							{
								subsequentframe = subsequent_frames_it.next();
								moving_average = subsequentframe.getMovingAverage(maw_int, current_designation);
								if(moving_average > (current_homogeneity * ma_modifier_double) && 
										moving_average == subsequentframe.getHighestMovingAverage(maw_int))
								{
									frame_that_passed = subsequentframe;
									break;
								}
							}
							if(frame_that_passed != null) 
								break; // get out of the loop
						}
					}
					if(frame_that_passed != null) 
					{
						JSONObject jo2add = currentframe.getAsJSONObject(true);
						current_frame_jo.put("designation", current_designation);
						current_frame_jo.put("twitter_handle", currentreporter.getTwitterHandle());
						current_frame_jo.put("moving_average", ma_over_window);
						current_frame_jo.put("homogeneity", current_homogeneity);
						current_frame_jo.put("ma_threshold",  (current_homogeneity * ma_modifier_double));
						current_frame_jo.put("single_threshold", (current_homogeneity * single_modifier_double));
						alert_frames_ja.put(jo2add);
					}
				}
				rs.close();
				stmt.close();
				rs2.close();
				stmt2.close();
				con.close();
			}
			catch(SQLException sqle)
			{
				sqle.printStackTrace();
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Endpoint getAlertFrames", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
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
						se.sendMail("SQLException in Endpoint getAlertFrames", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
					} catch (MessagingException e) {
						e.printStackTrace();
					}
				}
			}   
		}
		catch(JSONException jsone)
		{
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		}	
		return alert_frames_ja;
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
	
	/*
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
	}*/

}
