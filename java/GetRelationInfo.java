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
 * Retrieves relation information from a Morningstar Commodity Server using the WebServices REST API.
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
import java.util.Iterator;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class GetRelationInfo {
	
	//can be set as command line arguments
    private static String USERNAME = "joe@lim.com";    
    private static String PASSWORD = "ILoveWS1";
    private static String BASE_URL = "http://ausqadev01.lim.com:9090";

    //metadata URI
    private static final String METADATA_REQ_RSC_PATH = "rs/api/schema/relations";

    private static final int HTTP_CODE_OK = OK.getStatusCode();
    
    //web client resource
    private WebResource webRsc;
    
    /**
     * Constructor
     * Gets web client resource
     */
    public GetRelationInfo() {
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
        // Apply basic authentication.
        client.addFilter(new HTTPBasicAuthFilter(USERNAME, PASSWORD));
        // Define the base URL for the web resource.
        webRsc = client.resource(BASE_URL);
    }
    
    /**
     * usage
     * Required argument: Relation name(s). 
     * Optional argument: output file name (XML response will be sent to file if given).
     * Optional argument: query parameter attributes.
     * Optional argument: username.
     * Optional argument: password.
     * Optional argument: webserver.
     */
	private static void usage()
	{
    	System.out.println("Usage: " + GetRelationInfo.class.getName() +
    			" <relations> [-attr <attributes>] [-out <XMLOutputFileName>]" +
    			" [-username <username>] [-password <password>] [-webserver <webserver>]");
    	System.out.println("Example: " + GetRelationInfo.class.getName() +
    			" NG,CL -attr showChildren=true,desc=true -out /temp/out.xml");
        System.exit(1);
	}
	
    /**
     * Send HTTP get request to server and returns XNL String response
     * @param relation
     * @param withChildren
     * @return
     */
    String sendRequestAndGetServerResponse(String relation, String attributes)
    {
    	ClientResponse resp = null;
    	MultivaluedMap<String, String> Params = new MultivaluedMapImpl();

    	//setup query parameters
    	if (attributes != null)
    	{
    		String[] params = attributes.split(",");
    		for (int i = 0; i < params.length; i++)
    		{
    			String[] param = params[i].split("=");
    			Params.add(param[0], param[1]);
    		}
    	}

    	resp = webRsc.path(METADATA_REQ_RSC_PATH).path(relation).queryParams(Params).
    			accept(MediaType.TEXT_XML).get(ClientResponse.class);

        if (HTTP_CODE_OK != resp.getStatus()) {
            throw new IllegalStateException("Error while polling for results [" + resp + "]");
        }

        return resp.getEntity(String.class);        
	}

    /**
     * Parses Meta Data Relation XML response and display on console
     * @param response XML formated response
     */
	@SuppressWarnings("unchecked")
    private static void parseMetadataXMLResponseAndDisplayOnConsole(String response) throws UnsupportedEncodingException, XMLStreamException
    {
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
    			//If this is a RelInfo element print its attributes
    			if (startElement.getName().toString().equals("RelInfo")) {
					Iterator<Attribute> attributes = startElement.getAttributes();
    				System.out.println("=======================================================");
    				while (attributes.hasNext()) {
    					Attribute attribute = attributes.next();
    					System.out.println(attribute.getName().toString() + ": " + attribute.getValue());
    				}
    			}
    			//If this is a children element call a helper method to parse it.
    			else if (startElement.getName().toString().equals("children"))
    				parseRelInfoChildren(eventReader);
    		}
    	}
    }

    /**
     * helper method. Parses Meta Data Relation children
     * @param eventReader - XML formatted input resource.
     */
	@SuppressWarnings("unchecked")
    private static void parseRelInfoChildren(XMLEventReader eventReader) throws XMLStreamException
    {
    	System.out.println("Children: ");
    	while (eventReader.hasNext()) {
    		XMLEvent event = eventReader.nextEvent();
    		if (event.isStartElement()) {
    			StartElement startElement = event.asStartElement();
    			//If this is a RelInfo element print its attributes
    			if (startElement.getName().getLocalPart().equals("RelInfo")) {
					Iterator<Attribute> attributes = startElement.getAttributes();
    				while (attributes.hasNext()) {
    					Attribute attribute = attributes.next();
    					System.out.print("\t" + attribute.getName().toString() + ": " + attribute.getValue());
    				}
    				System.out.println();
    			}
    		}
    		else if (event.isEndElement()) {
    			EndElement endElement = event.asEndElement();
    			if (endElement.getName().getLocalPart().equals("children"))
    				break;
    		}
    	}
    }
    
    
    /**
     * prints string XML response to given file
     * @param fileName user specified output file
     * @param response server XML response
     * @return
     */
	private static void printXMLResponseToFile(String fileName, String response)
	{
		if (!fileName.contains(".xml") && !fileName.contains(".XML"))
			fileName = fileName.concat(".xml");
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
	
	/**
	 * @param args
	 * @throws XMLStreamException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException, XMLStreamException {
        if (args.length < 1) {
        	usage();
        }

        //user input arguments
	    String relations;
	    String attributes = null;
	    String fileName = null;
        
	    relations = args[0];
	    for (int i = 1; i < args.length; i++) {
	    	if (args[i].equals("-attr")) {
	    		i++;
	    		attributes = args[i];
	    	}
	    	else if (args[i].equals("-out")) {
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
	    
		GetRelationInfo c = new GetRelationInfo();
	    String response = null;

	    response = c.sendRequestAndGetServerResponse(relations, attributes);
	    
	    if (fileName != null)
	    	//XML output file specified.
	    	printXMLResponseToFile(fileName, response);
	    else
	    	//Parse and display on console
		    parseMetadataXMLResponseAndDisplayOnConsole(response);
	}
}
