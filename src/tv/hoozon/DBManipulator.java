package tv.hoozon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DBManipulator {

	/**
	 * @param args
	 */
	
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		DBManipulator dbm = new DBManipulator();
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://localhost/hoozon?user=root&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM frames_wkyt");
			JSONArray designations = dbm.getDesignations("wkyt");
			JSONArray all_scores_ja = null;
			JSONArray tempscores_ja = null;
			JSONArray all_deviations_ja = null;
			JSONArray all_deviation_squares_ja = null;
			double sum_of_deviation_squares = 0.0;
			double stddev_double = 0.0;
			double total_score = 0.0;
			double average_of_all_scores = 0.0;
			while(rs.next()) // get one row
			{
				all_scores_ja = new JSONArray(); // empty the scores array as we're starting to analyze a new row
				// 1. get all scores into an array
				for(int d = 0; d < designations.length(); d++) // for each designation, get its _scores array
				{	
					if(rs.getString(designations.getString(d) + "_scores") != null) // but not if it's null
					{	
						tempscores_ja = new JSONArray(rs.getString(designations.getString(d) + "_scores")); // get the _scores array for this designation									
						for(int i = 0; i < tempscores_ja.length(); i++) // loop through this _scores array for this designation and add to the full ja
						{
							all_scores_ja.put(tempscores_ja.getDouble(i));
						}
					}
				}
				//all_scores_ja is now a jsonarray of every score for this row
				System.out.println(all_scores_ja);
				total_score = 0;
				for(int s = 0; s < all_scores_ja.length(); s++)
				{
					total_score = total_score + all_scores_ja.getDouble(s);
				}
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
				rs.updateRow();
			}
		}
		catch(SQLException sqle)
		{
			System.out.println("Error: There was a problem attempting to insert the scores into the database. sqle.getMessage()=" + sqle.getMessage());
			sqle.printStackTrace();
		}
		catch(JSONException jsone)
		{
			System.out.println("{ \"error\": { \"message\": \"JSONException caught in Endpoint\" } }");
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
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
				System.out.println("There was a problem closing the resultset, statement and/or connection to the database.");
			}
		}   	
		
	}

}
