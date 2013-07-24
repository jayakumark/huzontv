package tv.huzon;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.Callable;


import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

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

	public GAF4ReporterCallable(User inc_reporter, Station inc_station_object, long inc_begin, long inc_end, double inc_mamodifier, double inc_singlemodifier, int inc_awp, int inc_nrpst)
	{
		begin = inc_begin;
		end = inc_end;
		mamodifier = inc_mamodifier;
		singlemodifier = inc_singlemodifier;
		awp = inc_awp;
		nrpst = inc_nrpst; // number required past single thresh
		reporter = inc_reporter;
		station_object = inc_station_object;
	}
	
	public JSONArray call() {
		long ts = 0L;
		double homogeneity = reporter.getHomogeneity();
		String designation = reporter.getDesignation();
		double single_thresh = homogeneity * singlemodifier;
		double ma_thresh = homogeneity * mamodifier;
		JSONArray alert_frames_ja = new JSONArray();
		try
		{
			TreeSet<Frame> frames_past_single_thresh = station_object.getFrames((begin*1000), (end*1000), designation);
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
					moving_average = currentframe.getMovingAverage6(designation);
					frame_that_passed_ma_thresh = null;
					if(moving_average == -1)
					{
						//System.out.println("GAF4ReporterCallable.call() There were not enough frames in this window. Skip this frame.");
					}
					else
					{
						if(moving_average > ma_thresh && moving_average == currentframe.getHighestMA6())
						{
							frame_that_passed_ma_thresh = currentframe;
						}
						else // initial frame didn't pass, look for subsequent frames that pass the ma threshold.
						{
							ts = currentframe.getTimestampInMillis();
							TreeSet<Frame> subsequent_frames = station_object.getFrames(ts, (ts + 1000*mawindow), null);
							Iterator<Frame> subsequent_frames_it = subsequent_frames.iterator();
							while(subsequent_frames_it.hasNext())
							{
								subsequentframe = subsequent_frames_it.next();
								moving_average = subsequentframe.getMovingAverage6(designation);
								if(moving_average > ma_thresh && moving_average == subsequentframe.getHighestMA6())
								{
									frame_that_passed_ma_thresh = subsequentframe;
									break;
								}
								else
								{
									//System.out.println("GAF4ReporterCallable.call() ma of subsequent DID NOT pass req thresh. ma=" + moving_average + " thresh=" + ma_thresh);
								}
							}
						}
						
						int num_frames_in_window_above_single_thresh = 0;
						if(frame_that_passed_ma_thresh != null) 
						{
							num_frames_in_window_above_single_thresh = frame_that_passed_ma_thresh.getNumFramesInWindowAboveSingleThresh(designation, single_thresh);
							if(num_frames_in_window_above_single_thresh >= nrpst)
							{	
								JSONObject jo2add = frame_that_passed_ma_thresh.getAsJSONObject(true, null); // no designation specified
								jo2add.put("designation", designation);
								jo2add.put("ma_for_alert_frame", currentframe.getMovingAverage6(designation));
								jo2add.put("ma_for_frame_that_passed_ma_thresh", frame_that_passed_ma_thresh.getMovingAverage6(designation));
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
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		return alert_frames_ja;
		
	}
}
