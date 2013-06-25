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
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class TwitterUploaderCallable implements Callable<JSONObject> {

	Frame frame2upload;
	User reporter;
	Station station_object;
	//boolean twitter_successful;
	//String twitter_failure_message;
	boolean simulation;
	
	public TwitterUploaderCallable(Frame inc_frame2upload, User inc_reporter, Station inc_station_object, boolean inc_simulation)
	{
		frame2upload = inc_frame2upload;
		station_object = inc_station_object;
		reporter = inc_reporter;
		//twitter_successful = false;
		//twitter_failure_message = "Init failure";
		simulation = inc_simulation;
	}
	
	/*public boolean getTwitterSuccessful()
	{
		return twitter_successful;
	}
	
	public String getTwitterFailureMessage()
	{
		return twitter_failure_message;
	}*/
	
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
	
	@Override
	public JSONObject call() {
		SimpleEmailer se = new SimpleEmailer();
		JSONObject return_jo = new JSONObject();
		try
		{
			(new Platform()).addMessageToLog("Tweet triggered for " + reporter.getDesignation() + "\n\nurl=" + frame2upload.getURLString() + "\n\nsimulation=" + simulation);
			
			// check to see that reporter has twitter credentials on file. If not, email the reporter and send email to admin.
			if(reporter.getTwitterAccessToken() == null || reporter.getTwitterAccessToken().equals("") || reporter.getTwitterAccessTokenSecret() == null || reporter.getTwitterAccessTokenSecret().equals(""))
			{
				return_jo.put("twitter_successful", false);
				return_jo.put("twitter_failure_message", "user_had_no_credentials");
				
				reporter.resetTwitterCredentialsInDB(); // at least one credential was missing, wipe out both tat and tats in db
				if(!simulation)
				{	
					String emailmessage = getMissingCredentialsEmailMessage();
					// send to reporter and to admin
					se.sendMail("Action required: huzon.tv Twitter alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
					(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of missing Twitter credentials");
				}
				else
				{
					(new Platform()).addMessageToLog(reporter.getDesignation() + " WOULD HAVE BEEN notified of missing TW creds, but this is a simulation");
				}
			}
			else // user appears to have twitter credentials
			{
				if(!simulation) // go ahead with the tweet
				{	
					System.out.println("TwitterUploaderCallable.call(): entering real post to reporter account");
					URL[] image_urls = frame2upload.get2x2CompositeURLs();
					if(image_urls == null)
					{
						System.out.println("TwitterUploaderCallable.call(): image_urls was null coming back from Frame.get2x2CompositeURLs()");
						return_jo.put("twitter_successful", false);
						return_jo.put("twitter_failure_message", "image_urls was null coming back from Frame.get2x2CompositeURLs()");
					}
					else
					{
						System.out.println("TwitterUploaderCallable.call(): image urls populated. Creating composite.");
						File[] image_files = new File[4];
						String tmpdir = System.getProperty("java.io.tmpdir");
						
						System.out.println("TwitterUploaderCallable.call(): downloading " + image_urls[0] + " and saving to " + tmpdir + "/image_for_twitter0.jpg");
						ReadableByteChannel rbc0 = Channels.newChannel(image_urls[0].openStream());
						FileOutputStream fos0 = new FileOutputStream(tmpdir + "/image_for_twitter0.jpg"); // FIXME this might have conflicts with multiple stations
						fos0.getChannel().transferFrom(rbc0, 0, Long.MAX_VALUE);
						image_files[0] = new File(tmpdir + "/image_for_twitter0.jpg");
						fos0.close();
						System.out.println("TwitterUploaderCallable.call(): downloading " + image_urls[1] + " and saving to " + tmpdir + "/image_for_twitter1.jpg");
						ReadableByteChannel rbc1 = Channels.newChannel(image_urls[1].openStream());
						FileOutputStream fos1 = new FileOutputStream(tmpdir + "/image_for_twitter1.jpg"); // FIXME this might have conflicts with multiple stations
						fos1.getChannel().transferFrom(rbc1, 0, Long.MAX_VALUE);
						image_files[1] = new File(tmpdir + "/image_for_twitter1.jpg");
						
						System.out.println("TwitterUploaderCallable.call(): downloading " + image_urls[2] + " and saving to " + tmpdir + "/image_for_twitter2.jpg");
						ReadableByteChannel rbc2 = Channels.newChannel(image_urls[2].openStream());
						FileOutputStream fos2 = new FileOutputStream(tmpdir + "/image_for_twitter2.jpg"); // FIXME this might have conflicts with multiple stations
						fos2.getChannel().transferFrom(rbc2, 0, Long.MAX_VALUE);
						image_files[2] = new File(tmpdir + "/image_for_twitter2.jpg");
						
						System.out.println("TwitterUploaderCallable.call(): downloading " + image_urls[3] + " and saving to " + tmpdir + "/image_for_twitter3.jpg");						
						ReadableByteChannel rbc3 = Channels.newChannel(image_urls[3].openStream());
						FileOutputStream fos3 = new FileOutputStream(tmpdir + "/image_for_twitter3.jpg"); // FIXME this might have conflicts with multiple stations
						fos3.getChannel().transferFrom(rbc3, 0, Long.MAX_VALUE);
						image_files[3] = new File(tmpdir + "/image_for_twitter3.jpg");
						fos0.close();fos1.close();fos2.close();fos3.close();
						
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
							System.out.println("TwitterUploaderCallable.call(): writing composite image.");
							ImageIO.write(after, "PNG", new File(tmpdir, "image_for_twitter.png"));
							File imagefile = new File(tmpdir + "/image_for_twitter.png");
							
							Twitter twitter = new Twitter();
							Platform p = new Platform();

							long redirect_id = p.createAlertInDB(station_object, "twitter", reporter.getDesignation(), frame2upload.getURLString());
							String message = station_object.getMessage("twitter", frame2upload.getTimestampInMillis(), redirect_id, reporter);
							
							System.out.println("TwitterUploaderCallable.call(): posting image to admin twitter account.");
							JSONObject twit_jo = twitter.updateStatusWithMedia(reporter.getTwitterAccessToken(), reporter.getTwitterAccessTokenSecret(), message, imagefile);
							
							if(twit_jo.has("response_status") && twit_jo.getString("response_status").equals("error")) // if an error was produced
							{
								System.out.println("TwitterUploaderCallable.call(): Error from twitter");
								return_jo.put("twitter_successful", false);
								return_jo.put("twitter_failure_message", twit_jo.getString("message"));

								if(twit_jo.has("twitter_code") && (twit_jo.getInt("twitter_code") == 32 || twit_jo.getInt("twitter_code") == 89)) // and it was due to bad credentials
								{
									reporter.resetTwitterCredentialsInDB(); // the credentials are no good anymore. Delete them to allow the user to start over. (Link is in email below)
									String emailmessage = getMissingCredentialsEmailMessage();
									// send to reporter and to admin
									se.sendMail("Action required: huzon.tv Twitter alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
									(new Platform()).addMessageToLog(reporter.getDesignation() + " was notified of a disconnected Twitter account");
								}
								else if(twit_jo.has("twitter_code"))
								{
									String emailmessage = getMissingCredentialsEmailMessage();
									// send mail to admin
									(new Platform()).addMessageToLog(reporter.getDesignation() + " unknown twitter error. There was an unknown error trying to tweet. twit_jo=" + twit_jo);
								}
								else
								{
									String emailmessage = getMissingCredentialsEmailMessage();
									// send mail to admin
									(new Platform()).addMessageToLog(reporter.getDesignation() + " some other twitter error. There was some other error trying to tweet which DID NOT produce a twitter_code. twit_jo=" + twit_jo);
								}
									
							}
							else
							{
								System.out.println("TwitterUploaderCallable.call(): No error. Tweet should have been successful");
								return_jo.put("twitter_successful", true); // the twitter post was successful, regardless of the two following db updates.
								(new Platform()).addMessageToLog("Tweet successful for " + reporter.getDesignation());
								// if either of these fail, alert the admin within the functions themselves
								boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
								boolean social_id_update_successful = p.updateSocialItemID(redirect_id,twit_jo.getString("id"));
							}
						} catch (IOException e) {
							(new Platform()).addMessageToLog("IOException trying to create composite image and post to twitter." + e.getMessage());
						}
					}
				}
				else
				{
					return_jo.put("twitter_successful", false);
					return_jo.put("twitter_failure_message", "simulation");
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
