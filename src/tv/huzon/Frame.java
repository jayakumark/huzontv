package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeSet;

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
	String[] reporter_score_arrays;
	int[] reporter_nums; 
	
	public Frame(long inc_timestamp_in_ms, String inc_image_name, String inc_s3_location,
			String inc_url, int inc_frame_rate, String inc_station, String[] inc_reporter_designations, 
			double[] inc_reporter_avgs, String[] inc_reporter_score_arrays, int[] inc_reporter_nums)
	{
		timestamp_in_ms = inc_timestamp_in_ms;
		image_name = inc_image_name;
		s3_location = inc_s3_location;
		url = inc_url;
		frame_rate = inc_frame_rate;
		station = inc_station;
		int x = 0;
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
			if(rs.next())
			{	
				image_name = rs.getString("image_name");
				s3_location = rs.getString("s3_location");
				url = rs.getString("url");
				frame_rate = rs.getInt("frame_rate");
				ResultSetMetaData rsmd = rs.getMetaData();
				int columncount = rsmd.getColumnCount();
				int x = 1; int reporter_index = 0;
				while(x <= columncount)
				{
					if(rsmd.getColumnName(x).endsWith("_avg"))
					{
						reporter_designations[reporter_index] = rsmd.getColumnName(x).substring(0,rsmd.getColumnName(x).indexOf("_avg"));
						reporter_avgs[reporter_index] = rs.getDouble(x);
						reporter_index++;
					}
					else if(rsmd.getColumnName(x).endsWith("_scores"))
					{
						reporter_score_arrays[reporter_index] = rs.getString(x);
					}
					else if(rsmd.getColumnName(x).endsWith("_num"))
					{
						reporter_nums[reporter_index] = rs.getInt(x);
					}
					x++;
				}
			}
			else
			{
				// error. This frame didn't exist in the specified table
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
	
	/* return object as:
	 * 
	 * {
	 * 		image_name : image_name,
	 * 		s3_location: s3_location,
	 * 		...
	 * 		reporters:[
	 * 			{
	 * 				designation: designation,
	 * 				score_avg: score_avg,
	 * 				num: num,
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
				JSONArray ja = new JSONArray();
				JSONObject jo2 = null;
				while(x < reporter_designations.length)
				{
					jo2 = new JSONObject();
					jo2.put("designation", reporter_designations[x]);
					jo2.put("score_avg", reporter_avgs[x]);
					jo2.put("num", reporter_nums[x]);
					jo2.put("scores", new JSONArray(reporter_score_arrays[x]));
					ja.put(jo2);
					x++;
				}
				jo.put("reporters", ja);
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
