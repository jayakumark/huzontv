
document.addEventListener('DOMContentLoaded', function () {
	
	var designations = null;
	$.ajax({
		type: 'GET',
		url: "http://hoozontv.elasticbeanstalk.com/endpoint",
		data: {
            method: "getDesignations",
            station: "wkyt"       
		},
        dataType: 'json',
        async: false,
        success: function (data, status) {
        	if (data.response_status == "error")
        		$("#main_div").html("error getting station designations");
        	else
        	{
        		designations = data.designations;
        		$("#main_div").html("Designations successfully retrieved.");
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#main_div").html("ajax error");
            console.log(textStatus, errorThrown);
        }
	});
	
	var mds = "";
	mds = mds + "<div style=\"font-size:16px;font-weight:bold\">";
	
	mds = mds + "<table style=\"margin-right:auto;margin-left:auto;border-spacing:10px\">";
	mds = mds + "	<tr>";
	mds = mds + "		<td colspan=4 style=\"vertical-align:top;text-align:left\">";
	mds = mds + "			<span style=\"color:green;font-weight:bold\">GREEN</span> = account is linked</span><br>";
	mds = mds + "			<span style=\"color:red;font-weight:bold\">RED</span> = account is not linked</span><br>";
	mds = mds + "		</td>";
	var min = 0;
	var max = 1;
	// and the formula is:
	var random = 0;
	for(var a = 0; a < designations.length; a++)
	{
		
		mds = mds + "	<tr>";
		mds = mds + "		<td style=\"vertical-align:top;text-align:left;\">";
		mds = mds + "							" + designations[a] + "";
		mds = mds + "		</td>";
		random = Math.floor(Math.random() * (max - min + 1)) + min;
		mds = mds + "		<td style=\"vertical-align:top;text-align:left;\">";
		if(random == 0)
			mds = mds + " 			<a href=\"blah\" style=\"color:red\">twitter</A>";
		else
			mds = mds + " 			<a href=\"blah\" style=\"color:green\">twitter</A>";
		mds = mds + "		</td>";
		random = Math.floor(Math.random() * (max - min + 1)) + min;
		mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
		if(random == 0)
			mds = mds + " 			<a href=\"blah\" style=\"color:red\">facebook</A>";
		else
			mds = mds + " 			<a href=\"blah\" style=\"color:green\">facebook</A>";
		mds = mds + "		</td>";
		random = Math.floor(Math.random() * (max - min + 1)) + min;
		mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
		if(random == 0)
			mds = mds + " 			<a href=\"blah\" style=\"color:red\">google+</A>";
		else
			mds = mds + " 			<a href=\"blah\" style=\"color:green\">google+</A>";
		mds = mds + "		</td>";
		mds = mds + "	</tr>";
	}	
	mds = mds + "</table>";
	mds = mds + "</div>";
	$("#main_div").html(mds);
});
	
	

