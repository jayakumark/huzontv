package tv.hoozon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
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
		response.setHeader("Access-Control-Allow-Origin","*"); //FIXME
		PrintWriter out = response.getWriter();
		JSONObject jsonresponse = new JSONObject();
		Calendar tempcal = Calendar.getInstance();
		long timestamp_at_entry = tempcal.getTimeInMillis();
		try
		{
			// FIXME // this should eventually be secure to protect info. leaving insecure for now due to testing
			/*if(!request.isSecure())
			{
				jsonresponse.put("message", "The hoozon.tv API endpoint must be communicated with securely.");
				jsonresponse.put("response_status", "error");
			}
			else
			{*/	
				String method = request.getParameter("method");
				if(method == null)
				{
					jsonresponse.put("message", "Method not specified. This should probably produce HTML output reference information at some point.");
					jsonresponse.put("response_status", "error");
				}
				else if (method.equals("commitFrameDataAndAlert"))
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
						ResultSet rs = null;
						Connection con = null;
						Statement stmt = null;
						try
						{
							con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
							stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
							rs = stmt.executeQuery("SELECT * FROM frames_" + jo.getString("station") + " WHERE timestamp_in_seconds='" + jo.getLong("timestamp_in_seconds") + "' limit 0,1");
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
								System.out.println("image_name: " + jo.getString("image_name"));
								rs.updateString("image_name", jo.getString("image_name"));
								rs.updateLong("timestamp_in_seconds", jo.getLong("timestamp_in_seconds"));
								
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
								JSONObject frame_processing_jo = processNewFrame(jo.getLong("timestamp_in_seconds"), "wkyt", 5, 0.67, 1.0, 3600);
								if(frame_processing_jo.getString("response_status").equals("error") && frame_processing_jo.has("error_code") && (frame_processing_jo.getString("error_code").equals("100")))
								{
									// missing a frame, wait 2 seconds, try again.
									Thread.sleep(2000);
									frame_processing_jo = processNewFrame(jo.getLong("timestamp_in_seconds"), "wkyt", 5, 0.67, 1.0, 3600);
								}
								if(frame_processing_jo.has("alert_triggered") && frame_processing_jo.getString("alert_triggered").equals("yes"))
								{
									jsonresponse.put("alert_triggered", "yes");
									
									jsonresponse.put("alert_designation", frame_processing_jo.getString("designation"));
									if(frame_processing_jo.has("twitter_handle"))
									{
										jsonresponse.put("alert_twitter_handle", frame_processing_jo.getString("twitter_handle"));
										
										// FIXME!!!! This is just for testing the tat and tats retrieval by the remote daemon
										JSONObject twitter_stuff = getUserTwitterAccessTokenAndSecret("wkyt","hoozon_master");
										//JSONObject facebook_stuff = getUserFacebookAccessToken
										if(twitter_stuff.has("response_status") && twitter_stuff.getString("response_status").equals("success")
												&& twitter_stuff.has("twitter_access_token") && !twitter_stuff.getString("twitter_access_token").isEmpty()
												&& twitter_stuff.has("twitter_access_token_secret") && !twitter_stuff.getString("twitter_access_token_secret").isEmpty())
										{
											jsonresponse.put("twitter_access_token",twitter_stuff.getString("twitter_access_token"));
											jsonresponse.put("twitter_access_token_secret",twitter_stuff.getString("twitter_access_token_secret"));
										}
									}
									jsonresponse.put("alert_display_name", frame_processing_jo.getString("display_name"));
								    
									/*
									SimpleEmailer se = new SimpleEmailer();
									try {
										se.sendMail("Alert fired for " + frame_processing_jo.getString("designation"), "http://hoozon-wkyt-alertimgs.s3-website-us-east-1.amazonaws.com/" + jo.getString("image_name"), "cyrus7580@gmail.com", "info@crasher.com");
										jsonresponse.put("message", "Scores entered. Alert email sent.");
									} catch (MessagingException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
										System.out.println("failed to send alert message=" + e.getMessage());
										jsonresponse.put("message", "Scores entered. Exception trying to send mail, though. message=" + e.getMessage());
									}*/
								}
								else
								{
									jsonresponse.put("alert_triggered", "no");
									jsonresponse.put("message", "Scores entered. No alert.");
								} 
								
							}
							/*else
							{
								jsonresponse.put("response_status", "error");
								jsonresponse.put("alert_triggered", "no");
								jsonresponse.put("message", "Duplicate frame.");
							}*/
							else // a row already exists... overwrite.
							{
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
								System.out.println("image_name: " + jo.getString("image_name"));
								rs.updateString("image_name", jo.getString("image_name"));
								rs.updateLong("timestamp_in_seconds", jo.getLong("timestamp_in_seconds"));
								
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
								rs.close();
								stmt.close();
								con.close();
								jsonresponse.put("response_status", "success");
								jsonresponse.put("alert_triggered", "no");
								jsonresponse.put("message", "Scores should be UPDATED now.");
							}
						}
						catch(SQLException sqle)
						{
							jsonresponse.put("message", "There was a problem attempting to insert the scores into the database. sqle.getMessage()=" + sqle.getMessage());
							jsonresponse.put("response_status", "error");
							sqle.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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
		//	}
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
		response.setHeader("Access-Control-Allow-Origin","*"); //FIXME
		PrintWriter out = response.getWriter();
		JSONObject jsonresponse = new JSONObject();
		Calendar tempcal = Calendar.getInstance();
		long timestamp_at_entry = tempcal.getTimeInMillis();
		try
		{
			if(!request.isSecure())
			{
				jsonresponse.put("message", "The hoozon.tv API endpoint must be communicated with securely.");
				jsonresponse.put("response_status", "error");
			}
			else
			{	
				String method = request.getParameter("method");
				if(method == null)
				{
					jsonresponse.put("message", "Method not specified. This should probably produce HTML output reference information at some point.");
					jsonresponse.put("response_status", "error");
				}
				else if (method.equals("checkPassword"))
				{
					String password = request.getParameter("password");
					if(password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(password.equals("firstnews72"))
						{
							jsonresponse.put("response_status", "success");
						}
						else
						{
							jsonresponse.put("message", "Incorrect password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("startTwitterAuthentication"))
				{
					String password = request.getParameter("hoozon_auth");
					if(password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(password.equals("firstnews72"))
						{
							String designation = request.getParameter("designation");
							if(designation == null)
							{
								// check for designation validity FIXME
								jsonresponse.put("message", "Please specify a valid designation");
								jsonresponse.put("response_status", "error");
							}
							else
							{	
								Twitter twitter = new Twitter();
								jsonresponse = twitter.startTwitterAuthentication();
								/*if(jsonresponse.getString("response_status").equals("success"))
								{
									setOAuthToken("wkyt", designation, jsonresponse.getString("oauth_token"));
								}*/
							}
						}
						else
						{
							jsonresponse.put("message", "Incorrect password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("getTwitterAccessTokenFromAuthorizationCode"))
				{
					String password = request.getParameter("hoozon_auth");
					if(password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(password.equals("firstnews72"))
						{
							String oauth_verifier = request.getParameter("oauth_verifier");
							String oauth_token = request.getParameter("oauth_token");
							String designation = request.getParameter("designation");
							if(oauth_verifier == null)
							{
								jsonresponse.put("message", "This method requires an oauth_verifier value.");
								jsonresponse.put("response_status", "error");
							}
							else if(oauth_token == null)
							{
								jsonresponse.put("message", "This method requires an oauth_token value.");
								jsonresponse.put("response_status", "error");
							}
							else
							{	
								// at this point the user has been sent to twitter with an initial request_token. They've been sent back
								// to the registration page with an oauth_token and oauth_verifier. However, as they come back, we don't know who they are 
								// except for this oauth_token we've seen before. Therefore, we've saved it above in startTwitterAuthentication
								// and we need to search the DB for it to figure out who it belongs to.
								//String designation = getDesignationFromOAuthToken("wkyt", oauth_token);
								
								String expected_twitter_handle = getTwitterHandle("wkyt", designation);
								Twitter twitter = new Twitter();
								JSONObject preliminary_jsonresponse = new JSONObject();
								preliminary_jsonresponse = twitter.getTwitterAccessTokenFromAuthorizationCode(oauth_verifier, oauth_token);
								if(preliminary_jsonresponse.getString("response_status").equals("success"))
								{
									if(!preliminary_jsonresponse.getString("screen_name").equals(expected_twitter_handle))
									{
										jsonresponse.put("message", "The screen name returned by Twitter (" + preliminary_jsonresponse.getString("screen_name") + ") did not match the expected value: " + expected_twitter_handle);
										jsonresponse.put("response_status", "error");
									}
									else
									{
										setTwitterAccessTokenAndSecret("wkyt", designation, preliminary_jsonresponse.getString("access_token"), preliminary_jsonresponse.getString("access_token_secret"));
										jsonresponse.put("response_status", "success");
										jsonresponse.put("message", "The access_token and access_token_secret should be set in the database now.");
									}
								}
								else
								{
									jsonresponse = preliminary_jsonresponse;
								}
							}
						}
						else
						{
							jsonresponse.put("message", "Incorrect password.");
							jsonresponse.put("response_status", "error");
						}
					}					
				}
				else if (method.equals("getFacebookAccessTokenFromAuthorizationCode"))
				{
					String password = request.getParameter("hoozon_auth");
					if(password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(password.equals("firstnews72"))
						{
							String facebook_code = request.getParameter("facebook_code");
							String designation = request.getParameter("designation");
							if(facebook_code == null)
							{
								jsonresponse.put("message", "This method requires a facebook_code value.");
								jsonresponse.put("response_status", "error");
							}
							else if(designation == null)
							{
								jsonresponse.put("message", "This method requires a designation value.");
								jsonresponse.put("response_status", "error");
							}
							else
							{	
								// at this point the user has been sent to facebook for permission. The response came back with a code.
								// Now we need to get and store the user's access_token
								JSONObject preliminary_jsonresponse = new JSONObject();
								preliminary_jsonresponse = getFacebookAccessTokenFromAuthorizationCode(facebook_code);
								
								if(preliminary_jsonresponse.getString("response_status").equals("success"))
								{
									JSONObject fb_profile_jo = getFacebookProfile("wkyt", preliminary_jsonresponse.getString("access_token"));
									long fb_uid = 0L;
									try
									{
										if(fb_profile_jo != null && fb_profile_jo.has("id"))
										{
											fb_uid = fb_profile_jo.getLong("id");
											Calendar cal = Calendar.getInstance();
											cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
											cal.add(Calendar.SECOND, Integer.parseInt( preliminary_jsonresponse.getString("expires")));
											long expires_timestamp = cal.getTimeInMillis() / 1000;
											boolean successful = setFacebookAccessTokenExpiresAndUID("wkyt", designation, preliminary_jsonresponse.getString("access_token"), expires_timestamp, fb_uid);
											if(successful)
											{	
												jsonresponse.put("response_status", "success");
												jsonresponse.put("message", "The access_token, expires and uid should be set in the database now.");
											}
											else
											{
												jsonresponse.put("message", "encountered error attempting to update the database with the 3 fb values");
												jsonresponse.put("response_status", "error");
											}
										}
										else
										{
											jsonresponse.put("message", "fb profile didn't have id field");
											jsonresponse.put("response_status", "error");
										}
									}
									catch(NumberFormatException nfe)
									{
										jsonresponse.put("message", "Number format exception for expires=" + preliminary_jsonresponse.getString("expires") + " or for fb profile id value full preliminary_jsonresponse=" + preliminary_jsonresponse);
										jsonresponse.put("response_status", "error");
									}
									/*try
									{
										String fbat = getFacebookAccessToken("wkyt", designation);
										if(fbat != null)
											jsonresponse.put("facebook_access_token", fbat);
										else
											jsonresponse.put("facebook_access_token", "was null");
										JSONObject fb_profile_jo = getFacebookProfile("wkyt", designation);
										if(fb_profile_jo != null)
											jsonresponse.put("facebook_profile", fb_profile_jo);
										else
											jsonresponse.put("facebook_profile", "was null");
									
										jsonresponse.put("response_from_facebook", preliminary_jsonresponse.getString("response_from_facebook"));
										
									}
									catch(NumberFormatException nfe)
									{
										jsonresponse.put("message", "Number format exception for expires=" + preliminary_jsonresponse.getString("expires") + " full preliminary_jsonresponse=" + preliminary_jsonresponse);
										jsonresponse.put("response_status", "error");
									}*/
								}
								else
								{
									jsonresponse = preliminary_jsonresponse; // just return the error
								}
							}
						}
						else
						{
							jsonresponse.put("message", "Incorrect password.");
							jsonresponse.put("response_status", "error");
						}
					}		
				}
				else if (method.equals("getUserTwitterAcesssTokenAndSecret"))
				{	
					// FIXME this method needs some sort of protection against hacking (i.e. an ssl layer and an administrator password or something
					String designation = request.getParameter("designation");
					if(designation == null)
					{
						jsonresponse.put("message", "This method requires a designation value.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						jsonresponse = getUserTwitterAccessTokenAndSecret("wkyt", designation);
					}
				}
				else if (method.equals("setFacebookAccountInfo")) // sets the designated journalist page for this user
				{
					String designation = request.getParameter("designation");
					String id = request.getParameter("id");
					if(designation == null)
					{
						jsonresponse.put("message", "This method requires a designation value.");
						jsonresponse.put("response_status", "error");
					}
					else if(id == null)
					{
						jsonresponse.put("message", "This method requires an id value for the journalist page.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						JSONArray fbaccounts_ja = getFacebookAccounts("wkyt", getFacebookAccessToken("wkyt",designation));
						String name = "";
						String account_access_token = "";
						long id_long = 0L;
						boolean successful = false;
						for(int x =0; x < fbaccounts_ja.length(); x++)
						{
							if(fbaccounts_ja.getJSONObject(x).getLong("id") == (new Long(Long.parseLong(id)).longValue()))
							{
								name = fbaccounts_ja.getJSONObject(x).getString("name");
								account_access_token = fbaccounts_ja.getJSONObject(x).getString("access_token");
								id_long =  (new Long(Long.parseLong(id)).longValue());
								successful = setFacebookAccountIdNameAndAccessToken("wkyt", designation, id_long, name, account_access_token);
							}
						}
						if(successful)
						{
							jsonresponse.put("response_status", "success");
						}
						else
						{
							jsonresponse.put("message", "There was an error trying to set the FB account id name and access_token for this user");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("getFrames"))
				{
					String admin_password = request.getParameter("hoozon_admin_auth");
					if(admin_password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A admin_password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(admin_password.equals("sanders.lov"))
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
									con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
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
										current_frame_jo.put("image_name", rs.getString("image_name"));
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
						else
						{
							jsonresponse.put("message", "Incorrect admin_password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("getMissingFrames"))
				{
					String admin_password = request.getParameter("hoozon_admin_auth");
					if(admin_password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A admin_password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(admin_password.equals("sanders.lov"))
						{
							System.out.println("Getting missing frames");
							String begin = request.getParameter("begin"); // required
							String end = request.getParameter("end"); // required
							if(begin == null || begin.isEmpty() || end == null || end.isEmpty()) // must always have the time range 
							{
								jsonresponse.put("message", "begin or end was empty.");
								jsonresponse.put("response_status", "error");
							}
							else
							{
								long begin_long = Long.parseLong(begin);
								long end_long = Long.parseLong(end);
								long counter = begin_long;
								System.out.println("Putting all timestamps in window in treeset");
								TreeSet<String> timestamps = new TreeSet<String>();
								while(counter < end_long)
								{
									timestamps.add(counter+"");
									counter++;
								}
								System.out.println("DONE Putting all timestamps in window in treeset");
								ResultSet rs = null;
								Connection con = null;
								Statement stmt = null;
								int x = 0;
								double total = 0; 
								System.out.println("1");
								try
								{
									con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
									System.out.println("2");
									stmt = con.createStatement();
									System.out.println("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + ") ORDER BY timestamp_in_seconds ASC");
									rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + ") ORDER BY timestamp_in_seconds ASC"); // get the frames in the time range
									System.out.println("4");
									rs.last();
									System.out.println("5");
									jsonresponse.put("frames_processed", rs.getRow());  // get a row count
									System.out.println("6");
								 	rs.beforeFirst(); // go back to the beginning for parsing
									System.out.println("Looping through frames found in database and removing entries from timestamps treeset");
									while(rs.next())
									{
										timestamps.remove(rs.getLong("timestamp_in_seconds")+"");
									}
									System.out.println("DONE Looping through frames found in database and removing entries from timestamps treeset");
									Iterator<String> it = timestamps.iterator();
									JSONArray timestamps_ja = new JSONArray();
									JSONArray datestrings_ja = new JSONArray();
									String current = "";
									System.out.println("Looping through missing frames and creating jsonarrays");
									while(it.hasNext())
									{
										current = it.next();
										timestamps_ja.put(current);
										datestrings_ja.put(getDatestringFromTimestampInSeconds(Long.parseLong(current)));
									}
									System.out.println("DONE Looping through frames found in database and removing entries from timestamps treeset");
									jsonresponse.put("missing_frames_timestamps", timestamps_ja);
									jsonresponse.put("missing_frames_datestrings", datestrings_ja);
									jsonresponse.put("response_status", "success");
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
						else
						{
							jsonresponse.put("message", "Incorrect admin_password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("getFramesByDesignation"))
				{
					String admin_password = request.getParameter("hoozon_admin_auth");
					if(admin_password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A admin_password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(admin_password.equals("sanders.lov"))
						{
							String begin = request.getParameter("begin"); // required
							String end = request.getParameter("end"); // required
							String designation = request.getParameter("designation");
							double ma_modifier_double = (new Double(request.getParameter("mamodifier"))).doubleValue();
							double single_modifier_double = (new Double(request.getParameter("singlemodifier"))).doubleValue();
							//int alert_waiting_period = (new Integer(request.getParameter("awp"))).intValue();
							String mawindow = request.getParameter("mawindow");
							int mawindow_int = Integer.parseInt(mawindow);
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
								Statement stmt2 = null;
								ResultSet rs2 = null;
								try
								{
									con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
									stmt = con.createStatement();
									rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + ")"); // get the frames in the time range
									rs.last();
									jsonresponse.put("frames_processed", rs.getRow());  // get a row count
									rs.beforeFirst(); // go back to the beginning for parsing
									JSONObject current_frame_jo = null;
									JSONArray frames_ja = new JSONArray();
									//JSONArray alertframes_ja = new JSONArray();
									//int x = 0;
									int y = 0;
									//double sum_of_last_few_seconds = 0.0;
									double total = 0.0;
									double ma_over_window = 0.0;
									double homogeneity_double = getHomogeneityScore("wkyt",designation);
									double lowest_score_in_window = 2.0;
									//int seconds_to_average_int = Integer.parseInt(mawindow);
									//int frames_since_last_alert = alert_waiting_period + 1; // this makes it so an alert can go out immediately
									while(rs.next())
									{
										current_frame_jo = new JSONObject();
										current_frame_jo.put("image_name", rs.getString("image_name"));
										current_frame_jo.put("timestamp_in_seconds", rs.getInt("timestamp_in_seconds"));
										current_frame_jo.put("designation_score", rs.getDouble(designation + "_avg"));
										current_frame_jo.put("homogeneity_score", homogeneity_double);
										current_frame_jo.put("ma_threshold", homogeneity_double * ma_modifier_double);
										current_frame_jo.put("single_threshold", homogeneity_double * single_modifier_double);
										stmt2 = con.createStatement();
										rs2 = stmt2.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds > " + (rs.getInt("timestamp_in_seconds") - mawindow_int) + " AND timestamp_in_seconds <= " + rs.getInt("timestamp_in_seconds") + ")");
										total = 0;
										lowest_score_in_window = 2.0;
										while(rs2.next())
										{
											/** this removes the lowest score over the window, essentially throwing out an outlier to compensate for closed eyes, 
											 * or other total failure to locate the face in an otherwise good stream of consistent images **/
											if(rs2.getDouble(designation + "_avg") < lowest_score_in_window)
												lowest_score_in_window = rs2.getDouble(designation + "_avg");
											total = total + rs2.getDouble(designation + "_avg");
										}
										ma_over_window = (total - lowest_score_in_window) / (mawindow_int - 1);
										/*y=0; sum_of_last_few_seconds = 0.0;
										while(y < seconds_to_average_int)
										{
											if(y == 0)
												rs.getDouble(designation + "_avg");
											else
											{	
												if((x-y) >= 0)
												{	
													sum_of_last_few_seconds = sum_of_last_few_seconds + frames_ja.getJSONObject(x-y).getDouble("designation_score");
												}
											}
											y++;
										}*/
										current_frame_jo.put("moving_average", ma_over_window);
										frames_ja.put(current_frame_jo);
										
										/*if((sum_of_last_few_seconds / seconds_to_average_int) > (homogeneity_double * ma_modifier_double) && // the moving average is greater than the moving average threshold
												frames_since_last_alert > alert_waiting_period &&  // it has been at least alert_waiting_period second since last alert
												rs.getDouble(designation + "_avg") > (homogeneity_double * single_modifier_double)) // this frame's raw singular average is greater than single threshold
										{
											alertframes_ja.put(current_frame_jo);
											frames_since_last_alert = 0;
										}
										frames_since_last_alert++;*/
										//x++;
									}
									jsonresponse.put("response_status", "success");
									if(frames_ja.length() > 0)
										jsonresponse.put("frames", frames_ja);
									//if(alertframes_ja.length() > 0)
									//	jsonresponse.put("alertframes", alertframes_ja);
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
										if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (rs2  != null){ rs2.close(); } if (stmt2  != null) { stmt2.close(); } if (con != null) { con.close(); }
									}
									catch(SQLException sqle)
									{ jsonresponse.put("warning", "Problem closing resultset, statement and/or connection to the database."); }
								}   	
							}
						}
						else
						{
							jsonresponse.put("message", "Incorrect admin_password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("getFramesByDesignationAndHomogeneityThreshold"))
				{	
					String admin_password = request.getParameter("hoozon_admin_auth");
					if(admin_password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A admin_password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(admin_password.equals("sanders.lov"))
						{
							String begin = request.getParameter("begin"); // required
							String end = request.getParameter("end"); // required
							if(begin == null || begin.isEmpty() || end == null || end.isEmpty()) // must always have the time range 
							{
								jsonresponse.put("message", "begin and/or end was null and/or empty.");
								jsonresponse.put("response_status", "error");
							}
							else // begin/end ok
							{	
								String designation = request.getParameter("designation"); 
								if(designation == null || designation.isEmpty())
								{	
									jsonresponse.put("message", "designation was null or empty");
									jsonresponse.put("response_status", "error");
								}
								else // designation/numstddev ok
								{
									double homogeneity_double = getHomogeneityScore("wkyt",designation);
									double modifier_double = (new Double(request.getParameter("singlemodifier"))).doubleValue();
									double threshold = homogeneity_double * modifier_double;
									double delta_double = (new Double(request.getParameter("delta"))).doubleValue();
									try
									{
										ResultSet rs = null;
										Connection con = null;
										Statement stmt = null;
										try
										{
											
											con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
											stmt = con.createStatement();
											rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds < " + end + " AND timestamp_in_seconds > " + begin + " AND " + designation + "_avg > " + threshold + ")"); // get the frames in the time range
											rs.last();
											jsonresponse.put("frames_processed", rs.getRow());  // get a row count
											rs.beforeFirst(); // go back to the beginning for parsing
											JSONObject current_frame_jo = null;
											JSONArray frames_ja = new JSONArray();
											boolean all_others_below_delta = true;
											double control_avg = 100;
											double closest_avg = 0;
											JSONArray designations = getDesignations("wkyt");
											double challenge_avg = 0;
											String closest_designation = "";
											int delta_suppressions = 0;
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
													current_frame_jo.put("image_name", rs.getString("image_name"));
													current_frame_jo.put("timestamp_in_seconds", rs.getInt("timestamp_in_seconds"));
													current_frame_jo.put("score_average", rs.getDouble(designation + "_avg"));
													current_frame_jo.put("homogeneity_score", homogeneity_double);
													current_frame_jo.put("threshold", threshold);
													current_frame_jo.put("closest_avg", closest_avg);
													current_frame_jo.put("closest_designation", closest_designation);
													//System.out.println("Adding a frame. " + rs.getString("image_name") + " " + rs.getInt("timestamp_in_seconds") + " " + rs.getDouble(designation + "_avg"));
													
													// if the previous frame's timestamp is one less than this one
													if(frames_ja.length() > 0 && frames_ja.getJSONObject(frames_ja.length()-1).getInt("timestamp_in_seconds") == (rs.getInt("timestamp_in_seconds") - 1))
													{
														// then add to whatever streak number is there
														current_frame_jo.put("streak", frames_ja.getJSONObject(frames_ja.length()-1).getInt("streak")+1);
													}
													else
													{
														// else set the streak to 0
														current_frame_jo.put("streak", 1);
													}
													frames_ja.put(current_frame_jo);
												}
												else
												{
													delta_suppressions++;
												}
											}
											jsonresponse.put("delta_suppressions", delta_suppressions);
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
						else
						{
							jsonresponse.put("message", "Incorrect admin_password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("getAlertsForTimePeriod"))
				{
					String admin_password = request.getParameter("hoozon_admin_auth");
					if(admin_password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A admin_password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(admin_password.equals("sanders.lov"))
						{
							int moving_average_window_int = Integer.parseInt(request.getParameter("mawindow"));
							int alert_waiting_period = Integer.parseInt(request.getParameter("awp"));
							double ma_modifier_double = (new Double(request.getParameter("mamodifier"))).doubleValue();
							double single_modifier_double = (new Double(request.getParameter("singlemodifier"))).doubleValue();
							String begin = request.getParameter("begin"); // required
							String end = request.getParameter("end"); // required
							double delta_double = (new Double(request.getParameter("delta"))).doubleValue();
							if(begin == null || begin.isEmpty() || end == null || end.isEmpty()) // must always have the time range 
							{
								jsonresponse.put("message", "begin and/or end was null and/or empty.");
								jsonresponse.put("response_status", "error");
							}
							else // begin and end exist, proceed
							{	
								long begin_long = Long.parseLong(begin);
								long end_long = Long.parseLong(end);
								jsonresponse  = getAlertFrames(begin_long, end_long, "wkyt", moving_average_window_int, ma_modifier_double, single_modifier_double, alert_waiting_period, delta_double);
							}
						}
						else
						{
							jsonresponse.put("message", "Incorrect admin_password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("simulateNewFrame"))
				{
					String admin_password = request.getParameter("hoozon_admin_auth");
					if(admin_password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A admin_password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(admin_password.equals("sanders.lov"))
						{
							// get the incoming parameters, the most important of which is the timestamp (ts) value
							String ts = request.getParameter("ts"); // required
							int moving_average_window_int = Integer.parseInt(request.getParameter("mawindow"));
							int alert_waiting_period = Integer.parseInt(request.getParameter("awp"));
							double ma_modifier_double = (new Double(request.getParameter("mamodifier"))).doubleValue();
							double single_modifier_double = (new Double(request.getParameter("singlemodifier"))).doubleValue();
							// is the ts empty? if so, bail.
							if(ts == null || ts.isEmpty()) // must always have the time range 
							{
								jsonresponse.put("message", "begin and/or end was null and/or empty.");
								jsonresponse.put("response_status", "error");
							}
							else // ts exists, proceed
							{	
								Long ts_long = new Long(ts);
								jsonresponse = processNewFrame(ts_long, "wkyt", moving_average_window_int, ma_modifier_double, single_modifier_double, alert_waiting_period);
							}
						}
						else
						{
							jsonresponse.put("message", "Incorrect admin_password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else if (method.equals("resetAllLastAlerts"))
				{	
					String admin_password = request.getParameter("hoozon_admin_auth");
					if(admin_password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A admin_password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(admin_password.equals("sanders.lov"))
						{
							String station = request.getParameter("station"); // required
							if(station == null || station.isEmpty()) // must always have the time range 
							{
								jsonresponse.put("message", "station was null or empty.");
								jsonresponse.put("response_status", "error");
							}
							else // station ok
							{	
								boolean successful = resetAllLastAlerts(station);
								jsonresponse.put("response_status", "success");
							}
						}
						else
						{
							jsonresponse.put("message", "Incorrect admin_password.");
							jsonresponse.put("response_status", "error");
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
				else if (method.equals("getDesignationsAndAccounts"))
				{	
					String password = request.getParameter("hoozon_auth");
					if(password == null)
					{
						// check for designation validity FIXME
						jsonresponse.put("message", "A password value must be supplied to this method.");
						jsonresponse.put("response_status", "error");
					}
					else
					{	
						if(password.equals("firstnews72"))
						{
							String station = request.getParameter("station"); // required
							String include_master = request.getParameter("include_master");
							if(station == null || station.isEmpty()) // must always have the time range 
							{
								jsonresponse.put("message", "station was null or empty.");
								jsonresponse.put("response_status", "error");
							}
							else // station ok
							{	
								
								JSONArray designations_ja = null;
								if(include_master != null && include_master.equals("yes")) 
									designations_ja = getDesignationsAndAccounts(station, true);
								else
									designations_ja = getDesignationsAndAccounts(station, false);
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
							jsonresponse.put("message", "Incorrect password.");
							jsonresponse.put("response_status", "error");
						}
					}
				}
				else
				{
					jsonresponse.put("message", "Unknown method " + method); // we have already checked for null above
					jsonresponse.put("response_status", "error");
				}
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
	
	JSONObject getAlertFrames(long begin_long, long end_long, String station, int moving_average_window_int, double ma_modifier_double, double single_modifier_double, int alert_waiting_period, double delta_double)
	{
		JSONArray alert_frames_ja = new JSONArray();
		JSONObject jsonresponse = new JSONObject();
		JSONObject current_frame_jo = new JSONObject();
		JSONArray delta_suppressed_frames_ja = new JSONArray();
		try
		{
			JSONArray dnh_ja = getDesignationsAndHomogeneities(station); // get the designations and homogeneities for WKYT
			Connection con = null;
			Statement stmt = null;
			ResultSet rs = null;
			Statement stmt2 = null;
			ResultSet rs2 = null;
			long ts = 0L;
			double total = 0.0;
			double ma_over_window = 0.0;
			double current_homogeneity;
			String current_designation;
			double current_delta = 0.0;
			double lowest_score_in_window = 2.0;
			long ts_of_highest_score_in_window = 0L;
			double highest_score_in_window = 0.0;
			try
			{
				for(int x = 0; x < dnh_ja.length(); x++)
				{
					current_homogeneity = dnh_ja.getJSONObject(x).getDouble("homogeneity");
					current_designation = dnh_ja.getJSONObject(x).getString("designation");
					// get frames where this designation crosses the single frame threshold
					con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
					stmt = con.createStatement();
					// search database for X frames
					rs = stmt.executeQuery("SELECT * FROM frames_" + station + " WHERE (timestamp_in_seconds > " + begin_long + " AND timestamp_in_seconds <= " + end_long + " AND " + current_designation + "_avg > " + (current_homogeneity * single_modifier_double) + ") ORDER BY timestamp_in_seconds ASC"); 
					while(rs.next())
					{
						lowest_score_in_window = 2.0;
						ts = rs.getLong("timestamp_in_seconds");
						stmt2 = con.createStatement();
						rs2 = stmt2.executeQuery("SELECT * FROM frames_" + station + " WHERE (timestamp_in_seconds > " + (ts - moving_average_window_int) + " AND timestamp_in_seconds <= " + ts + ")");
						total = 0;
						
						// this is a pure moving average
						while(rs2.next())
						{
							total = total + rs2.getDouble(current_designation + "_avg");
						}
						ma_over_window = total / moving_average_window_int;
						
						// this is a moving average minus the lowest frame across the window.
						/*while(rs2.next())
						{
							//** this removes the lowest score over the window, essentially throwing out an outlier to compensate for closed eyes, 
							//* or other total failure to locate the face in an otherwise good stream of consistent images *
							if(rs2.getDouble(current_designation + "_avg") < lowest_score_in_window)
								lowest_score_in_window = rs2.getDouble(current_designation + "_avg");
							
							//if(rs2.getDouble(current_designation + "_avg") > highest_score_in_window)
							//{
							//	highest_score_in_window = rs2.getDouble(current_designation + "_avg");
							//	ts_of_highest_score_in_window = rs2.getLong("timestamp_in_seconds");
							//}
							total = total + rs2.getDouble(current_designation + "_avg");
						}
						ma_over_window = (total - lowest_score_in_window) / (moving_average_window_int - 1);*/
						if(ma_over_window > (current_homogeneity * ma_modifier_double))
						{	
							/* get second place information for delta purposes */
							double max_avg = 0.0;
							String max_designation = "";
							for(int y = 0; y < dnh_ja.length(); y++)
							{
								if(!dnh_ja.getJSONObject(y).getString("designation").equals(current_designation))
								{
									if(rs.getDouble(dnh_ja.getJSONObject(y).getString("designation") + "_avg") > max_avg)
									{
										max_avg = rs.getDouble(dnh_ja.getJSONObject(y).getString("designation") + "_avg");
										max_designation = dnh_ja.getJSONObject(y).getString("designation");
									}
								}
							}
							current_delta = rs.getDouble(current_designation + "_avg") - max_avg;
							
							if(current_delta > delta_double)
							{	
								current_frame_jo = new JSONObject();
								current_frame_jo.put("datestring", getDatestringFromTimestampInSeconds(rs.getLong("timestamp_in_seconds")));
								current_frame_jo.put("designation", current_designation);
								current_frame_jo.put("image_name", rs.getString("image_name"));
								current_frame_jo.put("timestamp_in_seconds", rs.getLong("timestamp_in_seconds"));
								current_frame_jo.put("score", rs.getDouble(current_designation + "_avg"));
								current_frame_jo.put("twitter_handle", getTwitterHandle("wkyt",current_designation));
								current_frame_jo.put("moving_average", ma_over_window);
								current_frame_jo.put("homogeneity_score", current_homogeneity);
								current_frame_jo.put("ma_threshold",  (current_homogeneity * ma_modifier_double));
								current_frame_jo.put("single_threshold", (current_homogeneity * single_modifier_double));
								current_frame_jo.put("secondplace_designation", max_designation);
								current_frame_jo.put("secondplace_score", max_avg);
								alert_frames_ja.put(current_frame_jo);
								break; // only get one frame per designation right now FIXME
							}
							else
							{
								current_frame_jo = new JSONObject();
								current_frame_jo.put("datestring", getDatestringFromTimestampInSeconds(rs.getLong("timestamp_in_seconds")));
								current_frame_jo.put("designation", current_designation);
								current_frame_jo.put("image_name", rs.getString("image_name"));
								current_frame_jo.put("timestamp_in_seconds", rs.getLong("timestamp_in_seconds"));
								current_frame_jo.put("score", rs.getDouble(current_designation + "_avg"));
								current_frame_jo.put("twitter_handle", getTwitterHandle("wkyt",current_designation));
								current_frame_jo.put("moving_average", ma_over_window);
								current_frame_jo.put("homogeneity_score", current_homogeneity);
								current_frame_jo.put("ma_threshold",  (current_homogeneity * ma_modifier_double));
								current_frame_jo.put("single_threshold", (current_homogeneity * single_modifier_double));
								current_frame_jo.put("secondplace_designation", max_designation);
								current_frame_jo.put("secondplace_score", max_avg);
								delta_suppressed_frames_ja.put(current_frame_jo);
							}
						}
					}
				}
				jsonresponse.put("response_status", "success");
				if(alert_frames_ja.length() > 0)
					jsonresponse.put("alert_frames", alert_frames_ja);
				if(delta_suppressed_frames_ja.length() > 0)
					jsonresponse.put("delta_suppressed_frames", delta_suppressed_frames_ja);
				
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
				{ 
					jsonresponse.put("warning", "Problem closing resultset, statement and/or connection to the database."); 
				}
			}   
		}
		catch(JSONException jsone)
		{
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		}	
		return jsonresponse;
	}
	
	JSONObject processNewFrame(long ts_long, String station, int moving_average_window_int, double ma_modifier_double, double single_modifier_double, int alert_waiting_period)
	{
		
		/* this function takes certain parameters and simulates what would happen if this were a realtime new 
		 * frame coming into the system and we were checking for alerts. 
		 * 
		 * 1. It gets X frames, cycles through them, putting together a JSONArray of X frames with timestamp, image name and scores object for each frame.  (see structure below)
		 * 2. It then cycles through the designations array getting an average score for each designation over the window, creating a designation_averages_ja and finding the max avg
		 * 3. If the max average is greater than the moving average threshold for that person and the last frame is greater than the raw threshold for that person
		 * 4. then respond with all the information about that frame.
		 * 
		 */
		
		JSONObject jsonresponse = new JSONObject();
		try
		{
			JSONArray dnh_ja = getDesignationsAndHomogeneities(station); // get the designations and homogeneities for WKYT
			try
			{
				ResultSet rs = null;
				Connection con = null;
				Statement stmt = null;
				try
				{
					con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
					stmt = con.createStatement();
					// search database for X frames
					rs = stmt.executeQuery("SELECT * FROM frames_" + station + " WHERE (timestamp_in_seconds > " + (ts_long - moving_average_window_int) + " AND timestamp_in_seconds <= " + ts_long + ") ORDER BY timestamp_in_seconds ASC"); 
					rs.last();
					jsonresponse.put("frames_processed", rs.getRow());  // get a row count
					if(rs.getRow() == 0) // no frames at all
					{
						jsonresponse.put("message", "No frames for this entire window. Returning with next_frame.");
						jsonresponse.put("response_status", "error");
						long next_frame = getNextFrame(ts_long);
						jsonresponse.put("next_frame", next_frame);
					}
					else if(rs.getRow() < moving_average_window_int) // missing a frame
					{
						jsonresponse.put("message", "missing a frame in the target window, cant draw safe conclusions");
						jsonresponse.put("error_code", "100");
						jsonresponse.put("response_status", "error");
					}
					else // had all X frames
					{	
						rs.beforeFirst(); // go back to the beginning for parsing
						JSONArray frames_ja = new JSONArray();
						JSONObject current_frame_jo = null;
						JSONObject current_frame_scores_jo = null;
						/*
						
						{
							"response_status": "success",
							"frames":
								[
									{
										"timestamp_in_seconds": 12312312,
										"image_name": "url",
										"scores":
											{
												"person_a": .984332,
												"person_b": .3423423
												...
											}
									}
									...											
								],
							"averages":
								[
									{
										"designation": "person_a",
										"average": .9754
									},
									{
										"designation": "person_b",
										"average": .5432
									},
									...
								]
						}
						
						*/
						while(rs.next()) // get one row
						{
							current_frame_jo = new JSONObject();
							current_frame_jo.put("image_name", rs.getString("image_name"));
							current_frame_jo.put("timestamp_in_seconds", rs.getLong("timestamp_in_seconds"));
							current_frame_scores_jo = new JSONObject();
							for(int x = 0; x < dnh_ja.length(); x++)
							{
								current_frame_scores_jo.put(dnh_ja.getJSONObject(x).getString("designation"), rs.getDouble(dnh_ja.getJSONObject(x).getString("designation") + "_avg"));
							}
							current_frame_jo.put("scores", current_frame_scores_jo);
							frames_ja.put(current_frame_jo);
						}
						
						/*
						(designation_averages_ja)
						[
							{ "designation" : "guy_mcperson", "average": .4345 },
							{ "designation" : "suzi_cue", "average": .6555 }
							...
						]
						*/
						double person_total = 0.0;
						double person_avg = 0.0;
						JSONArray designation_averages_ja = new JSONArray();
						JSONObject jo = new JSONObject();
						double max_avg = 0.0;
						String max_designation = "";
						for(int x = 0; x < dnh_ja.length(); x++)
						{
							jo = new JSONObject();
							person_total = 0.0;
							for(int j = 0; j < frames_ja.length(); j++)
							{	
								person_total = person_total + frames_ja.getJSONObject(j).getJSONObject("scores").getDouble(dnh_ja.getJSONObject(x).getString("designation"));
							}
							person_avg = person_total/frames_ja.length();
							jo.put("designation", dnh_ja.getJSONObject(x).getString("designation"));
							jo.put("average", person_avg);
							if(person_avg > max_avg)
							{
								max_avg = person_avg;
								max_designation = dnh_ja.getJSONObject(x).getString("designation");
							}
							designation_averages_ja.put(jo);
						}
														
						String twitter_handle = null;
						double max_homogeneity_double = getHomogeneityScore("wkyt", max_designation);
						if(max_avg > (max_homogeneity_double * ma_modifier_double)) // the moving average is greater than the moving average threshold
						{
							if(frames_ja.getJSONObject(frames_ja.length() -1).getJSONObject("scores").getDouble(max_designation) > (max_homogeneity_double * single_modifier_double)) // this frame's raw singular average is greater than single threshold
							{
								long last_alert = getLastAlert("wkyt", max_designation);
								if((ts_long - last_alert) > alert_waiting_period)
								{
									jsonresponse.put("alert_triggered", "yes");
									jsonresponse.put("score", frames_ja.getJSONObject(frames_ja.length() -1).getJSONObject("scores").getDouble(max_designation));
									jsonresponse.put("designation", max_designation);
									twitter_handle = getTwitterHandle("wkyt",max_designation);
									if(twitter_handle != null)
									{
										jsonresponse.put("twitter_handle", getTwitterHandle("wkyt",max_designation));
									}
									jsonresponse.put("display_name", getDisplayName("wkyt",max_designation));
									jsonresponse.put("moving_average", max_avg);
									jsonresponse.put("homogeneity_score", max_homogeneity_double);
									jsonresponse.put("ma_threshold", max_homogeneity_double * ma_modifier_double);
									jsonresponse.put("single_threshold", max_homogeneity_double * single_modifier_double);
									jsonresponse.put("timestamp_in_seconds", frames_ja.getJSONObject(frames_ja.length() -1).getLong("timestamp_in_seconds"));
									jsonresponse.put("datestring", getDatestringFromTimestampInSeconds(frames_ja.getJSONObject(frames_ja.length() -1).getLong("timestamp_in_seconds")));
									setLastAlert("wkyt", max_designation, ts_long);
								}
								else
								{
									jsonresponse.put("alert_triggered", "no");
								}
							}
							else
							{
								jsonresponse.put("alert_triggered", "no");
							}
						}
						else
						{
							jsonresponse.put("alert_triggered", "no");
						}
						jsonresponse.put("designation_averages", designation_averages_ja);
						jsonresponse.put("response_status", "success");
						jsonresponse.put("frames", frames_ja);
					}
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
		catch(JSONException jsone)
		{
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		}	
		return jsonresponse;
	}
	
	long getNextFrame(long inc_ts)
	{
		long next_frame = -1L;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			// get next frame in database, up to one day from now.
			rs = stmt.executeQuery("SELECT * FROM frames_wkyt WHERE (timestamp_in_seconds>" + inc_ts + " AND timestamp_in_seconds<" + (inc_ts + 86400) + ") limit 1"); 
			if(rs.next())
			{
				next_frame = rs.getLong("timestamp_in_seconds");
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return next_frame; // -1L indicates no frame found
	}
	
	JSONArray getDesignations(String station)
	{
		JSONArray designations_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT designation FROM people_" + station + " WHERE (active=1 AND acct_type='person')"); 
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
	
	JSONArray getDesignationsAndHomogeneities(String station)
	{
		JSONArray designations_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT designation,homogeneity FROM people_" + station + " WHERE (active=1 AND acct_type='person')"); 
			JSONObject jo = null;
			while(rs.next())
			{
				jo = new JSONObject();
				try {
					jo.put("designation", rs.getString("designation"));
					jo.put("homogeneity", rs.getDouble("homogeneity"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				designations_ja.put(jo);
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
	
	JSONArray getDesignationsAndAccounts(String station, boolean include_master)
	{
		JSONArray designations_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			if(include_master)
				rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE active=1"); 
			else // only get people
				rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE (active=1 AND acct_type='person')"); 
			JSONObject jo = null;
			while(rs.next())
			{
				jo = new JSONObject();
				try {
					System.out.println("Adding " + rs.getString("designation"));
					jo.put("designation", rs.getString("designation"));
					jo.put("display_name",  rs.getString("display_name"));
					jo.put("acct_type", rs.getString("acct_type"));
					jo.put("job_function",  rs.getString("job_function"));
					if(rs.getString("twitter_handle") != null)
						jo.put("twitter_handle", rs.getString("twitter_handle"));
					if(rs.getString("twitter_access_token") != null && !rs.getString("twitter_access_token").isEmpty())
						jo.put("twitter_connected", "yes");
					else
						jo.put("twitter_connected", "no");
					if(rs.getString("facebook_access_token") != null && !rs.getString("facebook_access_token").isEmpty())
					{
						jo.put("facebook_connected", "yes");
						JSONArray fbaccounts_ja = getFacebookAccounts("wkyt", rs.getString("facebook_access_token"));
						if(fbaccounts_ja != null)
							jo.put("facebook_accounts", fbaccounts_ja);
					}
					else
					{
						jo.put("facebook_connected", "no");
					}
					
					if(rs.getString("last_alert") != null)
						jo.put("last_alert_timestamp", rs.getInt("last_alert"));
					if(rs.getString("last_alert") != null)
						jo.put("last_alert_datestring", getDatestringFromTimestampInSeconds(rs.getLong("last_alert")));
					jo.put("homogeneity", rs.getDouble("homogeneity"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					System.out.println("JSONException:" + e.getMessage());
					e.printStackTrace();
				}
				designations_ja.put(jo);
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
	
	String getTwitterHandle(String station, String designation)
	{
		String returnval = "unknown";
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE (designation='" + designation + "' AND active=1)"); 
			if(rs.next())
			{
				returnval = rs.getString("twitter_handle");
			}
			else
				returnval = null;
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	String getDisplayName(String station, String designation)
	{
		String returnval = "unknown";
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE (designation='" + designation + "' AND active=1)"); 
			if(rs.next())
			{
				returnval = rs.getString("display_name");
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	double getHomogeneityScore(String station, String designation)
	{
		double returnval = 0.0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE (designation='" + designation + "' AND active=1)"); 
			while(rs.next())
			{
				returnval = rs.getDouble("homogeneity");
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	JSONObject getUserTwitterAccessTokenAndSecret(String station, String designation)
	{
		JSONObject return_jo = new JSONObject();
		try
		{
			ResultSet rs = null;
			Connection con = null;
			Statement stmt = null;
			try
			{
				con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
				stmt = con.createStatement();
				rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
				if(rs.next())
				{
					return_jo.put("response_status", "success");
					return_jo.put("twitter_access_token", rs.getString("twitter_access_token"));
					return_jo.put("twitter_access_token_secret", rs.getString("twitter_access_token_secret"));
				}
				else
				{
					return_jo.put("response_status", "error");
					return_jo.put("message", "could not find the specified user in the database");
				}
			}
			catch(SQLException sqle)
			{
				sqle.printStackTrace();
				return_jo.put("response_status", "error");
				return_jo.put("message", "could not find the specified user in the database");
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
				}
			}  	
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
			return return_jo;
	}
	
	long getLastAlert(String station, String designation)
	{
		long returnval = 0L;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE (designation='" + designation + "' AND active=1) "); 
			while(rs.next())
			{
				returnval = rs.getLong("last_alert");
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	boolean setLastAlert(String station, String designation, long alert_ts)
	{
		boolean returnval;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			while(rs.next())
			{
				rs.updateLong("last_alert", alert_ts);
				rs.updateRow();
			}
			returnval = true;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			returnval = false;
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
			}
		}  	
		return returnval;
	}
	
	boolean setTwitterAccessTokenAndSecret(String station, String designation, String access_token, String access_token_secret)
	{
		boolean returnval;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			while(rs.next())
			{
				rs.updateString("twitter_access_token", access_token);
				rs.updateString("twitter_access_token_secret", access_token_secret);
				rs.updateRow();
			}
			returnval = true;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			returnval = false;
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
			}
		}  	
		return returnval;
	}
	
	boolean setFacebookAccessTokenExpiresAndUID(String station, String designation, String access_token, long expires_timestamp, long fb_uid)
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			if(rs.next())
			{
				rs.updateString("facebook_access_token", access_token);
				rs.updateLong("facebook_access_token_expires", expires_timestamp);
				rs.updateLong("facebook_uid", fb_uid);
				rs.updateRow();
				returnval = true;
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	boolean setFacebookAccountIdNameAndAccessToken(String station, String designation, long id_long, String name, String account_access_token)
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			if(rs.next())
			{
				rs.updateString("facebook_account_access_token", account_access_token);
				rs.updateString("facebook_account_name", name);
				rs.updateLong("facebook_account_id", id_long);
				rs.updateRow();
				returnval = true;
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	String getFacebookAccessToken(String station, String designation)
	{
		String returnval = null;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE designation='" + designation + "'"); 
			if(rs.next())
			{
				if(rs.getString("facebook_access_token") != null && !rs.getString("facebook_access_token").isEmpty())
				{
					returnval = rs.getString("facebook_access_token");
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
	boolean resetAllLastAlerts(String station)
	{
		boolean returnval;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://hoozon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/hoozon?user=hoozon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM people_" + station + " WHERE active=1"); 
			while(rs.next())
			{
				rs.updateLong("last_alert", 0);
				rs.updateRow();
			}
			returnval = true;
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			returnval = false;
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
			}
		}  	
		return returnval;
	}
	
	private long getTimestampInSecondsFromDatestring(String datestring)
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, Integer.parseInt(datestring.substring(0,4)));
		cal.set(Calendar.MONTH, Integer.parseInt(datestring.substring(4,6)) - 1);
		cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(datestring.substring(6,8)));
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(datestring.substring(9,11)));
		cal.set(Calendar.MINUTE, Integer.parseInt(datestring.substring(11,13)));
		cal.set(Calendar.SECOND, Integer.parseInt(datestring.substring(13,15)));
		return (cal.getTimeInMillis()/1000);
	}
	
	private String getDatestringFromTimestampInSeconds(long timestamp_in_seconds)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
		cal.setTimeInMillis(timestamp_in_seconds * 1000);
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
	
	public JSONObject getFacebookAccessTokenFromAuthorizationCode(String code)
	{
		JSONObject jsonresponse = new JSONObject();
		String client_id = "176524552501035";
		String client_secret = "dbf442014759e75f2f93f2054ac319a0";
		String redirect_uri = "https://www.hoozon.tv/registration.html";
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet("https://graph.facebook.com/oauth/access_token?client_id=" + client_id + "&client_secret=" + client_secret + "&redirect_uri=" + redirect_uri + "&code=" + code);
		HttpResponse response;
		try
		{
			try 
			{
				response = client.execute(request);
				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String text = "";
				String line = "";
				while ((line = rd.readLine()) != null) {
					text = text + line;
				} 
				jsonresponse.put("response_status", "success");
				jsonresponse.put("response_from_facebook", text);
				StringTokenizer st = new StringTokenizer(text,"&");
				String currenttoken = "";
				String access_token = "";
				String expires = "";
				while(st.hasMoreTokens())
				{
					 currenttoken = st.nextToken();
					 if(currenttoken.startsWith("access_token="))
						 access_token = currenttoken.substring(currenttoken.indexOf("=") + 1);
					 else if(currenttoken.startsWith("expires="))
						 expires = currenttoken.substring(currenttoken.indexOf("=") + 1);
					 else
					 {
						 // something else. The 4 values above are the only ones twitter should return, so this case would be weird.
						 // skip
					 }
				}
				jsonresponse.put("access_token", access_token);
				jsonresponse.put("expires", expires);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				jsonresponse.put("response_status", "error");
				jsonresponse.put("message", "clientprotocolexception " + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				jsonresponse.put("response_status", "error");
				jsonresponse.put("message", "ioexception " + e.getMessage());
			}
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonresponse;
	}
	
	public JSONObject getFacebookProfile(String station, String access_token)
	{
		JSONObject jsonresponse = new JSONObject();
		
		try
		{
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet("https://graph.facebook.com/me?access_token=" + access_token);
			HttpResponse response;
			
			try 
			{
				response = client.execute(request);
				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String text = "";
				String line = "";
				while ((line = rd.readLine()) != null) {
					text = text + line;
				} 
				jsonresponse = new JSONObject(text);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonresponse;
	}
	
	public JSONArray getFacebookAccounts(String station, String access_token)
	{
		JSONArray jsonresponse = new JSONArray();
		try
		{
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet("https://graph.facebook.com/me/accounts?access_token=" + access_token);
			HttpResponse response;
			
			try 
			{
				response = client.execute(request);
				// Get the response
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String text = "";
				String line = "";
				while ((line = rd.readLine()) != null) {
					text = text + line;
				} 
				System.out.println("Endpoint.getFacebookAccounts(): response to https://graph.facebook.com/me/accounts?access_token=" + access_token + "=" + text);
				JSONObject jo = new JSONObject(text);
				jsonresponse = jo.getJSONArray("data");
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonresponse;
	}
	
	public boolean isNumeric(String incoming_string) // only works for whole numbers, positive and negative
	{
		  int x=0;
		  while(x < incoming_string.length())
		  {
			  if((x==0 && incoming_string.substring(0,1).equals("-")) ||  // OK if first element is "-"
					  incoming_string.substring(x,x+1).equals("0") || incoming_string.substring(x,x+1).equals("1") ||
					  incoming_string.substring(x,x+1).equals("2") || incoming_string.substring(x,x+1).equals("3") ||
					  incoming_string.substring(x,x+1).equals("4") || incoming_string.substring(x,x+1).equals("5") ||
					  incoming_string.substring(x,x+1).equals("6") || incoming_string.substring(x,x+1).equals("7") ||
					  incoming_string.substring(x,x+1).equals("8") || incoming_string.substring(x,x+1).equals("9"))
			  {
				  // ok
			  }
			  else
			  {
				  return false;
			  }
			  x++;
		  }
		  return true;
	}	
	
}
