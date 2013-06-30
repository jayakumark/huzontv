package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Alert {

	long id;
	String social_type;
	String image_url;
	Timestamp creation_timestamp;
	String designation;
	String station_str;
	String actual_text;
	String social_item_id;
	
	public Alert(long inc_id)
	{
		try {
	        Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		id = inc_id;
		String dbName = System.getProperty("RDS_DB_NAME"); 
		String userName = System.getProperty("RDS_USERNAME"); 
		String password = System.getProperty("RDS_PASSWORD"); 
		String hostname = System.getProperty("RDS_HOSTNAME");
		String port = System.getProperty("RDS_PORT");
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id=" + inc_id); // get the frames in the time range
			
			// calculate the number of reporters by looping through all columns and looking for "_avg"
			if(rs.next())
			{	
				social_type = rs.getString("social_type");
				image_url = rs.getString("image_url");
				creation_timestamp = rs.getTimestamp("creation_timestamp");
				designation = rs.getString("designation");
				station_str = rs.getString("station");
				actual_text = rs.getString("actual_text");
				social_item_id = rs.getString("social_item_id");
			}
			else
			{
				id = -1L; // indicates failed lookup
				(new Platform()).addMessageToLog("SQLException in Alert.constructor: Failed lookup for id=" + inc_id);
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Alert.constructor: Error getting table row. message=" +sqle.getMessage());
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
	
	long getID()
	{
		return id;
	}
	
	String getSocialType()
	{
		return social_type;
	}

	String getImageURL()
	{
		return image_url;
	}
	
	Timestamp getTimestamp()
	{
		return creation_timestamp;
	}
	
	String getDesignation()
	{
		return designation;
	}
	
	String getStation()
	{
		return station_str;
	}
	
	String getActualText()
	{
		return actual_text;
	}
	
	String getSocialItemID()
	{
		return social_item_id;
	}
}
