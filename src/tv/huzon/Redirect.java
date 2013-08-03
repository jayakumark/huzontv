package tv.huzon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Redirect implements java.lang.Comparable<Redirect> {

	long id;
	long timestamp_in_ms;
	String timestamp_hr;
	long alert_id;
	Timestamp redirect_timestamp;
	String ip_address;
	String designation;
	String station;
	String referrer;
	String user_agent;
	String ultimate_destination;
	
	private DataSource datasource;
	
	public Redirect(long inc_id, String inc_station)
	{
		try {
			Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
			datasource = (DataSource) envCtx.lookup("jdbc/huzondb");
		}
		catch (NamingException e) {
			e.printStackTrace();
		}
		
		id = inc_id;
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM redirects_" + inc_station + " WHERE id=" + inc_id); // get the frames in the time range
			
			if(rs.next())
			{	
				timestamp_in_ms = rs.getLong("timestamp_in_ms");
				timestamp_hr = rs.getString("timestamp_hr");
				alert_id = rs.getLong("alert_id");
				redirect_timestamp = rs.getTimestamp("redirect_timestamp");
				ip_address = rs.getString("ip_address");
				designation = rs.getString("designation");
				station = rs.getString("station");
				referrer = rs.getString("referrer");
				user_agent = rs.getString("user_agent");
				ultimate_destination = rs.getString("ultimate_destination");
			}
			else
			{
				id = -1L; // indicates failed lookup
				(new Platform()).addMessageToLog("Error in Redirect.constructor: Failed lookup for id=" + inc_id);
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Redirect.constructor: Error getting table row. message=" +sqle.getMessage());
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
	
	long getTimestampInMillis()
	{
		return timestamp_in_ms;
	}
	
	Timestamp getTimestamp()
	{
		return redirect_timestamp;
	}
	
	long getAgeInMillis()
	{
		return (new Date().getTime() - getTimestampInMillis());
	}
	
	String getDesignation()
	{
		return designation;
	}
	
	String getStation()
	{
		return station;
	}
	
	String getUltimateDestination()
	{
		return ultimate_destination;
	}
	
	public int compareTo(Redirect o) // this sorts by designation alphabetically
	{
	    Timestamp othertimestmap = ((Redirect)o).getTimestamp();
	    int x = othertimestmap.compareTo(redirect_timestamp);
	    if(x >= 0) // this is to prevent equals
	    	return 1;
	    else
	    	return -1;
	}
	
}
