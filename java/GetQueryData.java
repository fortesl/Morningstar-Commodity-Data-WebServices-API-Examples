/*
Copyright (c) 2012 Morningstar, inc. All rights reserved.

 Redistribution and use of this software in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of
 conditions and the following disclaimer in the documentation and/or other materials provided
 with the distribution.
 * The name of Morningstar inc. may not be used to endorse or promote products derived from 
 this software without specific prior written permission of Morningstar inc.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
/**
 * Retrieves Query results from a Morningstar Commodity Server using the WebServices REST API.
 * @Dependencies: the jersey-bundle and jsr311-api jar library files must be included in the java build path.
 * @author lfortes
 * @version 2012-04-05
 */

package webclient.examples;

import static javax.ws.rs.core.Response.Status.OK;

import java.text.NumberFormat;
import java.util.List;

import webclient.examples.data.generated.DataRequestResponse;
import webclient.examples.data.generated.Report;
import webclient.examples.data.generated.ReportBlock;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class GetQueryData {
	
	//can be set as command line arguments
    private static String USERNAME = "joe@lim.com";    
    private static String PASSWORD = "ILoveWS1";
    private static String BASE_URL = "https://ws.lim.com";
    
    //Data request URI
    private static final String DATA_REQ_RSC_PATH = "rs/api/datarequests";

    private static final int HTTP_CODE_OK = OK.getStatusCode();
    
    //Web client resource
    private WebResource webRsc;
    
    /**
     * Constructor
     * Gets web server resource
     */
    public GetQueryData() {
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
        // Apply basic authentication.
        client.addFilter(new HTTPBasicAuthFilter(USERNAME, PASSWORD));
        // Define the base URL for the web resource.
        webRsc = client.resource(BASE_URL);
    }
    
    /**
     * usage
     * Required argument: Query 
     * Optional argument: output file name (XML response will be sent to file if given).
     * Optional argument: username.
     * Optional argument: password.
     * Optional argument: webserver.
     */
	private static void usage()
	{
    	System.out.println("Usage: " + GetQueryData.class.getName() + 
    			" <QueryString>" +
    			" [-username <username>] [-password <password>] [-webserver <webserver>]");
    	System.out.println("Example: " + GetQueryData.class.getName() +
    			" \"SHOW 1: Close of NG WHEN Date is within 1 year\" -out temp/xx.xml");
    	System.exit(1);
	}
   
	
	   /**
     * Send HTTP post request to server and returns XML String response
     * @param queryText
     * @return
	 * @throws InterruptedException 
     */
    private DataRequestResponse sendRequestAndGetServerResponse(String queryText)
    {
    	// Prepare the request pay-load with the given query.
		String queryPayload = "<DataRequest><Query><Text>" + queryText + "</Text></Query></DataRequest>";

        // Issue the POST HTTP request specifying URL path and media type. The pay-load is conveyed by the query.
        ClientResponse response = webRsc.path(DATA_REQ_RSC_PATH).
        		accept(MediaType.TEXT_XML).type(MediaType.TEXT_XML).
        		post(ClientResponse.class, queryPayload);
        
        // Check the HTTP response status.
        if (HTTP_CODE_OK != response.getStatus()) {
            // Present the error when something went wrong and bail out.
            throw new IllegalStateException("Initial request failed with response [" + response + "]");
        }

        // The request went well. Get the result from the response pay-load.
        DataRequestResponse respPayload = response.getEntity(DataRequestResponse.class);
        //check the status code.
        while (respPayload.getStatus() == 200) {
        	// When the payload status indicates waiting-on-result, retry the request with ID
        	try {
        		Thread.sleep(250);
        	}catch (InterruptedException e) {
        		System.out.println(e.getMessage());
        	}
        
        	long jobId = respPayload.getId();
        	response = webRsc.path(DATA_REQ_RSC_PATH + "/" + jobId).get(ClientResponse.class);
            if (HTTP_CODE_OK != response.getStatus()) {
                throw new IllegalStateException("Error while polling for results [" + response + "]");
            }
            respPayload = response.getEntity(DataRequestResponse.class);
        }
        
        //at this point, we have received a valid response with good pay-load data.
        return respPayload;
	}

	/**
	 * print string XML response to Console
	 * @param respPayload
	 * @param query
	 */
	private void printReportToConsole(DataRequestResponse respPayload, String query)
	{
    	NumberFormat formatter = NumberFormat.getInstance();
    	formatter.setMaximumFractionDigits(3);
    	formatter.setMinimumFractionDigits(2);

    	//Get report objects
		List <Report> reports = respPayload.getReports();

    	//print user input query
    	System.out.println("User Query: \n" + query);
    	System.out.println();
    	
    	//process each report
    	for (Report report: reports) {
    		
    		//print report title
    		if (report.getTitle().length() >0)
    			System.out.println("\nReport title: " + report.getTitle());

        	//process each reportBlock in report
        	List <ReportBlock> reportBlocks = report.getReportBlocks();
        	int block = 1;
            for (ReportBlock reportBlock : reportBlocks) {
                String [] headings = (String[]) reportBlock.getColumnHeadings().toArray(new String[]{}); 
                String [] dates = (String[]) reportBlock.getRowDates().toArray(new String[]{});
                Double [] values = (Double[]) reportBlock.getValues().toArray(new Double[]{});
                
                //print reportBlock header (column names)
                System.out.print("Block: " + block++);
                for (int i=0; i<headings.length; i++)
                	System.out.print("\t\t" + headings[i]);
                System.out.println();
                
                //print dates & values
                for (int k = 0; k < dates.length; k++) {
                    System.out.print(dates[k]);
                    for (int i=0; i<headings.length; i++) {
                        String value_str = "NaN";
                        int idx = reportBlock.getNumCols() * k + i;
                        if (!Double.isNaN(values[idx]))
                        	value_str = formatter.format(values[idx]);
                        System.out.print("\t\t" + value_str);
                        }
                    System.out.println();
                    }
                System.out.println("Number of Dates: " + reportBlock.getNumRows() + "\n");                
               }
    	}
	}
    
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length < 1) {
        	usage();
        }
        
	    for (int i = 1; i < args.length; i++) {
	    	if (args[i].equals("-username")) {
	    		i++;
	    		USERNAME = args[i];
	    	}
	    	else if (args[i].equals("-password")) {
	    		i++;
	    		PASSWORD = args[i];
	    	}
	    	else if (args[i].equals("-webserver")) {
	    		i++;
	    		BASE_URL = args[i];
	    	}
	    	
	    }

        GetQueryData c = new GetQueryData();
	    String query = args[0];

	    DataRequestResponse respPayload = c.sendRequestAndGetServerResponse(query);
	    c.printReportToConsole(respPayload, query);
	}

}
