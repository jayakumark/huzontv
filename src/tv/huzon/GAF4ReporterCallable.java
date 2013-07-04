package tv.huzon;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Media;
import facebook4j.auth.AccessToken;

public class GAF4ReporterCallable implements Callable<JSONArray> {

	long begin;
	long end;
	int mawindow;
	double mamodifier;
	double singlemodifier;
	int awp;
	int nrpst;
	User reporter;
	Station station_object;
	
	String dbName = System.getProperty("RDS_DB_NAME"); 
	String userName = System.getProperty("RDS_USERNAME"); 
	String password = System.getProperty("RDS_PASSWORD"); 
	String hostname = System.getProperty("RDS_HOSTNAME");
	String port = System.getProperty("RDS_PORT");
	
	public GAF4ReporterCallable(User inc_reporter, Station inc_station_object, long inc_begin, long inc_end, int inc_mawindow, double inc_mamodifier, double inc_singlemodifier, int inc_awp, int inc_nrpst)
	{
		try {
	        Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		begin = inc_begin;
		end = inc_end;
		mawindow = inc_mawindow;
		mamodifier = inc_mamodifier;
		singlemodifier = inc_singlemodifier;
		awp = inc_awp;
		nrpst = inc_nrpst; // number required past single thresh
		reporter = inc_reporter;
		station_object = inc_station_object;
	}
	
	public JSONArray call() {
		
		//Connection con = null;
		//Statement stmt = null;
		//ResultSet rs = null;
		//Statement stmt2 = null;
		//ResultSet rs2 = null;
		long ts = 0L;
		double homogeneity = reporter.getHomogeneity();
		String designation = reporter.getDesignation();
		double single_thresh = homogeneity * singlemodifier;
		double ma_thresh = homogeneity * mamodifier;
		JSONArray alert_frames_ja = new JSONArray();
		try
		{
			//con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			//stmt = con.createStatement();
			// get frames where this designation crosses the single frame threshold
			//rs = stmt.executeQuery("SELECT * FROM frames_" + station_object + " WHERE (timestamp_in_ms >= " + (1000*begin) + " AND timestamp_in_ms <= " + (1000*end) + " AND " + designation + "_avg > " + single_thresh + ") ORDER BY timestamp_in_ms ASC");
			//rs.last();
			//System.out.println("GAF4ReporterCallable.call() found " + rs.getRow()  + " frames over threshold (" + homogeneity + " * " + singlemodifier + "=" +  single_thresh + ") for " + designation);
			//rs.beforeFirst();
			//Platform p = new Platform();
			TreeSet<Frame> frames_past_single_thresh = station_object.getFrames((begin*1000), (end*1000), designation, singlemodifier);
			//TreeSet<Frame> frames_past_single_thresh = p.getFramesFromResultSet(rs);
			Iterator<Frame> frames_past_single_thresh_it = frames_past_single_thresh.iterator();
			Frame currentframe = null;
			Frame subsequentframe = null;
			Frame frame_that_passed_ma_thresh = null;
			double moving_average = 0.0;
			long last_ts_of_frame_added = 0L;
			while(frames_past_single_thresh_it.hasNext()) // loop through frames
			{
				currentframe = frames_past_single_thresh_it.next();
				if((currentframe.getTimestampInMillis() - last_ts_of_frame_added) > (awp * 1000)) // if this frame is outside the awp and eligible to be returned
				{	
					System.out.println("GAF4ReporterCallable.call() Calculating moving average of " + designation + " for the current frame with maw_int=" + mawindow);
					moving_average = currentframe.getMovingAverage(designation, mawindow);
					frame_that_passed_ma_thresh = null;
					if(moving_average == -1)
					{
						//System.out.println("GAF4ReporterCallable.call() There were not enough frames in this window. Skip this frame.");
					}
					else
					{
						if(moving_average > ma_thresh && moving_average == currentframe.getHighestMovingAverage(mawindow))
						{
							//System.out.println("GAF4ReporterCallable.call() " + designation + " passed ma threshold on first shot and is the highest moving average of the frame. ma=" + moving_average);
							frame_that_passed_ma_thresh = currentframe;
						}
						else // initial frame didn't pass, look for subsequent frames that pass the ma threshold.
						{
							//System.out.println("GAF4ReporterCallable.call() ma of current DID NOT pass req thresh. ma=" + moving_average + " thresh=" + mamodifier);
							//stmt2 = con.createStatement();
							// get frames after the current ts within the maw_int window
							ts = currentframe.getTimestampInMillis();
							//System.out.println("GAF4ReporterCallable.call() executing SELECT * FROM frames_" + station + " WHERE (timestamp_in_ms > " + ts + " AND timestamp_in_ms <= " + (ts + 1000*mawindow) + ") ORDER BY timestamp_in_ms ASC");
							//rs2 = stmt2.executeQuery("SELECT * FROM frames_" + station + " WHERE (timestamp_in_ms > " + ts + " AND timestamp_in_ms <= " + (ts + 1000*mawindow) + ") ORDER BY timestamp_in_ms ASC");
							//rs2.last();
							//System.out.println("Got " + rs2.getRow() + " subsequent frames.");
							//rs2.beforeFirst();
							TreeSet<Frame> subsequent_frames = station_object.getFrames(ts, (ts + 1000*mawindow), null, -1);
							//TreeSet<Frame> subsequent_frames = p.getFramesFromResultSet(rs2);
							Iterator<Frame> subsequent_frames_it = subsequent_frames.iterator();
							while(subsequent_frames_it.hasNext())
							{
								subsequentframe = subsequent_frames_it.next();
								moving_average = subsequentframe.getMovingAverage(designation, mawindow);
								if(moving_average > ma_thresh && moving_average == subsequentframe.getHighestMovingAverage(mawindow))
								{
									frame_that_passed_ma_thresh = subsequentframe;
									break;
								}
								else
								{
									//System.out.println("GAF4ReporterCallable.call() ma of subsequent DID NOT pass req thresh. ma=" + moving_average + " thresh=" + ma_thresh);
								}
							}
							//rs2.close();
							//stmt2.close();
						}
						
						int num_frames_in_window_above_single_thresh = 0;
						if(frame_that_passed_ma_thresh != null) 
						{
							num_frames_in_window_above_single_thresh = frame_that_passed_ma_thresh.getNumFramesInWindowAboveSingleThresh(designation, mawindow, single_thresh);
							if(num_frames_in_window_above_single_thresh >= nrpst)
							{	
								JSONObject jo2add = frame_that_passed_ma_thresh.getAsJSONObject(true, null, -1); // no designation specified
								jo2add.put("designation", designation);
								jo2add.put("ma_for_alert_frame", currentframe.getMovingAverage(designation, mawindow));
								jo2add.put("ma_for_frame_that_passed_ma_thresh", frame_that_passed_ma_thresh.getMovingAverage(designation, mawindow));
								jo2add.put("score_for_alert_frame", currentframe.getScore(designation));
								jo2add.put("score_for_frame_that_passed_ma_thresh", frame_that_passed_ma_thresh.getScore(designation));
								jo2add.put("image_name_for_frame_that_passed_ma_thresh", frame_that_passed_ma_thresh.getImageName());
								jo2add.put("homogeneity", homogeneity);
								jo2add.put("ma_threshold", ma_thresh);
								jo2add.put("single_threshold", single_thresh);
								alert_frames_ja.put(jo2add); 
								last_ts_of_frame_added = frame_that_passed_ma_thresh.getTimestampInMillis();
								// do not break the frames loop as before. Get all alerts for this window, pursuant to the awp between each.
							}
						}	
					}
				}
			}
		//rs.close();
		//stmt.close();
		//con.close();
		//}
		//catch(SQLException sqle)
		//{
		//	sqle.printStackTrace();
		//	(new Platform()).addMessageToLog("SQLException in GAF4ReporterCallable.call() message=" +sqle.getMessage());
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
		finally
		{
		/*	try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in GAF4ReporterCallable.call() Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}*/
		}   
		
		return alert_frames_ja;
		
		/*
		SimpleEmailer se = new SimpleEmailer();
		JSONObject return_jo = new JSONObject();
		try
		{
			(new Platform()).addMessageToLog("FB triggered for " + reporter.getDesignation() + " mode=" + mode);
			
			if(mode.equals("silent"))
			{
				return_jo.put("facebook_successful", false);
				return_jo.put("facebook_failure_message", "silent");
				(new Platform()).addMessageToLog("FB triggered but suppressed for " + reporter.getDesignation() + ". mode=silent");
			}
			else if(mode.equals("test") || mode.equals("live"))
			{
				User postinguser = null;
				if(mode.equals("test"))
					postinguser = new User("huzon_master", "designation");
				else
					postinguser = reporter;
				
				if(postinguser.getFacebookPageAccessToken() == null || postinguser.getFacebookPageAccessToken().equals(""))
				{
					return_jo.put("facebook_successful", false);
					return_jo.put("facebook_failure_message", "user " + postinguser.getDesignation() + " has no twitter credentials");
					(new Platform()).addMessageToLog("FB triggered for " + reporter.getDesignation() + " but failed due to lack of tw credentials. user=" + postinguser.getDesignation() + ". mode=" + mode);
					if(mode.equals("live"))
					{
						String emailmessage = getMissingCredentialsEmailMessage();
						se.sendMail("Action required: huzon.tv FB alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
						(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of missing FB credentials.");
					}
				}
				else // user appears to have facebook credentials
				{
					URL[] image_urls = frame2upload.get2x2CompositeURLs();
					if(image_urls == null)
					{
						return_jo.put("facebook_successful", false);
						return_jo.put("facebook_failure_message", "image_urls was null coming back from Frame.get2x2CompositeURLs()");
						(new Platform()).addMessageToLog("FB triggered for " + reporter.getDesignation() + " + creds, but get2x2 failed.  user=" + postinguser.getDesignation() + ". mode=" + mode);
					}
					else
					{
						File[] image_files = new File[4];
						String tmpdir = System.getProperty("java.io.tmpdir");
						String[] image_filenames = new String[4];
						image_filenames[0] = tmpdir + "/" + UUID.randomUUID() + ".jpg";
						image_filenames[1] = tmpdir + "/" + UUID.randomUUID() + ".jpg";
						image_filenames[2] = tmpdir + "/" + UUID.randomUUID() + ".jpg";
						image_filenames[3] = tmpdir + "/" + UUID.randomUUID() + ".jpg";
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[0] + " and saving to " + image_filenames[0]);
						ReadableByteChannel rbc0 = Channels.newChannel(image_urls[0].openStream());
						FileOutputStream fos0 = new FileOutputStream(image_filenames[0]); // FIXME this might have conflicts with multiple stations
						fos0.getChannel().transferFrom(rbc0, 0, Long.MAX_VALUE);
						image_files[0] = new File(image_filenames[0]);
						fos0.close();
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[1] + " and saving to " + image_filenames[1]);
						ReadableByteChannel rbc1 = Channels.newChannel(image_urls[1].openStream());
						FileOutputStream fos1 = new FileOutputStream(image_filenames[1]); // FIXME this might have conflicts with multiple stations
						fos1.getChannel().transferFrom(rbc1, 0, Long.MAX_VALUE);
						image_files[1] = new File(image_filenames[1]);
						fos1.close();
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[2] + " and saving to " + image_filenames[2]);
						ReadableByteChannel rbc2 = Channels.newChannel(image_urls[2].openStream());
						FileOutputStream fos2 = new FileOutputStream(image_filenames[2]); // FIXME this might have conflicts with multiple stations
						fos2.getChannel().transferFrom(rbc2, 0, Long.MAX_VALUE);
						image_files[2] = new File(image_filenames[2]);
						fos2.close();
						
						System.out.println("FacebookUploaderCallable.call(): downloading " + image_urls[3] + " and saving to " + image_filenames[3]);				
						ReadableByteChannel rbc3 = Channels.newChannel(image_urls[3].openStream());
						FileOutputStream fos3 = new FileOutputStream(image_filenames[3]); // FIXME this might have conflicts with multiple stations
						fos3.getChannel().transferFrom(rbc3, 0, Long.MAX_VALUE);
						image_files[3] = new File(image_filenames[3]);
						fos3.close();
						
						try 
						{
							// create the new image, canvas size is the max. of both image sizes
							int w = 2560;
							int h = 1440;
							BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

							// load source images
							// paint both images, preserving the alpha channels
							Graphics G = combined.getGraphics();
							BufferedImage image0 = ImageIO.read(image_files[0]);
							G.drawImage(image0, 0, 0, null);
							BufferedImage image1 = ImageIO.read(image_files[1]);
							G.drawImage(image1, 1280, 0, null);
							BufferedImage image2 = ImageIO.read(image_files[2]);
							G.drawImage(image2, 0, 720, null);
							BufferedImage image3 = ImageIO.read(image_files[3]);
							G.drawImage(image3, 1280, 720, null);
							
							BufferedImage after = new BufferedImage(w/4, h/4, BufferedImage.TYPE_INT_ARGB);
							AffineTransform at = new AffineTransform();
							at.scale(0.25, 0.25);
							AffineTransformOp scaleOp = 
							   new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
							after = scaleOp.filter(combined, after);
							
							//Graphics g = after.getGraphics();

							// Save as new image
							String composite_filename = tmpdir + "/" + UUID.randomUUID() + ".png";
							System.out.println("FacebookUploaderCallable.call(): writing composite image.");
							ImageIO.write(after, "PNG", new File(composite_filename));
							File composite_file = new File(composite_filename);
							
							image_files[0].delete();
							image_files[1].delete();
							image_files[2].delete();
							image_files[3].delete();
							
							if(!station_object.isLocked("facebook"))
							{
								String uuid = UUID.randomUUID().toString();
								boolean successfullylocked = station_object.lock(uuid, "facebook");
								
								if(successfullylocked)
								{	
							
									Platform p = new Platform();
									long redirect_id = p.createAlertInDB(station_object, "facebook", reporter.getDesignation(), frame2upload.getURLString(), postinguser);
									String message = station_object.getMessage("facebook", frame2upload.getTimestampInMillis(), redirect_id, reporter);

									Facebook facebook = new FacebookFactory().getInstance();
									facebook.setOAuthAppId("176524552501035", "dbf442014759e75f2f93f2054ac319a0");
									facebook.setOAuthPermissions("publish_stream,manage_page");
									facebook.setOAuthAccessToken(new AccessToken(postinguser.getFacebookPageAccessToken(), null)); // if "test" get huzontv access token, if "live" get reporter's access token
									
									String facebookresponse = "";
									try {
										facebookresponse = facebook.postPhoto(new Long(postinguser.getFacebookPageID()).toString(), new Media(composite_file), message, "33684860765", false); // FIXME hardcode to wkyt station

										return_jo.put("facebook_successful", true); // if no exception thrown above, we get to this statement and assume post was successful, regardless of two db updates below
										(new Platform()).addMessageToLog("FB successful for " + reporter.getDesignation() + ". Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". mode=" + mode);
										// if either of these fail, notify admin from within functions themselves
										boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
										boolean social_id_update_successful = p.updateSocialItemID(redirect_id, facebookresponse);
										se.sendMail("FB successful for " + reporter.getDesignation(), "Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". mode=" + mode + "\n\nhttps://www.huzon.tv/alert_monitor.html", "cyrus7580@gmail.com", "info@huzon.tv");
									} 
									catch (FacebookException e) 
									{
										return_jo.put("facebook_successful", false);
										return_jo.put("facebook_failure_message", e.getErrorMessage());
										
										if((e.getErrorCode() == 190) || (e.getErrorCode() == 100)) // if one of these errors was generated...
										{
											if(mode.equals("live"))
											{	
												reporter.resetFacebookCredentialsInDB(); // the credentials are no good anymore. Delete them to allow the user to start over.
												String emailmessage = getMissingCredentialsEmailMessage();
												se.sendMail("Action required: huzon.tv FB alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
												(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of invalid FB credentials. Actual FB response=" + facebookresponse + " user=" + postinguser.getDesignation() + ". mode=" + mode);
											}
											else
												(new Platform()).addMessageToLog("test account FB creds invalid trying to fire for " + reporter.getDesignation() + ". Actual FB response=" + facebookresponse + "\nuser=" + postinguser.getDesignation() + ". mode=" + mode);
											
										}
										else
										{	
											e.printStackTrace();
											(new Platform()).addMessageToLog("Failed facebook photo post. Unknown error. (This is not a user-has-not-linked issue.) Actual FB response=" + facebookresponse + " " + reporter.getDesignation() + " " + 
													new Long(reporter.getFacebookPageID()).toString() + " " + message + " " + e.getMessage() + "\nuser=" + postinguser.getDesignation() + ". mode=" + mode);
										}
									}
									station_object.unlock(uuid, "facebook");			
								}
								else
								{
									(new Platform()).addMessageToLog("Skipped FB post for " + reporter.getDesignation() + ". Tried to set lock but station.lock() returned false.  user=" + postinguser.getDesignation() + ". mode=" + mode);
								}
							}
							else
							{
								(new Platform()).addMessageToLog("Skipped FB post due to lock.\nreporter=" + reporter.getDesignation() + " user=" + postinguser.getDesignation() + ". mode=" + mode);
							}
							composite_file.delete();
							
						} catch (IOException e) {
							(new Platform()).addMessageToLog("IOException trying to create composite image and post to facebook." + "\nuser=" + postinguser.getDesignation() + ". mode=" + mode);
						}
					}
				}
			}
		}
		catch(MalformedURLException murle)
		{
			murle.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return return_jo;*/
	}
}
