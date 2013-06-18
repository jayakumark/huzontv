package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Frame implements Comparable<Frame> {

	long timestamp_in_ms;
	String image_name;
	String s3_location;
	String url;
	int frame_rate;
	String station;
	String[] reporter_designations;
	double[] reporter_avgs;
	JSONArray[] reporter_score_arrays;
	int[] reporter_nums;
	
	double[] reporter_moving_avgs; // starts out null until populated
	int maw_int; // since the moving average can be over any window, we have to keep track of the window used to calculate moving averages
	String max_ma_designation;
	String second_max_ma_designation;
	double max_ma;
	double second_max_ma;
	
	
	public Frame(long inc_timestamp_in_ms, String inc_image_name, String inc_s3_location,
			String inc_url, int inc_frame_rate, String inc_station, String[] inc_reporter_designations, 
			double[] inc_reporter_avgs, JSONArray[] inc_reporter_score_arrays, int[] inc_reporter_nums)
	{
		timestamp_in_ms = inc_timestamp_in_ms;
		image_name = inc_image_name;
		s3_location = inc_s3_location;
		url = inc_url;
		frame_rate = inc_frame_rate;
		station = inc_station;
		reporter_designations = inc_reporter_designations;
		reporter_avgs = inc_reporter_avgs;
		reporter_score_arrays = inc_reporter_score_arrays;
		reporter_nums = inc_reporter_nums;
	}
	
	
	public Frame(long inc_timestamp_in_ms, String inc_station)
	{
		timestamp_in_ms = inc_timestamp_in_ms;
		station = inc_station;
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM frames_" + station + " WHERE timestamp_in_ms=" + timestamp_in_ms); // get the frames in the time range
			
			// calculate the number of reporters by looping through all columns and looking for "_avg"
			ResultSetMetaData rsmd = rs.getMetaData();
			int columncount = rsmd.getColumnCount();
			int reportercount = 0;
			int x = 1; 
			while(x <= columncount)
			{
				if(rsmd.getColumnName(x).endsWith("_avg"))
				{
					reportercount++;
				}
				x++;
			}

			// now examine the row and extract all the information
			if(rs.next())
			{	
				image_name = rs.getString("image_name");
				s3_location = rs.getString("s3_location");
				url = rs.getString("url");
				frame_rate = rs.getInt("frame_rate");
				x = 1; 
				int reporter_index = 0;
				reporter_designations = new String[reportercount];
				reporter_avgs = new double[reportercount];
				reporter_score_arrays = new JSONArray[reportercount];
				reporter_nums = new int[reportercount];
				while(x <= columncount)
				{
					if(rsmd.getColumnName(x).endsWith("_avg"))
					{
						reporter_designations[reporter_index] = rsmd.getColumnName(x).substring(0,rsmd.getColumnName(x).indexOf("_avg"));
						reporter_avgs[reporter_index] = rs.getDouble(x);
					}
					else if(rsmd.getColumnName(x).endsWith("_scores"))
					{
						if(rs.getString(x) == null || rs.getString(x).isEmpty())
							reporter_score_arrays[reporter_index] = new JSONArray();
						else
							reporter_score_arrays[reporter_index] = new JSONArray(rs.getString(x));
					}
					else if(rsmd.getColumnName(x).endsWith("_num"))
					{
						reporter_nums[reporter_index] = rs.getInt(x);
						reporter_index++;
					}
					x++;
				}
			}
			else
			{
				timestamp_in_ms = 0;
			}
			rs.close();
			stmt.close();
			con.close();
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
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}   		
	}
	
	long getTimestampInMillis()
	{
		return timestamp_in_ms;
	}
	
	String getImageName()
	{
		return image_name;
	}

	String getS3Location()
	{
		return s3_location;
	}
	
	String getURL()
	{
		return url;
	}
	
	String getStation()
	{
		return station;
	}
	
	int getFrameRate()
	{
		return frame_rate;
	}
	
	int getNumReporters()
	{
		return reporter_designations.length;
	}
	
	String[] getReporterDesignations()
	{
		return reporter_designations;
	}
	
	double getScore(String reporter_designation)
	{
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(reporter_designation))
				return reporter_avgs[x];
			x++;
		}
		return 0;
	}
	
	boolean populateMovingAverages(int inc_maw_int)
	{
		//System.out.println("Frame.populateMovingAverages()");
		max_ma = 0;
		second_max_ma = 0;
		max_ma_designation = null;
		second_max_ma_designation = null;
		
		reporter_moving_avgs = new double[reporter_designations.length];
		maw_int = inc_maw_int;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs2 = null;
		double ma_over_window = 0;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs2 = stmt.executeQuery("SELECT * FROM frames_" + getStation() + " WHERE (timestamp_in_ms > " + (timestamp_in_ms - maw_int*1000) + " AND timestamp_in_ms <= " + timestamp_in_ms + ")");
			
			// 6/12/2013 simplified this function. To see old vers2ion, check github prior to this date.
			
			// so what we're doing here is we've got a single frame with a single score above the single thresh.
			// we want to check the moving average of this frame (going back maw_int*1000 milliseconds) to see if the ma is above its required thresh, too
			
			rs2.last();
			int num_frames_in_window = rs2.getRow();
			
			if(num_frames_in_window < maw_int) // only process this frame if there were enough prior frames to warrn
			{
				// NOT ENOUGH FRAMES (i.e. less than 1 per second) 
				reporter_moving_avgs = null;
				System.out.println("Frame.populateMovingAverages(): not enough frames in this moving average window (" + num_frames_in_window + " < " + maw_int + ")");
				rs2.close();
				stmt.close();
				con.close();
				return false;
			}
			else
			{
				for(int x = 0; x < reporter_designations.length; x++)
				{
					int i = 0; 
					double total = 0;
					rs2.beforeFirst();
					while(rs2.next()) // looping through all the frames in the moving average window before the current frame
					{
						total = total + rs2.getDouble(reporter_designations[x] + "_avg"); // the running total of the last maw_int frames
						i++;
					}
					ma_over_window = total / i; // i should = num_frames_in_window
					reporter_moving_avgs[x] = ma_over_window;
				}

				//double max_moving_average = 0;
				//double second_max_moving_average = 0;
				//String max_designation;
				//String second_max_designation;
				for(int x = 0; x < reporter_designations.length; x++)
				{
					if(reporter_moving_avgs[x] > max_ma)
					{
						second_max_ma = max_ma;
						max_ma = reporter_moving_avgs[x];
						second_max_ma_designation = max_ma_designation;
						max_ma_designation = reporter_designations[x];
					}
				}
				rs2.close();
				stmt.close();
				con.close();
				return true; // reporter_moving_avgs[], max_ma, second_max_ma, max_ma_designation, second_max_ma_designation should be set now.
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
		return false; // something went wrong along the way
	}
	
	
	double getMovingAverage(int inc_maw_int, String current_designation)
	{
		//System.out.println("Frame.getMovingAverage(" + inc_maw_int + "," + current_designation + ")");
		if(reporter_moving_avgs == null || maw_int != inc_maw_int)
		{
			boolean successfullypopulated = populateMovingAverages(inc_maw_int);
			if(!successfullypopulated)
			{
				System.out.println("******** Moving averages population unsuccessful. returning double value of -1");
				return -1;
			}
		}
			
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(current_designation))
				return reporter_moving_avgs[x];
			x++;
		}
		return 0;
	}
		
	double getHighestMovingAverage(int inc_maw_int)
	{
		//System.out.println("Frame.getHighestMovingAverage()");
		if(reporter_moving_avgs == null || maw_int != inc_maw_int)
		{
			boolean successfullypopulated = populateMovingAverages(inc_maw_int);
			if(!successfullypopulated)
			{
				System.out.println("******** Moving averages population unsuccessful. returning double value of -1");
				return -1;
			}
		}
		
		return max_ma;
	}
	
	String getHighestMovingAverageDesignation(int inc_maw_int)
	{
		//System.out.println("Frame.getHighestMovingAverageDesignation()");
		if(reporter_moving_avgs == null || maw_int != inc_maw_int)
		{
			boolean successfullypopulated = populateMovingAverages(inc_maw_int);
			if(!successfullypopulated)
			{
				System.out.println("******** Moving averages population unsuccessful. returning null");
				return null;
			}
		}
		
		return max_ma_designation;
	}
	
	double getSecondHighestMovingAverage(int inc_maw_int)
	{
		//System.out.println("Frame.getSecondHighestMovingAverage()");
		if(reporter_moving_avgs == null || maw_int != inc_maw_int)
		{
			boolean successfullypopulated = populateMovingAverages(inc_maw_int);
			if(!successfullypopulated)
			{
				System.out.println("******** Moving averages population unsuccessful. returning double value of -1");
				return -1;
			}
		}
		
		return second_max_ma;
	}
	
	String getSecondHighestMovingAverageDesignation(int inc_maw_int)
	{
		//System.out.println("Frame.getSecondHighestMovingAverageDesignation()");
		if(reporter_moving_avgs == null || maw_int != inc_maw_int)
		{
			boolean successfullypopulated = populateMovingAverages(inc_maw_int);
			if(!successfullypopulated)
			{
				System.out.println("******** Moving averages population unsuccessful. returning double value of 0.0");
				return null;
			}
		}
		
		return second_max_ma_designation;
	}
	
	/* return object as:
	 * 
	 * {
	 * 		image_name : image_name,
	 * 		s3_location: s3_location,
	 * 		...
	 * 		reporters:[
	 * 			designation: {
	 * 				designation: designation,
	 * 				score_avg: score_avg,
	 * 				num: num,
	 * 				moving_average: moving_average,
	 * 				scores: [ score1, score2, .... score3]
	 * 			},
	 * 			designation2: {
	 * 				designation: designation2,
	 * 				score_avg: score_avg,
	 * 				num: num,
	 * 				moving_average: moving_average,
	 * 				scores: [ score1, score2, .... score3]
	 * 			},
	 * 			...
	 * 		]
	 *	} 
	 *
	 */
	
	JSONObject getAsJSONObject(boolean get_score_data)
	{
		JSONObject jo = new JSONObject();
		try
		{
			jo.put("image_name", image_name);
			jo.put("s3_location", s3_location); 
			jo.put("url", url);
			jo.put("timestamp_in_ms", timestamp_in_ms);
			jo.put("frame_rate", frame_rate);
			jo.put("station", station);
			if(get_score_data)
			{	
				int x = 0;
				JSONObject reporter_jo = new JSONObject();
				JSONObject jo2 = null;
				while(x < reporter_designations.length)
				{
					jo2 = new JSONObject();
					jo2.put("designation", reporter_designations[x]);
					jo2.put("score_avg", reporter_avgs[x]);
					jo2.put("num", reporter_nums[x]);
					jo2.put("scores", reporter_score_arrays[x]);
					reporter_jo.put(reporter_designations[x],jo2);
					x++;
				}
				jo.put("reporters", reporter_jo);
			}
		}
		catch(JSONException jsone)
		{
			jsone.printStackTrace();
		}
		return jo;
	}
	
	public int compareTo(Frame o) // this sorts by ts
	{
	    Long othertimestamp = ((Frame)o).getTimestampInMillis();
	    int x = othertimestamp.compareTo(timestamp_in_ms);
	    if(x >= 0) // this is to prevent equals
	    	return -1;
	    else
	    	return 1;
	}
	
}
