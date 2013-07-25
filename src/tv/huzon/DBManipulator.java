package tv.huzon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class DBManipulator {

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		DataSource datasource;
		
		try {
			Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
			datasource = (DataSource) envCtx.lookup("jdbc/huzondb");
		
		
			ResultSet rs = null;
			Connection con = null;
			Statement stmt = null;
			try
			{
				con = datasource.getConnection();
				stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				String query = "SELECT * FROM alerts";
				rs = stmt.executeQuery(query);
				Timestamp currenttimestamp = null;
				long timestamp_in_ms = 0L;
				Calendar cal = Calendar.getInstance();
				int x = 0;
				while(rs.next())
				{
					currenttimestamp = rs.getTimestamp("creation_timestamp");
					System.out.println("Processing timestamp=" + currenttimestamp);
					timestamp_in_ms = currenttimestamp.getTime();
					cal.setTimeInMillis(timestamp_in_ms); // we know that the most recent image has a timestamp of right now. It can't "survive" there for more than a few seconds
					//cal.add(Calendar.HOUR_OF_DAY, -4); // convert UTC to eastern
					// make the filename human-readable
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
					String ms = new Long(timestamp_in_ms%1000).toString();
					if(ms.length() == 1) { ms = "00" + ms;} 
					if(ms.length() == 2) { ms = "0" + ms;} 
					String timestamp_hr = year  + month + day + "_" + hour24 + minute + second + "_" + ms;		
					System.out.println(timestamp_hr);
					rs.updateString("timestamp_hr", timestamp_hr);
					rs.updateLong("timestamp_in_ms", timestamp_in_ms);
					rs.updateRow();
					x++;
				} 
			}
			catch(SQLException sqle)
			{
				System.out.println("Error: There was a problem attempting to insert the scores into the database. sqle.getMessage()=" + sqle.getMessage());
				sqle.printStackTrace();
			}
			/*catch(JSONException jsone)
			{
				System.out.println("{ \"error\": { \"message\": \"JSONException caught in Endpoint\" } }");
				System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
			}*/	
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
					System.out.println("There was a problem closing the resultset, statement and/or connection to the database.");
				}
			}   	
		}
		catch (NamingException e) {
			e.printStackTrace();
		}
		
	}

}