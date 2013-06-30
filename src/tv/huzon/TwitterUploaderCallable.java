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
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class TwitterUploaderCallable implements Callable<JSONObject> {

	Frame frame2upload;
	User reporter;
	Station station_object;
	String mode; // "live" = post to reporter's account, if possible
	 // "test" = post to test account, if possible
	 // "silent" = don't post anything
	
	public TwitterUploaderCallable(Frame inc_frame2upload, User inc_reporter, Station inc_station_object, String inc_mode)
	{
		frame2upload = inc_frame2upload;
		station_object = inc_station_object;
		reporter = inc_reporter;
		mode = inc_mode;
	}
	
	public String getMissingCredentialsEmailMessage()
	{
		String message = "";
		
			message = 
				reporter.getDisplayName() + 
				",\n\nAn alert triggered for you with huzon.tv. However, our system was unable to actually fire the alert because your Twitter account " + 
				"has become disconnected from huzon.tv. This can happen for several reasons:" +
				"\n\n- You disabled the huzon.tv app in your Twitter configuration" +
				"\n- Your Twitter account was never linked to huzon.tv in the first place"+
				"\n\nPlease go to https://www.huzon.tv/registration.html to link your Twitter account to huzon.tv and enable automated alerts. " +
				"Thanks!\n\nhuzon.tv staff\n\nPS: Here's the image that would have posted: " + frame2upload.getURLString();
			return message;
	}	
	
	public JSONObject call() {
		SimpleEmailer se = new SimpleEmailer();
		JSONObject return_jo = new JSONObject();
		try
		{
			(new Platform()).addMessageToLog("TW triggered for " + reporter.getDesignation() + " mode=" + mode);
			
			if(mode.equals("silent"))
			{
				return_jo.put("twitter_successful", false);
				return_jo.put("twitter_failure_message", "silent");
				(new Platform()).addMessageToLog("FB triggered but suppressed for " + reporter.getDesignation() + ". mode=silent");
			}
			else if(mode.equals("test") || mode.equals("live"))
			{
				User postinguser = null;
				if(mode.equals("test"))
					postinguser = new User("huzon_master", "designation");
				else
					postinguser = reporter;
				
				if(postinguser.getTwitterAccessToken() == null || postinguser.getTwitterAccessToken().equals("") || postinguser.getTwitterAccessTokenSecret() == null || postinguser.getTwitterAccessTokenSecret().equals(""))
				{
					return_jo.put("twitter_successful", false);
					return_jo.put("twitter_failure_message", "user " + postinguser.getDesignation() + " has no twitter credentials");
					(new Platform()).addMessageToLog("TW triggered for " + reporter.getDesignation() + " but failed due to lack of tw credentials. user=" + postinguser.getDesignation() + ". mode=" + mode);
					if(mode.equals("live"))
					{
						String emailmessage = getMissingCredentialsEmailMessage();
						se.sendMail("Action required: huzon.tv TW alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
						(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of missing TW credentials.");
					}
				}
				else // postinguser appears to have twitter credentials
				{
					URL[] image_urls = frame2upload.get2x2CompositeURLs();
					if(image_urls == null)
					{
						return_jo.put("twitter_successful", false);
						return_jo.put("twitter_failure_message", "image_urls was null coming back from Frame.get2x2CompositeURLs()");
						(new Platform()).addMessageToLog("TW triggered for " + reporter.getDesignation() + " with cred. but 2x2 failed. user=" + postinguser.getDesignation() + ". mode=" + mode);
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
						
						System.out.println("TwitterUploaderCallable.call(): downloading " + image_urls[0] + " and saving to " + image_filenames[0]);
						ReadableByteChannel rbc0 = Channels.newChannel(image_urls[0].openStream());
						FileOutputStream fos0 = new FileOutputStream(image_filenames[0]); // FIXME this might have conflicts with multiple stations
						fos0.getChannel().transferFrom(rbc0, 0, Long.MAX_VALUE);
						image_files[0] = new File(image_filenames[0]);
						fos0.close();
						
						System.out.println("TwitterUploaderCallable.call(): downloading " + image_urls[1] + " and saving to " + image_filenames[1]);
						ReadableByteChannel rbc1 = Channels.newChannel(image_urls[1].openStream());
						FileOutputStream fos1 = new FileOutputStream(image_filenames[1]); // FIXME this might have conflicts with multiple stations
						fos1.getChannel().transferFrom(rbc1, 0, Long.MAX_VALUE);
						image_files[1] = new File(image_filenames[1]);
						fos1.close();
						
						System.out.println("TwitterUploaderCallable.call(): downloading " + image_urls[2] + " and saving to " + image_filenames[2]);
						ReadableByteChannel rbc2 = Channels.newChannel(image_urls[2].openStream());
						FileOutputStream fos2 = new FileOutputStream(image_filenames[2]); // FIXME this might have conflicts with multiple stations
						fos2.getChannel().transferFrom(rbc2, 0, Long.MAX_VALUE);
						image_files[2] = new File(image_filenames[2]);
						fos2.close();
						
						System.out.println("TwitterUploaderCallable.call(): downloading " + image_urls[3] + " and saving to " + image_filenames[3]);				
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
							
							// Save as new image
							String composite_filename = tmpdir + "/" + UUID.randomUUID() + ".png";
							System.out.println("TwitterUploaderCallable.call(): writing composite image.");
							ImageIO.write(after, "PNG", new File(composite_filename));
							File composite_file = new File(composite_filename);
							
							image_files[0].delete();
							image_files[1].delete();
							image_files[2].delete();
							image_files[3].delete();
							
							
							if(!station_object.isLocked("twitter"))
							{
								String uuid = UUID.randomUUID().toString();
								boolean successfullylocked = station_object.lock(uuid, "twitter");
								
								if(successfullylocked)
								{	
									Twitter twitter = new Twitter();
									Platform p = new Platform();

									long redirect_id = p.createAlertInDB(station_object, "twitter", reporter.getDesignation(), frame2upload.getURLString());
									String message = station_object.getMessage("twitter", frame2upload.getTimestampInMillis(), redirect_id, reporter);
									
									System.out.println("TwitterUploaderCallable.call(): posting image to admin twitter account.");
									
									JSONObject twit_jo = null;
									twit_jo = twitter.updateStatusWithMedia(postinguser.getTwitterAccessToken(), postinguser.getTwitterAccessTokenSecret(), message, composite_file);
									
									if(twit_jo.has("response_status") && twit_jo.getString("response_status").equals("error")) // if an error was produced
									{
										return_jo.put("twitter_successful", false);
										return_jo.put("twitter_failure_message", twit_jo.getString("message"));

										if(twit_jo.has("twitter_code") && (twit_jo.getInt("twitter_code") == 32 || twit_jo.getInt("twitter_code") == 89)) // and it was due to bad credentials
										{
											if(mode.equals("live"))
											{
												reporter.resetTwitterCredentialsInDB(); // the user's credentials are no good anymore. Delete them to allow the user to start over. (Link is in email below)
												String emailmessage = getMissingCredentialsEmailMessage();
												se.sendMail("Action required: huzon.tv Twitter alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
												(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of a disconnected Twitter account");
											}
											else
											{
												(new Platform()).addMessageToLog("test account TW creds invalid trying to fire for " + reporter.getDesignation() + ". user=" + postinguser.getDesignation() + ". mode=" + mode);
											}
										}
										else if(twit_jo.has("twitter_code"))
										{
											(new Platform()).addMessageToLog(reporter.getDesignation() + " unknown twitter error. There was an unknown error trying to tweet. twit_jo=" + twit_jo + "user=" + postinguser.getDesignation() + ". mode=" + mode);
										}
										else
										{
											(new Platform()).addMessageToLog(reporter.getDesignation() + " some other twitter error. There was some other error trying to tweet which DID NOT produce a twitter_code. twit_jo=" + twit_jo + " user=" + postinguser.getDesignation() + ". mode=" + mode);
										}
									}
									else
									{
										return_jo.put("twitter_successful", true); // the twitter post was successful, regardless of the two following db updates.
										(new Platform()).addMessageToLog("TW successful for " + reporter.getDesignation() + " user=" + postinguser.getDesignation() + ". mode=" + mode);
										// if either of these fail, alert the admin within the functions themselves
										boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
										boolean social_id_update_successful = p.updateSocialItemID(redirect_id,twit_jo.getString("id"));
										se.sendMail("TW successful for " + reporter.getDesignation(), "user=" + postinguser.getDesignation() + ". mode=" + mode + "\n\nhttps://www.huzon.tv/alert_monitor.html", "cyrus7580@gmail.com", "info@huzon.tv");
									}
									station_object.unlock(uuid, "twitter");		
								}
								else
								{
									(new Platform()).addMessageToLog("Skipped TW post for " + reporter.getDesignation() + ". Tried to set lock but station.lock() returned false.  user=" + postinguser.getDesignation() + ". mode=" + mode);
								}
							}
							else
							{
								(new Platform()).addMessageToLog("Skipped TW post due to lock.\nreporter=" + reporter.getDesignation() + " user=" + postinguser.getDesignation() + ". mode=" + mode);
							}
							composite_file.delete();
							
						} catch (IOException e) {
							(new Platform()).addMessageToLog("IOException trying to create composite image and post to twitter for " + reporter.getDesignation() + ". " + e.getMessage() + " user=" + postinguser.getDesignation() + ". mode=" + mode);
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
		return return_jo;
	}
}
