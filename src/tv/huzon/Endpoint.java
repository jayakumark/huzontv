package tv.huzon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.mail.MessagingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Media;
import facebook4j.auth.AccessToken;


public class Endpoint extends HttpServlet {

	private static final long serialVersionUID = 1L;
	String dbName = System.getProperty("RDS_DB_NAME"); 
	String userName = System.getProperty("RDS_USERNAME"); 
	String password = System.getProperty("RDS_PASSWORD"); 
	String hostname = System.getProperty("RDS_HOSTNAME");
	String port = System.getProperty("RDS_PORT");
	
	public void init(ServletConfig config) throws ServletException
	{
		//System.err.println("Endpoint init()");
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
		//System.out.println("tv.huzon.Endpoint.doPost(): entering...");
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
				jsonresponse.put("message", "The huzon.tv API endpoint must be communicated with securely.");
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
				else if (method.equals("commitFrameDataAndAlert"))
				{
					String jsonpostbody_str = request.getParameter("jsonpostbody");
					if(jsonpostbody_str == null)
					{
						jsonresponse.put("message", "jsonpostbody was null. Couldn't find the parameter.");
						jsonresponse.put("response_status", "error");
					}
					else if(jsonpostbody_str.isEmpty())
					{
						jsonresponse.put("message", "jsonpostbody was empty.");
						jsonresponse.put("response_status", "error");
					}	
					else
					{
						JSONObject jsonpostbody = new JSONObject(jsonpostbody_str);
						boolean simulation = false;
						if(!jsonpostbody.has("password") || !jsonpostbody.getString("password").equals("Xri47Q00EIY70S4t5l02637371g2V"))
						{	
							jsonresponse.put("message", "password was missing or incorrect");
							jsonresponse.put("response_status", "error");
						}
						else if(!jsonpostbody.has("station"))
						{
							jsonresponse.put("message", "method requires a station value");
							jsonresponse.put("response_status", "error");
						}
						else // postbody exists and password is correct
						{	
							Station station_object = new Station(jsonpostbody.getString("station"));
							boolean process = true;
							if(jsonpostbody.has("simulation") && (jsonpostbody.getString("simulation").equals("yes") || jsonpostbody.getString("simulation").equals("true")))
							{ 
								System.out.println("Endpoint.commitFrameDataAndAlert(): This is a simulation. Looking up existing frame....");
								simulation = true;
								// no need to do anything here. The frame (if it exists in the db) gets constructed and processed below.
							}
							else // treat as real commit
							{	
								
								Connection con = null;
								Statement stmt = null;
								ResultSet rs = null;
								try
								{
									long inc_ts = jsonpostbody.getLong("timestamp_in_ms");
									con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);

									stmt = con.createStatement();
									rs = stmt.executeQuery("SELECT timestamp_in_ms FROM frames_" + jsonpostbody.getString("station") + " ORDER BY timestamp_in_ms DESC limit 1");
									if(rs.next()) // the only reason there wouldn't be a row is the VERY FIRST FRAME EVER for this station's table (or if the table gets emptied for some reason
									{
										// 1. if newer frames are ALREADY in the database, discard this one. Shouldn't happen.
										// 2. if the newest frame in the database is between 0 and .8 seconds older, that's good. That's exactly what we want. Insert this one.
										// 3. if the newest frame in the database is older than .8 seconds, we need to wait a few sec for our target frame to come in.

										// 1. THIS FRAME IS TOO OLD.
										if(rs.getLong("timestamp_in_ms") >= inc_ts) // if the frame from the DB is NEWER than this one, then insert but don't process
										{
											System.out.println("Endpoint.commit(): Frame is too old. Discarding.");
											//(new Platform()).addMessageToLog("discarded old frame for station=" + jsonpostbody.getString("station") + " with timestamp=" + jsonpostbody.getString("image_name"));

											jsonresponse.put("message", "There is at least one newer frame in the database. This one was old, inserted, but not processed for alerts.");
											jsonresponse.put("response_status", "error");
											process = false;
										}
										else
										{	
											if((inc_ts - rs.getLong("timestamp_in_ms")) > 3000) // this frame is VERY new. Don't wait.
											{	
												process = true;
											}
											else // this frame was between 1 and 3000 milliseconds newer than the last one. Go through waiting process, if need be.
											{
												// 3. THIS FRAME IS TOO NEW, wait up to 1.5 seconds, checking every 400ms for a "just right" frame
												int x = 0;
												while(!((inc_ts - rs.getLong("timestamp_in_ms")) < 800 && (inc_ts - rs.getLong("timestamp_in_ms")) > 0) && x < 5) // if it's not in the sweet zone, wait
												{
													//System.out.println("Endpoint.commit(): Frame is too new. Waiting x=" + x);
													try { Thread.sleep(300);} catch (InterruptedException e) { e.printStackTrace(); }
													rs.close();
													rs = stmt.executeQuery("SELECT timestamp_in_ms FROM frames_" + jsonpostbody.getString("station") + " ORDER BY timestamp_in_ms DESC limit 1");
													rs.next();
													x++;
												}

												if(x==5)
												{
													System.out.println("Endpoint.commit(): Frame was too new and waiting failed. Inserting, but not processing.");
													jsonresponse.put("message", "Frame was too new, waited unsuccessful, will be inserted, but not processed.");
													jsonresponse.put("response_status", "error");
													process = false;
												}
												else if(x == 0)
												{
													//System.out.println("Endpoint.commit(): Frame was perfect on first try.");
													process = true;
												}
												else
												{
													//System.out.println("Endpoint.commit(): Frame is good now. Had to wait x=" + x);
													process = true;
												}
											}
										}
									}

									double currentavgscore = 0.0;
									double reporter_total = 0.0;
									JSONArray all_scores_ja = new JSONArray();
									JSONArray ja = jsonpostbody.getJSONArray("reporter_scores");
									String fieldsstring = " (";
									String valuesstring = " (";
									fieldsstring = fieldsstring + "`" + "image_name" + "`, ";
									valuesstring = valuesstring + "'" + jsonpostbody.getString("image_name") + "', ";
									fieldsstring = fieldsstring + "`" + "s3_location" + "`, ";
									valuesstring = valuesstring + "'s3://huzon-frames-" + station_object.getCallLetters() + "/" + jsonpostbody.getString("image_name") + "', ";
									fieldsstring = fieldsstring + "`" + "url" + "`, ";
									valuesstring = valuesstring + "'http://" + station_object.getS3BucketPublicHostname() + "/" + jsonpostbody.getString("image_name") + "', ";
									fieldsstring = fieldsstring + "`" + "timestamp_in_ms" + "`, ";
									valuesstring = valuesstring + "'" + jsonpostbody.getLong("timestamp_in_ms") + "', ";
									fieldsstring = fieldsstring + "`" + "frame_rate" + "`, ";
									valuesstring = valuesstring + "'" + jsonpostbody.getInt("frame_rate") + "', ";
									for(int x = 0; x < ja.length(); x++)
									{
										reporter_total = 0.0;
										for(int i = 0; i < ja.getJSONObject(x).getJSONArray("scores").length(); i++)
										{
											reporter_total = reporter_total + ja.getJSONObject(x).getJSONArray("scores").getDouble(i); 
											all_scores_ja.put(ja.getJSONObject(x).getJSONArray("scores").getDouble(i));
											//total_score = total_score + ja.getJSONObject(x).getJSONArray("scores").getDouble(i); 
										}
										currentavgscore = reporter_total / ja.getJSONObject(x).getJSONArray("scores").length();

										// have decided these raw scores are unnecessary. Leaving it out makes for a smaller, more efficient database.
										//fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_scores" + "`, ";
										//valuesstring = valuesstring + "'" + ja.getJSONObject(x).getJSONArray("scores").toString() + "', ";
										fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_score" + "`, ";
										valuesstring = valuesstring + "'" + currentavgscore + "', ";
										fieldsstring = fieldsstring + "`" + ja.getJSONObject(x).getString("designation")+"_num" + "`, ";
										valuesstring = valuesstring + "'" + ja.getJSONObject(x).getJSONArray("scores").length() + "', ";
									}
									fieldsstring = fieldsstring.substring(0,fieldsstring.length() - 2) + ")";
									valuesstring = valuesstring.substring(0,valuesstring.length() - 2) + ")";
									//System.out.println("Attempting to execute query: INSERT IGNORE INTO `frames_" + jo.getString("station") + "` " + fieldsstring + " VALUES " + valuesstring);
									con.createStatement().execute("INSERT IGNORE INTO `frames_" + station_object.getCallLetters() + "` " + fieldsstring + " VALUES " + valuesstring);
									con.createStatement().execute("UPDATE `stations` SET `frame_rate`='" + jsonpostbody.getInt("frame_rate") + "' WHERE call_letters='" + station_object.getCallLetters() + "'");	

									rs.close();
									stmt.close();
									con.close();
								}
								catch(SQLException sqle)
								{
									jsonresponse.put("message", "There was a problem attempting to insert the scores into the database. sqle.getMessage()=" + sqle.getMessage());
									jsonresponse.put("response_status", "error");
									sqle.printStackTrace();
									(new Platform()).addMessageToLog("SQLException in Endpoint commitFrameDataAndAlert: Error occurred when inserting frame scores. message=" +sqle.getMessage());
								} 
								finally
								{
									try
									{
										if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
									}
									catch(SQLException sqle)
									{
										jsonresponse.put("warning", "There was a problem closing the resultset, statement and/or connection to the database.");
										(new Platform()).addMessageToLog("SQLException in Endpoint commitFrameDataAndAlert: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
									}
								}  
							}

							if(process)
							{
								Frame newframe = new Frame(jsonpostbody.getLong("timestamp_in_ms"), jsonpostbody.getString("station"));
								if(newframe.getTimestampInMillis() > 0) // 0 indicates failure to insert/retrieve
								{	
									newframe.calculateAndSetMAs();
									JSONObject jo2 = null;
									if(simulation)
										jo2 = newframe.process(station_object.getMAModifier(), station_object.getNRPST(), station_object.getDelta(), 
												"test", "silent", jsonpostbody.getInt("awp_int"), jsonpostbody.getInt("awp_int"), jsonpostbody.getInt("maw_int")); // last 4 are which_timers, alert_mode and tw/fb overrides (-1 = use database vals)
									else
										jo2 = newframe.process(station_object.getMAModifier(), station_object.getNRPST(), station_object.getDelta(), 
												"production", station_object.getAlertMode(), -1, -1, station_object.getMAWindow()); // last 4 are which_timers, alert_mode and tw/fb overrides (-1 = use database vals)

									// {
									// 		alert_triggered: true or false,                         // means the user passed/failed the metric thresholds to fire an alert
									//		(if alert_triggered==true)
									//      	twitter_triggered: true or false,
									//			(if twitter_triggered then)
									//				twitter_successful: true or false,					// means alert posted and returned an id (or failed)
									//				(if !twitter_successful, then)
									//					twitter_failure_message: some message about why twitter failed,	
									//
									//      	facebook_triggered: true or false,
									//			(if facebook_triggered then)				
									//				facebook_successful: true or false,					// means alert posted and returned an id (or failed)
									//				(if !facebook_successful, then)
									//					facebook_failure_message: some message about why facebook failed,	
									//		(else if alert_triggered== false)
									//			alert_triggered_failure_message: reason,			// means triggered + the actual alert was attempted bc user had credentials and was outside waiting period
									// }

									jsonresponse = jo2;
									jsonresponse.put("response_status", "success"); // just means the insertion was received and a response is coming back. Means nothing in terms of alerts
									if(simulation) // then return additional info to the simulator. If not, don't.
									{	
										if(jsonpostbody.has("designation"))
										{
											System.out.println("Endpoint.commitFrameDataAndAlert(): a designation=" + jsonpostbody.getString("designation") + " (maw_int=" + jsonpostbody.getInt("maw_int") + ") was specified by the simulator. Returning specialized information in each frame_jo.");
											jsonresponse.put("frame_jo", newframe.getAsJSONObject(true, jsonpostbody.getString("designation")));
										}
										else
											jsonresponse.put("frame_jo", newframe.getAsJSONObject(true, null));
									}
								}
								else
								{
									jsonresponse.put("message", "There was a problem inserting the data and/or building a Frame object from it.");
									jsonresponse.put("response_status", "error");
								}
							}
						}
					}
				}
				else
				{
					jsonresponse.put("message", "Unknown method.");
					jsonresponse.put("response_status", "error");
				}
			}
			tempcal = Calendar.getInstance();
			long timestamp_at_exit = tempcal.getTimeInMillis();
			long elapsed = timestamp_at_exit - timestamp_at_entry;
			jsonresponse.put("elapsed", elapsed);
			//System.out.println("Endpoint.doGet(): final jsonresponse=" + jsonresponse);	// respond with object, success response, or error
			out.println(jsonresponse);
		}
		catch(JSONException jsone)
		{
			out.println("{ \"error\": { \"message\": \"JSONException caught in Endpoint\" } }");
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
			jsone.printStackTrace();
		}	
		return;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		// All requests must be secure
		// All requests must have a method value
		// startTwitterAuthentication and getTwitterAccessTokenFromAuthorizationCode do not need TW authorization, every other request does.
		// for methods requiring authentication there are currently two permission levels: normal and global
		
		
		
		//System.out.println("tv.huzon.Endpoint.doGet(): entering...");
		response.setContentType("application/json; charset=UTF-8;");
		response.setHeader("Access-Control-Allow-Origin","*"); //FIXME
		PrintWriter out = response.getWriter();
		JSONObject jsonresponse = new JSONObject();
		Calendar tempcal = Calendar.getInstance();
		long timestamp_at_entry = tempcal.getTimeInMillis();
		try
		{
			String method = request.getParameter("method");
			String twitter_handle = request.getParameter("twitter_handle");
			String twitter_access_token = request.getParameter("twitter_access_token");
			if(!request.isSecure())
			{
				jsonresponse.put("message", "The huzon.tv API endpoint must be communicated with securely.");
				jsonresponse.put("response_status", "error");
				(new Platform()).addMessageToLog("Endpoint.doGet(): Endpoint rejected request for being insecure.");
			}
			else if(method == null)
			{
				jsonresponse.put("message", "Method not specified. This should probably produce HTML output reference information at some point.");
				jsonresponse.put("response_status", "error");
				(new Platform()).addMessageToLog("Endpoint.doGet(): Endpoint rejected incoming request for lacking a method value.");
			}
			/***
			 *     _   _ _____ _   _         ___  _   _ _____ _   _  ___  ___ _____ _____ _   _ ___________  _____ 
			 *    | \ | |  _  | \ | |       / _ \| | | |_   _| | | | |  \/  ||  ___|_   _| | | |  _  |  _  \/  ___|
			 *    |  \| | | | |  \| |______/ /_\ \ | | | | | | |_| | | .  . || |__   | | | |_| | | | | | | |\ `--. 
			 *    | . ` | | | | . ` |______|  _  | | | | | | |  _  | | |\/| ||  __|  | | |  _  | | | | | | | `--. \
			 *    | |\  \ \_/ / |\  |      | | | | |_| | | | | | | | | |  | || |___  | | | | | \ \_/ / |/ / /\__/ /
			 *    \_| \_/\___/\_| \_/      \_| |_/\___/  \_/ \_| |_/ \_|  |_/\____/  \_/ \_| |_/\___/|___/  \____/ 
			 *                                                                                                     
			 *                                                                                                     
			 */
			else if (method.equals("startTwitterAuthentication") || method.equals("getTwitterAccessTokenFromAuthorizationCode"))
			{
				if (method.equals("startTwitterAuthentication"))
				{
					jsonresponse = (new Twitter()).startTwitterAuthentication();
					(new Platform()).addMessageToLog("Endpoint.startTwitterAuthentication(): Someone started Twitter authentication");
				}
				else if (method.equals("getTwitterAccessTokenFromAuthorizationCode"))
				{
					String oauth_verifier = request.getParameter("oauth_verifier");
					String oauth_token = request.getParameter("oauth_token");
					if(oauth_verifier == null)
					{
						jsonresponse.put("message", "This method requires an oauth_verifier value.");
						jsonresponse.put("response_status", "error");
						(new Platform()).addMessageToLog("Endpoint.getTwitterAccessTokenFromAuthorizationCode(): Error: getTwitterAccessTokenFromAuthorizationCode called without oauth_verifier value.");
					}
					else if(oauth_token == null)
					{
						jsonresponse.put("message", "This method requires an oauth_token value.");
						jsonresponse.put("response_status", "error");
						(new Platform()).addMessageToLog("Endpoint.getTwitterAccessTokenFromAuthorizationCode(): Error: getTwitterAccessTokenFromAuthorizationCode called without oauth_token value.");
					}
					else
					{	
						(new Platform()).addMessageToLog("Endpoint.getTwitterAccessTokenFromAuthorizationCode(): Continuing TW authentication. Will make call to Twitter and return immediately with screen_name");
						JSONObject preliminary_jsonresponse = (new Twitter()).getTwitterAccessTokenFromAuthorizationCode(oauth_verifier, oauth_token);
						if(preliminary_jsonresponse.getString("response_status").equals("success"))
						{
							(new Platform()).addMessageToLog("Endpoint.getTwitterAccessTokenFromAuthorizationCode(): Continuing TW authentication (gTwAuthTokFromAccCode) for " + preliminary_jsonresponse.getString("screen_name"));
							User user = new User(preliminary_jsonresponse.getString("screen_name"), "twitter_handle");
							if(!user.isValid())
							{
								jsonresponse.put("message", "The screen name returned by Twitter (" + preliminary_jsonresponse.getString("screen_name") + ") was not found in our database.");
								jsonresponse.put("response_status", "error");
							}
							else
							{
								user.setTwitterAccessTokenAndSecret(preliminary_jsonresponse.getString("access_token"), preliminary_jsonresponse.getString("access_token_secret"));
								jsonresponse.put("response_status", "success");
								jsonresponse.put("message", "The access_token and access_token_secret should be set in the database now.");
								jsonresponse.put("twitter_handle", preliminary_jsonresponse.getString("screen_name"));
								jsonresponse.put("twitter_access_token", preliminary_jsonresponse.getString("access_token"));
							}
						}
						else
						{
							(new Platform()).addMessageToLog("Endpoint.getTwitterAccessTokenFromAuthorizationCode(): Failed talking to twitter. Twitter object jsonresponse=" + jsonresponse);
							jsonresponse = preliminary_jsonresponse;
						}
					}
				}
			}
			/***
			 *      ___  _   _ _____ _   _       ______ _____ _____    ___  ___ _____ _____ _   _ ___________  _____ 
			 *     / _ \| | | |_   _| | | |      | ___ \  ___|  _  |   |  \/  ||  ___|_   _| | | |  _  |  _  \/  ___|
			 *    / /_\ \ | | | | | | |_| |______| |_/ / |__ | | | |   | .  . || |__   | | | |_| | | | | | | |\ `--. 
			 *    |  _  | | | | | | |  _  |______|    /|  __|| | | |   | |\/| ||  __|  | | |  _  | | | | | | | `--. \
			 *    | | | | |_| | | | | | | |      | |\ \| |___\ \/' /_  | |  | || |___  | | | | | \ \_/ / |/ / /\__/ /
			 *    \_| |_/\___/  \_/ \_| |_/      \_| \_\____/ \_/\_(_) \_|  |_/\____/  \_/ \_| |_/\___/|___/  \____/ 
			 *                                                                                                       
			 *                                                                                                       
			 */			
			else if(twitter_access_token == null || twitter_handle == null)
			{
				if(twitter_handle == null && twitter_access_token == null) // both missing
				{	
					jsonresponse.put("message", "Endpoint.doGet(): The method you have specified requires twitter_handle and twitter_access_token values. Neither was specified.");
					jsonresponse.put("response_status", "error");
				}
				else if(twitter_handle == null) // just twitter_handle missing
				{	
					jsonresponse.put("message", "Endpoint.doGet(): The method you have specified requires a twitter_handle value.");
					jsonresponse.put("response_status", "error");
				}
				else if (twitter_access_token == null)
				{
					jsonresponse.put("message", "Endpoint.doGet(): The method you have specified requires a twitter_access_token value.");
					jsonresponse.put("response_status", "error");
				}
				(new Platform()).addMessageToLog("Endpoint.doGet(): Rejected incoming request for lacking twitter credentials.");
			}
			else  // at this point, we know the request is secure, has a method value and the user has supplied credentials (which have not been checked yet)
			{
				/***
				 *     _____  _   _  _____ _____  _   __  _____ _    _   _____ ______ ___________ _____ _   _ _____ _____  ___   _      _____ 
				 *    /  __ \| | | ||  ___/  __ \| | / / |_   _| |  | | /  __ \| ___ \  ___|  _  \  ___| \ | |_   _|_   _|/ _ \ | |    /  ___|
				 *    | /  \/| |_| || |__ | /  \/| |/ /    | | | |  | | | /  \/| |_/ / |__ | | | | |__ |  \| | | |   | | / /_\ \| |    \ `--. 
				 *    | |    |  _  ||  __|| |    |    \    | | | |/\| | | |    |    /|  __|| | | |  __|| . ` | | |   | | |  _  || |     `--. \
				 *    | \__/\| | | || |___| \__/\| |\  \   | | \  /\  / | \__/\| |\ \| |___| |/ /| |___| |\  | | |  _| |_| | | || |____/\__/ /
				 *     \____/\_| |_/\____/ \____/\_| \_/   \_/  \/  \/   \____/\_| \_\____/|___/ \____/\_| \_/ \_/  \___/\_| |_/\_____/\____/ 
				 *                                                                                                                            
				 *                                                                                                                            
				 */
				User user = new User(twitter_handle, "twitter_handle");
				if(!user.isValid()) // meaning, this twitter_handle/user was found in the database. Auth token not checked yet. 
				{
					jsonresponse.put("message", "Invalid user.");
					jsonresponse.put("response_status", "error");
					(new Platform()).addMessageToLog("Endpoint.doGet(): incoming request rejected bc twitter_handle not found in db.) " + twitter_handle);
				}
				else if(user.getTwitterAccessToken() == null || user.getTwitterAccessToken().equals("")) // twitter_handle was in the db, but no twitter_access_token value exists. User needs to register.
				{
					jsonresponse.put("message", "Your twitter_handle was found in our database, but does not appear to be linked yet. Please register.");
					jsonresponse.put("response_status", "error"); 
					jsonresponse.put("error_code", "07734");  // this code tells the frontend to forget everything it knows about this user.
					(new Platform()).addMessageToLog("Endpoint.doGet(): incoming request rejected bc although twitter_handle found in db, no twitter_access_token provided. User needs to register." + twitter_handle);
				}
				else if(!user.getTwitterAccessToken().equals(twitter_access_token))
				{
					jsonresponse.put("message", "The user's twitter_access_token has become invalid. Please re-link your Twitter account to huzon.tv via the registration page.");
					jsonresponse.put("response_status", "error");
					jsonresponse.put("error_code", "07734"); // this code tells the frontend to forget everything it knows about this user.
					(new Platform()).addMessageToLog("Endpoint.doGet(): incoming request rejected bc provided twitter_access_token did not match the value in the database. " + twitter_handle);
				}
				else // twitter creds were OK -- permission level has not been checked yet.
				{
					/***
					 *     _   _ ______________  ___  ___   _      ______ ______________  ________ _____ _____ _____ _____ _   _  ___  ___ _____ _____ _   _ ___________  _____ 
					 *    | \ | |  _  | ___ \  \/  | / _ \ | |     | ___ \  ___| ___ \  \/  |_   _/  ___/  ___|_   _|  _  | \ | | |  \/  ||  ___|_   _| | | |  _  |  _  \/  ___|
					 *    |  \| | | | | |_/ / .  . |/ /_\ \| |     | |_/ / |__ | |_/ / .  . | | | \ `--.\ `--.  | | | | | |  \| | | .  . || |__   | | | |_| | | | | | | |\ `--. 
					 *    | . ` | | | |    /| |\/| ||  _  || |     |  __/|  __||    /| |\/| | | |  `--. \`--. \ | | | | | | . ` | | |\/| ||  __|  | | |  _  | | | | | | | `--. \
					 *    | |\  \ \_/ / |\ \| |  | || | | || |____ | |   | |___| |\ \| |  | |_| |_/\__/ /\__/ /_| |_\ \_/ / |\  | | |  | || |___  | | | | | \ \_/ / |/ / /\__/ /
					 *    \_| \_/\___/\_| \_\_|  |_/\_| |_/\_____/ \_|   \____/\_| \_\_|  |_/\___/\____/\____/ \___/ \___/\_| \_/ \_|  |_/\____/  \_/ \_| |_/\___/|___/  \____/ 
					 *                                                                                                                                                          
					 *                                                                                                                                                          
					 */
					// getSelf and the three FB auth methods do not require global permissions.
					if (method.equals("getSelf")) // used for getting oneself, no admin priviliges required
					{
						jsonresponse.put("response_status", "success");
						 boolean return_tokens = false; boolean return_tw_profile = false; boolean return_fb_profile = false; boolean return_fb_page = false; boolean return_alerts = false;
						jsonresponse.put("user_jo", user.getAsJSONObject(return_tokens, return_tw_profile, return_fb_profile, return_fb_page, return_alerts));
						(new Platform()).addMessageToLog("Endpoint.getSelf(): successful for " + twitter_handle);
					}
					else if(method.equals("getFacebookAccessTokenFromAuthorizationCode"))
					{
						String facebook_code = request.getParameter("facebook_code");
						if(facebook_code == null)
						{
							jsonresponse.put("message", "This method requires a facebook_code value.");
							jsonresponse.put("response_status", "error");
							(new Platform()).addMessageToLog("Ep.getFBAccTokFromAuthCode for twitter_handle=" + twitter_handle + " no fb code");
						}
						else
						{	
							// at this point the user has been sent to facebook for permission. The response came back with a code.
							// Now we need to get and store the user's access_token
							JSONObject preliminary_jsonresponse = getFacebookAccessTokenFromAuthorizationCode(facebook_code);
							if(preliminary_jsonresponse.getString("response_status").equals("success"))
							{
								user.setTwitterAccessTokenAndSecret(preliminary_jsonresponse.getString("access_token"), "notyetknown");
								JSONObject fb_profile_jo = user.getProfileFromFacebook();
								long fb_uid = 0L;
								try
								{
									if(fb_profile_jo != null && fb_profile_jo.has("id"))
									{
										fb_uid = fb_profile_jo.getLong("id");
										Calendar cal = Calendar.getInstance();
										cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
										String expires = preliminary_jsonresponse.getString("expires");
										int expires_in_seconds = 0;
										if(expires == null || expires.isEmpty()) // this seems to happen when the user has already given the fb permission but is re-linking the account for whatever reason 
										{
											expires_in_seconds = 5184000; // FIXME? Defaulting to 60 days may not be the right behavior. 
										}
										else
											expires_in_seconds = Integer.parseInt(expires);
										cal.add(Calendar.SECOND, expires_in_seconds);
										long expires_timestamp = cal.getTimeInMillis() / 1000;
										boolean successful = user.setFacebookAccessTokenExpiresAndUID(preliminary_jsonresponse.getString("access_token"), expires_timestamp, fb_uid);
																					
										if(successful)
										{	
											jsonresponse.put("response_status", "success");
											jsonresponse.put("message", "The access_token, expires and uid should be set in the database now.");
											(new Platform()).addMessageToLog("Ep.getFBAccTokFromAuthCode for twitter_handle=" + twitter_handle + " success");
										}
										else
										{
											jsonresponse.put("message", "encountered error attempting to update the database with the 3 fb values");
											jsonresponse.put("response_status", "error");
											(new Platform()).addMessageToLog("Ep.getFBAccTokFromAuthCode for twitter_handle=" + twitter_handle + " couldn't update db with 3 fb values");
										}
									}
									else if(fb_profile_jo != null && fb_profile_jo.has("error"))
									{
										jsonresponse.put("message", "Getting profile from FB produced an error. message=" + fb_profile_jo.getJSONObject("error").getString("message") + " code=" + fb_profile_jo.getJSONObject("error").getString("code"));
										jsonresponse.put("response_status", "error");
										(new Platform()).addMessageToLog("Ep.getFBAccTokFromAuthCode for twitter_handle=" + twitter_handle + " getting profile from FB produced an error" + fb_profile_jo.getJSONObject("error").getString("message"));
									}
									else
									{
										jsonresponse.put("message", "fb profile didn't have id field");
										jsonresponse.put("response_status", "error");
										(new Platform()).addMessageToLog("Ep.getFBAccTokFromAuthCode for twitter_handle=" + twitter_handle + " fb profile didn't have id field");
									}
								}
								catch(NumberFormatException nfe)
								{
									jsonresponse.put("message", "Number format exception for expires=" + preliminary_jsonresponse.getString("expires") + " or for fb profile id value full preliminary_jsonresponse=" + preliminary_jsonresponse);
									jsonresponse.put("response_status", "error");
									(new Platform()).addMessageToLog("Ep.getFBAccTokFromAuthCode for twitter_handle=" + twitter_handle + " number format exception");
								}
							}
							else
							{
								jsonresponse = preliminary_jsonresponse; // just return the error
								(new Platform()).addMessageToLog("Ep.getFBAccTokFromAuthCode for twitter_handle=" + twitter_handle + " preliminary response was erroneous");
							}
						}
					}
					else if (method.equals("getFacebookSubAccountInfoFromFacebook"))
					{
						(new Platform()).addMessageToLog("Ep.getFacebookSubAccountInfoFromFacebook for twitter_handle=" + twitter_handle + " ");
						// now check to see if top-level FB is linked
						if(!user.facebookTopLevelIsLinked())
						{
							jsonresponse.put("message", "It appears the top-level facebook account is not linked. Thus, we can't get the subaccount (reporter page) information.");
							jsonresponse.put("response_status", "error");
							(new Platform()).addMessageToLog("Ep.getFacebookSubAccountInfoFromFacebook for twitter_handle=" + twitter_handle + " top level fb not linked");
						}
						else
						{
							JSONArray fb_subaccounts_ja = user.getSubAccountsFromFacebook();
							if(fb_subaccounts_ja == null)
							{
								jsonresponse.put("message", "Error retrieving subaccount information from Facebook.");
								jsonresponse.put("response_status", "error");
								(new Platform()).addMessageToLog("Ep.getFacebookSubAccountInfoFromFacebook for twitter_handle=" + twitter_handle + " error retrieving from fb");
							}
							else
							{	
								if(fb_subaccounts_ja.length() == 0)
								{
									jsonresponse.put("response_status", "success");
									jsonresponse.put("message", "Successfully pinged facebook, but no subaccounts found.");
									(new Platform()).addMessageToLog("Ep.getFacebookSubAccountInfoFromFacebook for twitter_handle=" + twitter_handle + " got from fb, no subaccounts found");
								}
								else
								{
									jsonresponse.put("response_status", "success");
									jsonresponse.put("fb_subaccounts_ja", fb_subaccounts_ja);
									(new Platform()).addMessageToLog("Ep.getFacebookSubAccountInfoFromFacebook for twitter_handle=" + twitter_handle + " success getting subaccounts");
								}
							}
						}
					}
					else if (method.equals("setFacebookSubAccountInfo")) // sets the designated journalist page for this user
					{
						String fb_subaccount_id = request.getParameter("fb_subaccount_id");
						if(fb_subaccount_id == null)
						{
							jsonresponse.put("message", "A fb_subaccount_id value must be supplied to this method.");
							jsonresponse.put("response_status", "error");
							(new Platform()).addMessageToLog("Ep.setFacebookSubAccountInfo for twitter_handle=" + twitter_handle + " no fb_subaccount_id supplied to method");
						}
						else
						{	
							if(!user.facebookTopLevelIsLinked())
							{
								jsonresponse.put("message", "It appears the top-level facebook account is not linked. Thus, we can't set the subaccount (reporter page).");
								jsonresponse.put("response_status", "error");
								(new Platform()).addMessageToLog("Ep.setFacebookSubAccountInfo for twitter_handle=" + twitter_handle + " top-level account is not linked. can't set subaccount");
							}
							else
							{
								JSONArray fb_subaccounts_ja = user.getSubAccountsFromFacebook();
								if(fb_subaccounts_ja == null)
								{
									jsonresponse.put("message", "Error retrieving subaccount information from Facebook.");
									jsonresponse.put("response_status", "error");
									(new Platform()).addMessageToLog("Ep.setFacebookSubAccountInfo for twitter_handle=" + twitter_handle + " error retrieving subaccount info from facebook");
								}
								else
								{	
									if(fb_subaccounts_ja.length() == 0)
									{
										jsonresponse.put("response_status", "error");
										jsonresponse.put("message", "Successfully pinged facebook, but no subaccounts found. Can't set subaccount.");
										(new Platform()).addMessageToLog("Ep.setFacebookSubAccountInfo for twitter_handle=" + twitter_handle + " got info from fb but no subaccounts found");
									}
									else
									{
										boolean specified_subaccount_exists = false;
										for(int x = 0; x < fb_subaccounts_ja.length(); x++)
										{
											if(fb_subaccounts_ja.getJSONObject(x).getString("id").equals(fb_subaccount_id))
											{
												specified_subaccount_exists = true;
											}
										}
										if(!specified_subaccount_exists)
										{
											jsonresponse.put("message", "The specified subaccount id (" + fb_subaccount_id + ") doesn't exist for this user's top-level facebook account.");
											jsonresponse.put("response_status", "error");
											jsonresponse.put("fb_subaccounts_ja", fb_subaccounts_ja);
											(new Platform()).addMessageToLog("Ep.setFacebookSubAccountInfo for twitter_handle=" + twitter_handle + " the subaccount id provided to this method wasn't found in this user's facebook subaccounts");
										}
										else
										{
											boolean successful = user.setFacebookSubAccountIdNameAndAccessToken(fb_subaccount_id, fb_subaccounts_ja);
											if(successful)
											{
												jsonresponse.put("response_status", "success");
												(new Platform()).addMessageToLog("Ep.setFacebookSubAccountInfo for twitter_handle=" + twitter_handle + " success setting fb subaccount id name and access token");
											}
											else
											{
												jsonresponse.put("message", "Pinged facebook, specified subaccount is valid, but ran into error inserting into db.");
												jsonresponse.put("response_status", "error");
												(new Platform()).addMessageToLog("Ep.setFacebookSubAccountInfo for twitter_handle=" + twitter_handle + " Pinged facebook, specified subaccount is valid, but ran into error inserting into db.");
											}
										}
									}
								}
							}
						}
					}
					else if(user.isGlobalAdmin()) // everything else requires global permissions
					{
						/***
						 *     _____                                                  _   _               _                                                                            
						 *    |  __ \                                                | | | |             | |                                                                           
						 *    | |  \/_____ _ __   ___ _ __ _ __ ___    _ __ ___   ___| |_| |__   ___   __| |___   _ __ ___  __ _     _ __   ___    _ __   __ _ _ __ __ _ _ __ ___  ___ 
						 *    | | _|______| '_ \ / _ \ '__| '_ ` _ \  | '_ ` _ \ / _ \ __| '_ \ / _ \ / _` / __| | '__/ _ \/ _` |   | '_ \ / _ \  | '_ \ / _` | '__/ _` | '_ ` _ \/ __|
						 *    | |_\ \     | |_) |  __/ |  | | | | | | | | | | | |  __/ |_| | | | (_) | (_| \__ \ | | |  __/ (_| |_  | | | | (_) | | |_) | (_| | | | (_| | | | | | \__ \
						 *     \____/     | .__/ \___|_|  |_| |_| |_| |_| |_| |_|\___|\__|_| |_|\___/ \__,_|___/ |_|  \___|\__, (_) |_| |_|\___/  | .__/ \__,_|_|  \__,_|_| |_| |_|___/
						 *                | |                                                                                 | |                 | |                                  
						 *                |_|                                                                                 |_|                 |_|                                  
						 */
						if(method.equals("getStations"))// || method.equals("") || method.equals("") || method.equals("") || method.equals(""))
						{
							if(method.equals("getStations"))
							{
								JSONArray stations_ja = (new Platform()).getStationsAsJSONArray();
								if(stations_ja != null)
								{
									jsonresponse.put("response_status", "success");
									jsonresponse.put("stations_ja", stations_ja);
									(new Platform()).addMessageToLog("Ep.getStations(): requested by twitter_handle=" + twitter_handle + " successful.");
								}
								else
								{
									jsonresponse.put("message", "Error getting stations as JSONArray");
									jsonresponse.put("response_status", "error");
									(new Platform()).addMessageToLog("Ep.getStations():requested by twitter_handle=" + twitter_handle + " unsuccessful. Unable to retrieve stations from db.");
								}
							}
						}
						/***
						 *     _____                                                  _   _               _                            _                           _       
						 *    |  __ \                                                | | | |             | |                          | |                         | |      
						 *    | |  \/_____ _ __   ___ _ __ _ __ ___    _ __ ___   ___| |_| |__   ___   __| |___   _ __ ___  __ _    __| | ___  ___      ___  _ __ | |_   _ 
						 *    | | _|______| '_ \ / _ \ '__| '_ ` _ \  | '_ ` _ \ / _ \ __| '_ \ / _ \ / _` / __| | '__/ _ \/ _` |  / _` |/ _ \/ __|    / _ \| '_ \| | | | |
						 *    | |_\ \     | |_) |  __/ |  | | | | | | | | | | | |  __/ |_| | | | (_) | (_| \__ \ | | |  __/ (_| | | (_| |  __/\__ \_  | (_) | | | | | |_| |
						 *     \____/     | .__/ \___|_|  |_| |_| |_| |_| |_| |_|\___|\__|_| |_|\___/ \__,_|___/ |_|  \___|\__, |  \__,_|\___||___(_)  \___/|_| |_|_|\__, |
						 *                | |                                                                                 | |                                     __/ |
						 *                |_|                                                                                 |_|                                    |___/ 
						 */
						if(method.equals("getUser") || method.equals("verifyTwitterCredentials") || method.equals("verifyTopLevelFBCredentials") || method.equals("verifyPageFBCredentials"))
						{
							String designation = request.getParameter("designation");
							if(designation == null)
							{
								jsonresponse.put("message", "This method (" + method + ") requires a designation value.");
								jsonresponse.put("response_status", "error");
								(new Platform()).addMessageToLog("Ep.doGet(): method (" + method + ") requested by twitter_handle=" + twitter_handle + " rejected due to missing designation value.");
							}
							else
							{
								if (method.equals("getUser")) // for getting a DIFFERENT user, global_admin required
								{
									User target_user = new User(designation, "designation");
								    String return_tokens_param = request.getParameter("return_tokens");
									String return_tw_profile_param = request.getParameter("return_tw_profile");
									String return_fb_profile_param = request.getParameter("return_fb_profile");
									String return_fb_page_param = request.getParameter("return_fb_page");
									String return_alerts_param = request.getParameter("return_alerts");
									boolean return_tokens = false; boolean return_tw_profile = false; boolean return_fb_profile = false; boolean return_fb_page = false; boolean return_alerts = false;
									if(return_tokens_param != null && (return_tokens_param.equals("yes") || return_tokens_param.equals("true")))
										return_tokens = true;
									if(return_tw_profile_param != null && (return_tw_profile_param.equals("yes") || return_tw_profile_param.equals("true")))
										return_tw_profile = true;
									if(return_fb_profile_param != null && (return_fb_profile_param.equals("yes") || return_fb_profile_param.equals("true")))
										return_fb_profile = true;
									if(return_fb_page_param != null && (return_fb_page_param.equals("yes") || return_fb_page_param.equals("true")))
										return_fb_page = true;
									if(return_alerts_param != null && (return_alerts_param.equals("yes") || return_alerts_param.equals("true")))
										return_alerts = true;
									jsonresponse.put("response_status", "success");
									jsonresponse.put("user_jo", target_user.getAsJSONObject(return_tokens, return_tw_profile, return_fb_profile, return_fb_page, return_alerts));
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
								}
								else if (method.equals("verifyTwitterCredentials"))
								{	
									User target_user = new User(designation, "designation");
									boolean tCredsAreValid = target_user.twitterCredentialsAreValid();
									jsonresponse.put("response_status", "success");
									jsonresponse.put("valid", tCredsAreValid);
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
								}
								else if (method.equals("verifyTopLevelFBCredentials"))
								{	
									User target_user = new User(designation, "designation");
									boolean fbCredsAreValid = target_user.fbTopLevelTokenIsValid();
									jsonresponse.put("valid", fbCredsAreValid);
									jsonresponse.put("response_status", "success");
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");	
								} 
								else if (method.equals("verifyPageFBCredentials"))
								{	
									User target_user = new User(designation, "designation");
									boolean fbPageCredsAreValid = target_user.fbPageTokenIsValid();
									jsonresponse.put("valid", fbPageCredsAreValid);
									jsonresponse.put("response_status", "success");
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");		
								} 
							}
						}
						/***
						 *     _____                                                  _   _               _                              _        _   _             
						 *    |  __ \                                                | | | |             | |                            | |      | | (_)            
						 *    | |  \/_____ _ __   ___ _ __ _ __ ___    _ __ ___   ___| |_| |__   ___   __| |___   _ __ ___  __ _     ___| |_ __ _| |_ _  ___  _ __  
						 *    | | _|______| '_ \ / _ \ '__| '_ ` _ \  | '_ ` _ \ / _ \ __| '_ \ / _ \ / _` / __| | '__/ _ \/ _` |   / __| __/ _` | __| |/ _ \| '_ \ 
						 *    | |_\ \     | |_) |  __/ |  | | | | | | | | | | | |  __/ |_| | | | (_) | (_| \__ \ | | |  __/ (_| |_  \__ \ || (_| | |_| | (_) | | | |
						 *     \____/     | .__/ \___|_|  |_| |_| |_| |_| |_| |_|\___|\__|_| |_|\___/ \__,_|___/ |_|  \___|\__, (_) |___/\__\__,_|\__|_|\___/|_| |_|
						 *                | |                                                                                 | |                                   
						 *                |_|                                                                                 |_|                                   
						 */
						if(method.equals("getActiveReporterDesignations") || method.equals("resetProductionAlertTimers") || method.equals("resetTestAlertTimers") || method.equals("getMostRecentAlerts") || // station only
								method.equals("getFrameTimestamps") || method.equals("getFrames") || method.equals("getFramesAboveDesignationHomogeneityThreshold") || method.equals("getAlertFrames") 
								|| method.equals("getStation") || method.equals("getActiveReporters")) // station + begin/end
						{
							String station_param = request.getParameter("station");
							if(station_param == null)
							{
								jsonresponse.put("message", "This method (" + method + ") requires a station value.");
								jsonresponse.put("response_status", "error");
								(new Platform()).addMessageToLog("Ep.doGet(): method (" + method + ") requested by twitter_handle=" + twitter_handle + " rejected due to missing station value.");
							}
							else
							{
								 Station station_object = new Station(station_param);
								 if(!station_object.isValid())
								 {
									 jsonresponse.put("message", "The station value provided was not valid.");
									 jsonresponse.put("response_status", "error");
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " rejected. Station value invalid.");
								 }
								/***
								 *     _____                                                  _   _               _                              _        _   _               _____ _   _  _   __   __
								 *    |  __ \                                                | | | |             | |                            | |      | | (_)             |  _  | \ | || |  \ \ / /
								 *    | |  \/_____ _ __   ___ _ __ _ __ ___    _ __ ___   ___| |_| |__   ___   __| |___   _ __ ___  __ _     ___| |_ __ _| |_ _  ___  _ __   | | | |  \| || |   \ V / 
								 *    | | _|______| '_ \ / _ \ '__| '_ ` _ \  | '_ ` _ \ / _ \ __| '_ \ / _ \ / _` / __| | '__/ _ \/ _` |   / __| __/ _` | __| |/ _ \| '_ \  | | | | . ` || |    \ /  
								 *    | |_\ \     | |_) |  __/ |  | | | | | | | | | | | |  __/ |_| | | | (_) | (_| \__ \ | | |  __/ (_| |_  \__ \ || (_| | |_| | (_) | | | | \ \_/ / |\  || |____| |  
								 *     \____/     | .__/ \___|_|  |_| |_| |_| |_| |_| |_|\___|\__|_| |_|\___/ \__,_|___/ |_|  \___|\__, (_) |___/\__\__,_|\__|_|\___/|_| |_|  \___/\_| \_/\_____/\_/  
								 *                | |                                                                                 | |                                                             
								 *                |_|                                                                                 |_|                                                             
								 */
								 else if (method.equals("getActiveReporterDesignations"))
								 {	
									 jsonresponse.put("response_status", "success");
									 jsonresponse.put("reporters_ja", new JSONArray(station_object.getReporterDesignations()));
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
								 }
								/* else if (method.equals("getActiveReporters"))
								 {	
									 jsonresponse.put("response_status", "success");
									 boolean return_tokens = false; boolean return_tw_profile = true; boolean return_fb_profile = true; boolean return_fb_page = true; boolean return_alerts = true;
									 jsonresponse.put("reporters_ja", station_object.getReportersAsJSONArray(return_tokens, return_tw_profile, return_fb_profile, return_fb_page, return_alerts));
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
								 }*/
								 else if (method.equals("resetProductionAlertTimers")) // DANGEROUS!!!!
								 {
									 station_object.resetProductionAlertTimers();
									 jsonresponse.put("response_status", "success");
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
								 } 
								 else if (method.equals("resetTestAlertTimers"))
								 {
									 station_object.resetTestAlertTimers();
									 jsonresponse.put("response_status", "success");
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
								 }
								 else if (method.equals("getMostRecentAlerts"))
								 {	
									 jsonresponse.put("alerts_ja",station_object.getMostRecentAlerts(24));
									 jsonresponse.put("response_status", "success");
									 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
								 }
								 else if(method.equals("getStation"))
								 {
										JSONObject station_jo = station_object.getAsJSONObject();
										if(station_jo != null)
										{
											jsonresponse.put("response_status", "success");
											jsonresponse.put("station_jo", station_jo);
											(new Platform()).addMessageToLog("Ep.getStations(): requested by twitter_handle=" + twitter_handle + " successful.");
										}
										else
										{
											jsonresponse.put("message", "Error getting station as JSONObject");
											jsonresponse.put("response_status", "error");
											(new Platform()).addMessageToLog("Ep.getStations():requested by twitter_handle=" + twitter_handle + " unsuccessful. Unable to return station as JSON object.");
										}
								 }
								 /***
								  *     _____                                                  _   _                                _        _   _                 _                _                          _ 
								  *    |  __ \                                                | | | |                              | |      | | (_)               | |              (_)                        | |
								  *    | |  \/_____ _ __   ___ _ __ _ __ ___    _ __ ___   ___| |_| |__      _ __ ___  __ _     ___| |_ __ _| |_ _  ___  _ __     | |__   ___  __ _ _ _ __       ___ _ __   __| |
								  *    | | _|______| '_ \ / _ \ '__| '_ ` _ \  | '_ ` _ \ / _ \ __| '_ \    | '__/ _ \/ _` |   / __| __/ _` | __| |/ _ \| '_ \    | '_ \ / _ \/ _` | | '_ \     / _ \ '_ \ / _` |
								  *    | |_\ \     | |_) |  __/ |  | | | | | | | | | | | |  __/ |_| | | |_  | | |  __/ (_| |_  \__ \ || (_| | |_| | (_) | | | |_  | |_) |  __/ (_| | | | | |_  |  __/ | | | (_| |
								  *     \____/     | .__/ \___|_|  |_| |_| |_| |_| |_| |_|\___|\__|_| |_(_) |_|  \___|\__, (_) |___/\__\__,_|\__|_|\___/|_| |_( ) |_.__/ \___|\__, |_|_| |_( )  \___|_| |_|\__,_|
								  *                | |                                                                   | |                                  |/               __/ |       |/                    
								  *                |_|                                                                   |_|                                                  |___/                              
								  */
								 else if(method.equals("getFrameTimestamps") || method.equals("getFrames") || method.equals("getFramesAboveDesignationHomogeneityThreshold") || method.equals("getAlertFrames"))
								 {
									 String begin = request.getParameter("begin");
									 String end = request.getParameter("end");
									 if(begin == null)
									 {
										 jsonresponse.put("message", "A begin value must be supplied to this method.");
										 jsonresponse.put("response_status", "error");
										 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " rejected due to missing begin value.");
									 }
									 else if(end == null)
									 {
										 jsonresponse.put("message", "A end value must be supplied to this method.");
										 jsonresponse.put("response_status", "error");
										 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " rejected due to missing end value.");
									 }
									 else
									 {
										 // the begin and end values can come in as ms timestmap or as YYYYMMDD_HHMMSS format. The underscore decides which to use.
										 boolean use_long = false;
										 long begin_long = 0L;
										 long end_long = 0L;
										 if(begin.indexOf("_") == -1 && end.indexOf("_") == -1)
										 {
											 begin_long = Long.parseLong(begin);
											 end_long = Long.parseLong(end);
											 use_long = true;
										 }
										 if (method.equals("getFrameTimestamps")) // inclusive
										 {
											 JSONArray timestamps_ja = null;
											 if(use_long)
												 timestamps_ja = station_object.getFrameTimestamps(begin_long, end_long);
											 else
												 timestamps_ja = station_object.getFrameTimestamps(begin, end);
											 jsonresponse.put("response_status", "success");
											 jsonresponse.put("timestamps_ja", timestamps_ja);
											 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
										 }
										 else if (method.equals("getFrames")) // inclusive
										 {
											 String get_score_data = request.getParameter("get_score_data");
											 if(get_score_data == null)
											 {
												 jsonresponse.put("message", "A get_score_data value must be supplied to this method.");
												 jsonresponse.put("response_status", "error");
												 (new Platform()).addMessageToLog("Ep.getFrames():requested by twitter_handle=" + twitter_handle + " unsuccessful. a get_score_data param must be provided to this method.");
											 }
											 else
											 {	
												 if(get_score_data.equals("true"))
												 {	
													 jsonresponse.put("response_status", "success");
													 if(use_long)
														 jsonresponse.put("frames_ja", station_object.getFramesAsJSONArray(begin_long, end_long, true));
													 else
														 jsonresponse.put("frames_ja", station_object.getFramesAsJSONArray(begin, end, true));
													 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
												 }
												 else if(get_score_data.equals("false"))
												 {
													 jsonresponse.put("response_status", "success");
													 if(use_long)
														 jsonresponse.put("frames_ja", station_object.getFramesAsJSONArray(begin_long, end_long, false));
													 else
														 jsonresponse.put("frames_ja", station_object.getFramesAsJSONArray(begin, end, false));
													 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
												 }
												 else
												 {	
													 jsonresponse.put("message", "get_score_data must be true or false.");
													 jsonresponse.put("response_status", "error");
													 (new Platform()).addMessageToLog("Ep.getFrames(): get_score_data param must be \"true\" or \"false\".");
												 }
											 }
										 }
										 else if (method.equals("getFramesAboveDesignationHomogeneityThreshold"))
										 {	
											 String designation = request.getParameter("designation");
											 String delta = request.getParameter("delta");
											 if(designation == null)
											 {
												 jsonresponse.put("message", "A designation value must be supplied to this method.");
												 jsonresponse.put("response_status", "error");
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. A designation value is required.");
											 }
											 else if(delta == null)
											 {
												 jsonresponse.put("message", "A delta value must be supplied to this method.");
												 jsonresponse.put("response_status", "error");
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. A delta value is required.");
											 }
											 else
											 {	
												 jsonresponse.put("response_status", "success");
												 boolean get_score_data = true;
												 jsonresponse.put("frames_ja", station_object.getFramesAboveDesignationHomogeneityThresholdAsJSONArray(new Long(begin).longValue()*1000, new Long(end).longValue()*1000, 
														 designation, get_score_data)); 
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
											 }	
										 }
										 else if (method.equals("getAlertFrames"))
										 {
											 String mamodifier = request.getParameter("mamodifier");
											 String awp = request.getParameter("awp");
											 String nrpst = request.getParameter("nrpst"); // number required past single threshold
											 String delta = request.getParameter("delta");
											 String maw = request.getParameter("maw");
											 if(awp == null)
											 {
												 jsonresponse.put("message", "An awp value must be supplied to this method.");
												 jsonresponse.put("response_status", "error");
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. An awp value is required.");
											 }
											 else if(mamodifier == null)
											 {
												 jsonresponse.put("message", "A mamodifier value must be supplied to this method.");
												 jsonresponse.put("response_status", "error");
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. A mamodifier value is required.");
											 }
											 else if(nrpst == null)
											 {
												 jsonresponse.put("message", "A nrpst value must be supplied to this method.");
												 jsonresponse.put("response_status", "error");
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. A nrpst value is required.");
											 }
											 else if(delta == null)
											 {
												 jsonresponse.put("message", "A delta value must be supplied to this method.");
												 jsonresponse.put("response_status", "error");
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. A delta value is required.");
											 }
											 else if(maw == null)
											 {
												 jsonresponse.put("message", "A maw value must be supplied to this method.");
												 jsonresponse.put("response_status", "error");
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. A maw value is required.");
											 }
											 else
											 {	
												 double ma_modifier_double = (new Double(request.getParameter("mamodifier"))).doubleValue();
												 int awp_in_sec = (new Integer(request.getParameter("awp"))).intValue();
												 int nrpst_int = (new Integer(request.getParameter("nrpst"))).intValue();
												 int maw_int = (new Integer(request.getParameter("maw"))).intValue();
												 double delta_double = Double.parseDouble(request.getParameter("delta"));
												 
												 JSONArray alert_frames_ja = null;
												 if(!use_long)
												 {
													 System.out.println("Endpoint.getAlertFrames(): calling Station.getAlertFrames with begin and end STRINGS");
													 alert_frames_ja = station_object.getAlertFrames(begin, end, ma_modifier_double, awp_in_sec, nrpst_int, delta_double, maw_int);
												 }
												 else
												 {
													 System.out.println("Endpoint.getAlertFrames(): calling Station.getAlertFrames with begin and end LONGS");
													 alert_frames_ja = station_object.getAlertFrames(begin_long, end_long, ma_modifier_double, awp_in_sec, nrpst_int, delta_double, maw_int);
												 }
												 jsonresponse.put("response_status", "success");
												 jsonresponse.put("alert_frames_ja", alert_frames_ja);
												 (new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
											 }		
										 }
									 }
								 } // end methods requiring station, begin, end
							}
						} // end methods requiring station value
						else if (method.equals("deleteSocialItem"))
						{	
							String id = request.getParameter("id"); // this is the huzon.tv ID. NOT the social item ID
							if(id == null)
							{
								jsonresponse.put("message", "A id value must be supplied to this method.");
								jsonresponse.put("response_status", "error");
								(new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. An id value is required.");
							}
							else
							{	
								Alert alert = new Alert(Long.parseLong(id));
								boolean deletionsuccessful = alert.deleteSocialItem();
								if(deletionsuccessful)
								{
									jsonresponse.put("response_status", "success");
									jsonresponse.put("social_response", true);
									(new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " successful.");
								}
								else
								{
									jsonresponse.put("message", "Error. Could not delete from the social service.");
									jsonresponse.put("response_status", "error");
									(new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") requested by twitter_handle=" + twitter_handle + " unsuccessful. Could not delete from the social service.");
								}
							}	
						} 
					} // end methods requiring global permissions (user is global admin) block
					else
					{
						// user is not global admin or method unknown
						jsonresponse.put("message", "Method unknown or global permissions required.");
						jsonresponse.put("response_status", "error");
						(new Platform()).addMessageToLog("Ep.doGet():  method (" + method + ") method unknown or global permissions required.");
					}
				} // end twitter creds ok block
			}// end methods requiring auth
			tempcal = Calendar.getInstance();
			long timestamp_at_exit = tempcal.getTimeInMillis();
			long elapsed = timestamp_at_exit - timestamp_at_entry;
			jsonresponse.put("elapsed", elapsed);
			out.println(jsonresponse);
		}
		catch(JSONException jsone)
		{
			out.println("{ \"error\": { \"message\": \"JSONException caught in Endpoint\" } }");
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		} 
		return;
	}

	public JSONObject getFacebookAccessTokenFromAuthorizationCode(String code)
	{
		JSONObject jsonresponse = new JSONObject();
		String client_id = "176524552501035";
		String client_secret = "dbf442014759e75f2f93f2054ac319a0";
		String redirect_uri = "https://www.huzon.tv/registration.html";
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet("https://graph.facebook.com/oauth/access_token?" +
				"client_id=" + client_id + 
				"&client_secret=" + client_secret + 
				"&redirect_uri=" + redirect_uri + 
				"&code=" + code);
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
				//String longlived_access_token = getLongLivedFacebookAccessToken(access_token);
				//(new Platform()).addMessageToLog("Attempted to get long-lived FB access token", "previous token=" + access_token + "\nLL token=" + longlived_access_token + "\n\nSubaccount tokens gotten with this LL access token should be non-expiring.\n\nresponse from fb original access token request (not LL)=" + text, "cyrus7580@gmail.com", "info@huzon.tv");
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
			/*catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}	
		catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonresponse;
	}
	
	public String getLongLivedFacebookAccessToken(String existing_access_token)
	{
		String returnstring = null;
		String client_id = "176524552501035";
		String client_secret = "dbf442014759e75f2f93f2054ac319a0";
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet("https://graph.facebook.com/oauth/access_token?" +
				"client_id=" + client_id + 
				"&client_secret=" + client_secret + 
				"&grant_type=fb_exchange_token" + 
				"&fb_exchange_token=" + existing_access_token);
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
			returnstring = text;
			System.out.println("returnstring=" + returnstring);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return returnstring;
	}
	
	/*https://graph.facebook.com/oauth/access_token?             
	    client_id=APP_ID&
	    client_secret=APP_SECRET&
	    grant_type=fb_exchange_token&
	    fb_exchange_token=EXISTING_ACCESS_TOKEN */
	
	public static void main(String[] args) {
		Endpoint e = new Endpoint();
		User cyrus = new User("huzon_master", "designation");
		String llat = e.getLongLivedFacebookAccessToken(cyrus.getFacebookAccessToken());
	}
	
}




