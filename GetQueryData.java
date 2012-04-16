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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
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
    
    private long jobID;
    
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
    			" <QueryString> [-out <XMLOutputFileName>]" +
    			" [-username <username>] [-password <password>] [-webserver <webserver>]");
    	System.out.println("Example: " + GetQueryData.class.getName() +
    			" \"SHOW 1: Close of NG WHEN Date is within 1 year\" -out temp/xx.xml");
    	System.exit(1);
	}
   
	
	   /**
     * Send HTTP post request to server and returns XML String response
     * @param query
     * @return
	 * @throws XMLStreamException 
	 * @throws UniformInterfaceException 
	 * @throws ClientHandlerException 
	 * @throws UnsupportedEncodingException 
	 * @throws InterruptedException 
     */
    String sendRequestAndGetServerResponse(String queryText) throws UnsupportedEncodingException, ClientHandlerException, UniformInterfaceException, XMLStreamException, InterruptedException
    {
    	ClientResponse resp = null;
    	String response = null;

    	// Prepare the request pay-load with the given query.
		String query = "<DataRequest>" + 
				"<Query>"+ 
				"<Text>" + 
				queryText + 
				"</Text>"+ 
				"</Query>"+ 
				"</DataRequest>";

        // Issue the POST HTTP request specifying URL path and media type. The pay-load is conveyed by the query.
        resp = webRsc.path(DATA_REQ_RSC_PATH).
        		accept(MediaType.TEXT_XML).type(MediaType.TEXT_XML).
        		post(ClientResponse.class, query);
        
        response = resp.getEntity(String.class);
        // Check the HTTP response status.
        if (HTTP_CODE_OK != resp.getStatus()) {
            // Present the error when something went wrong and bail out.
            throw new IllegalStateException("Initial request failed with response [" + resp + "]");
        }
        
        
        //Also check the response status attribute of the returned XML
        //200 is still processing.
        while (getDataRequestResponseStatus(response) == 200) {
            // When the status indicates waiting-on-result, retry the request with ID.
            Thread.sleep(250);
            resp = webRsc.path(DATA_REQ_RSC_PATH + "/" + jobID).get(ClientResponse.class);
            if (HTTP_CODE_OK != resp.getStatus()) {
                throw new IllegalStateException("Error while polling for results [" + resp + "]");
            }
            response = resp.getEntity(String.class);
        }
        
        return response;        
	}

	@SuppressWarnings("unchecked")
    private int getDataRequestResponseStatus(String response) throws XMLStreamException, UnsupportedEncodingException
    {
    	int status =0;
    	
    	//First create a new XMLInptFactory
    	XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    	//Setup a new eventReader
    	InputStream in = new ByteArrayInputStream(response.getBytes("UTF-8"));
    	XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
    	
    	//walk through XML document
    	while (eventReader.hasNext()) {
    		XMLEvent event = eventReader.nextEvent();
    		if (event.isStartElement()) {
    			StartElement startElement = event.asStartElement();
    			//If this is a DataRequestResponse, get status
    			if (startElement.getName().getLocalPart().equals("DataRequestResponse")) {
					Iterator <Attribute> attributes = startElement.getAttributes();
    				while (attributes.hasNext()) {
    					Attribute attribute = attributes.next();
    					if (attribute.getName().toString().equals("status"))
    						status = Integer.parseInt(attribute.getValue());
    					else if (attribute.getName().toString().equals("id"))
    						jobID = Integer.parseInt(attribute.getValue());
    				}
    				break;
    			}
    		}
    	}
		return status;
    }
    
    /**
     * Parses Data XML response to console
     * @param response XML formated response
     * @throws UnsupportedEncodingException 
     * @throws XMLStreamException 
     */
    @SuppressWarnings("unchecked")
	private List <Report> parseDataXMLResponse(String response) throws UnsupportedEncodingException, XMLStreamException
    {
    	//First create a new XMLInptFactory
    	XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    	//Setup a new eventReader
    	InputStream in = new ByteArrayInputStream(response.getBytes("UTF-8"));
    	XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
    	
    	//List of report objects
    	List <Report> reports = new ArrayList<Report>();
    	Report report = null;
    	ReportBlock reportBlock = null;
    	
    	//walk through XML document
    	while (eventReader.hasNext()) {
    		XMLEvent event = eventReader.nextEvent();
    		if (event.isStartElement()) {
    			StartElement startElement = event.asStartElement();
    			//If this is a RelInfo element print its attributes
    			if (startElement.getName().getLocalPart().equals("Reports")) {
					Iterator<Attribute> attributes = startElement.getAttributes();
    				while (attributes.hasNext()) {
    					Attribute attribute = attributes.next();
    					if (attribute.getName().toString().equals("title")) {
    						report = new Report(attribute.getValue());
    					}
    				}
    			}
    			else if (startElement.getName().getLocalPart().equals("ReportBlocks")) {
					int cols = 0, rows = 0;
					Iterator<Attribute> attributes = startElement.getAttributes();
    				while (attributes.hasNext()) {
    					Attribute attribute = attributes.next();
    					if (attribute.getName().toString().equals("numCols")) {
    						cols = Integer.parseInt(attribute.getValue());
    					}
    					else if (attribute.getName().toString().equals("numRows")) {
    						rows = Integer.parseInt(attribute.getValue());
    					}
    				}
    				reportBlock = new ReportBlock(cols, rows);
    			}
    		}
        	if (event.isStartElement()) {
    			if (event.asStartElement().getName().getLocalPart().equals("RowDates")) {
    				event = eventReader.nextEvent();
    				reportBlock.addRowDate(event.asCharacters().getData());
    			}
    			else if (event.asStartElement().getName().getLocalPart().equals("ColumnHeadings")) {
    				event = eventReader.nextEvent();
    				reportBlock.addColumnHeading(event.asCharacters().getData());
    			}
    			else if (event.asStartElement().getName().getLocalPart().equals("Values")) {
    				event = eventReader.nextEvent();
    				reportBlock.addValue(Double.parseDouble(event.asCharacters().getData()));
    			}
    		}
    		else if (event.isEndElement()) {
    			EndElement endElement = event.asEndElement();
    			if (endElement.getName().toString().equals("ReportBlocks")) {
    				report.addReportBlock(reportBlock);
    			}
    			else if (endElement.getName().toString().equals("Reports")) {
    				reports.add(report);
    			}
    		}
    	}
    	
    	return reports;
    }
    
    /**
     * prints string XML response to given file
     * @param fileName user specified output file
     * @param response server XML response
     * @return
     */
	private void printXMLResponseToFile(String fileName, String response)
	{
		if (!fileName.contains(".xml") || !fileName.contains(".XML"))
			fileName.concat(".xml");
	    File f = new File(fileName);
		if(f.exists()) {
			f.delete();
		}
		PrintWriter pw;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
			pw.println(response);
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done. output written to " + fileName);
	}
	
	
	private void printReport(List<Report> reports, String query)
	{
    	NumberFormat formatter = NumberFormat.getInstance();
    	formatter.setMaximumFractionDigits(3);
    	formatter.setMinimumFractionDigits(2);

    	//print user input query
    	System.out.println("User Query: \n" + query);
    	
    	//process each report
    	for (Report report: reports) {
    		
    		//print report title
            System.out.println("\nReport title: " + report.getTitle());

        	//process each reportBlock in report
        	List <ReportBlock> reportBlocks = report.getReportBlocks();
        	int block = 1;
            for (ReportBlock reportBlock : reportBlocks) {
                String [] headings = reportBlock.getColumnHeadings();
                String [] dates = reportBlock.getRowDates();
                double [] values = reportBlock.getValues();
                
                //print reportBlock header (column names)
                System.out.print("Block: " + block++);
                for (int i = 0; i < headings.length; i++ )
                	System.out.print("\t\t" + headings[i]);
                System.out.println();
                
                //print dates & values
                for (int k = 0; k < dates.length; k++) {
                    System.out.print(dates[k]);
                    for (int i = 0; i < headings.length; i++ ) {
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
	 * @throws XMLStreamException 
	 * @throws UnsupportedEncodingException 
	 * @throws InterruptedException 
	 * @throws UniformInterfaceException 
	 * @throws ClientHandlerException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException, XMLStreamException, ClientHandlerException, UniformInterfaceException, InterruptedException {
        if (args.length < 1) {
        	usage();
        }
        String fileName = null;
        
	    for (int i = 1; i < args.length; i++) {
	    	if (args[i].equals("-out")) {
	    		i++;
	    		fileName = args[i];
	    	}
	    	else if (args[i].equals("-username")) {
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
	    String response = null;
	    String query = args[0];

	    response = c.sendRequestAndGetServerResponse(query);
	    
	    if (fileName != null)
	    	//XML output file specified.
	    	c.printXMLResponseToFile(fileName, response);
	    else {
	    	//Parse response into object model
	    	List<Report> reports = c.parseDataXMLResponse(response);
	    	//display on console
	    	c.printReport(reports, query);
	    }
	}

}
