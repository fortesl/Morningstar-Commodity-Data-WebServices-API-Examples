using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;
using System.Xml;
using System.Xml.Serialization;

namespace DataRequestExample
{
	class Program
	{
		static string webservice = null;
		static string rels = "NG, HO, HU, CL, PN, NYM.COAL";
		static string cols = "Open, High, Low, Close, Volume";
		static string startDate = "1980-01-03";
		static string endDate = "2015-05-01";
		static string creds = null;
		static DateTime sDate = DateTime.Parse(startDate).AddDays(-1);
		static DateTime eDate = DateTime.Parse(endDate).AddDays(1);
		static string userName;
		
		public static void Main(string[] args)
		{
			while (true) {
				webservice = "https://ws.lim.com";			

				Console.WriteLine("\n/*******************************************************/");
				Console.WriteLine("/*  Morningstar Commodity Data Webservice C# Examples  */");
				Console.WriteLine("/*                                                     */");
				Console.WriteLine("/*          1: Get Schema Example                      */");
				Console.WriteLine("/*          2: Get Records Example                     */");
				Console.WriteLine("/*          3: Query Execute Example                   */");
				Console.WriteLine("/*          4: Load Data Example                       */");
				Console.WriteLine("/*          Q: Quit                                    */");
				Console.WriteLine("/*                                                     */");
				Console.WriteLine("/*******************************************************/");
				Console.WriteLine("\nEnter selection: ");
				
				ConsoleKeyInfo keyInfo = Console.ReadKey(true);
				char ch = keyInfo.KeyChar;
				switch(ch)
				{
					case '1':
						Console.WriteLine("\n=== Running Get Schema Example ===");
						DoGetSchemaExample();
						break;
					case '2':
						Console.WriteLine("\n=== Running Get Records Example ===");
						Console.WriteLine("Rels: {0}", rels);
						Console.WriteLine("Cols: {0}", cols);
						DoDataRequest(BuildGetRecordsRequest());
						break;
					case '3':
						Console.WriteLine("\n=== Running Exectute Query Example ===");
						DoDataRequest(BuildQueryRequest());
						break;
					case '4':
						webservice = "http://localhost:8080";
						creds = null;
						Console.WriteLine("\n=== Running Load Data Example ===");
						DoDataLoadExample("C:\\temp\\upload_sample_data.xml");
						creds = null;
						break;
					case 'Q':
					case 'q':
						Environment.Exit(0);
						break;
				}
			}

		}
		
		private static WebRequest BuildWebRequest(String uriPath, String method, byte [] postData)
		{
			WebRequest req = WebRequest.Create(webservice + uriPath);
			req.Method = method;
			req.ContentType = "text/xml";

			// The service is a RESTful service that is stateless and most resources require authenticaiton via HTTP Basic Authenticaiton.
			// Here we set the Authoricaiton header.
			creds = GetCreds(webservice);

			
			req.Headers["Authorization"] = "Basic " + Convert.ToBase64String(Encoding.Default.GetBytes(creds));;

			// Since we have the credentials let's pre-authenticate.
			req.PreAuthenticate = true;
			
			if (postData != null) {
				// HTTP Connection is established.
				Stream stream = req.GetRequestStream();
				
				stream.Write(postData, 0, postData.Length);
				stream.Close();

			}
			
			return req;
		}
		
		/*
		 * Get Schema Example.
		 * Performs HTTP request to get schema information for a specified relation.
		 */
		private static void DoGetSchemaExample()
		{
			Console.Write("Enter a relation: ");
			String relation = Console.ReadLine();
			
			string path = "/rs/api/schema/relations/" + relation +
				"?showChildren=true" +
				"&desc=true" +
				"&cDesc=true" +
				"&showColumns=true" +
				"&dateRange=false" +
				"&dataPreview=false";
			
			WebRequest req = BuildWebRequest(path, "GET", null);
			
			string s = ExecuteHttpRequest<string>(req);
			
			if (s != null) 
				Console.WriteLine(FormatXmlString(s));
			
		}
		
		private static string BuildGetRecordsRequest()
		{
			StringBuilder postData = new StringBuilder();
			postData.Append("<DataRequest> ");
			postData.Append("<GetRecsParams> ");
			postData.Append(String.Format("<Rels>{0}</Rels><Cols>{1}</Cols>", rels, cols));
			postData.Append("<nu>1</nu> ");
			postData.Append("<Unit>DAYS</Unit> ");
			postData.Append(String.Format("<fd>{0}</fd><td>{1}</td>", startDate, endDate));
			postData.Append("<mdf>FILL_NAN</mdf>");
			postData.Append("<mhf>FILL_NAN</mhf>");
			postData.Append("<sn>SKIP_NONE</sn>");
			postData.Append("<LimitMode>BY_RECORDS</LimitMode>");
			postData.Append("<LimitSize>100000</LimitSize>");
			postData.Append("</GetRecsParams>");
			postData.Append("</DataRequest>");
			
			return postData.ToString();
			
		}
		
		private static string BuildQueryRequest()
		{
			StringBuilder buff = new StringBuilder("<DataRequest><Query><Text>SHOW\n");
			
			int i = 1;
			foreach(String rel in rels.Split(','))
			{
				foreach(String col in cols.Split(','))
				{
					buff.Append(String.Format(" {0}: {1} of {2}\n", i++, col, rel));
				}
			}
			
			buff.Append(String.Format("WHEN Date is after {0} AND Date is before {1}</Text></Query></DataRequest>", sDate.ToString("MM/dd/yyyy"), eDate.ToString("dd/MM/yyyy")));
			
			return buff.ToString();
		}
		
		private static void DoDataRequest(string dataRequestXml)
		{
			
			byte[] bytes = System.Text.Encoding.UTF8.GetBytes(dataRequestXml);

			WebRequest req = BuildWebRequest("/rs/api/datarequests", "POST", bytes);
			DataRequestResponse drr = ExecuteHttpRequest<DataRequestResponse>(req);
			
			while (drr.status == Status.INCOMPLETE)
			{
				req = BuildWebRequest("/rs/api/datarequests/" + drr.id, "GET", null);
				drr = ExecuteHttpRequest<DataRequestResponse>(req);
			}
			ProcessDataRequestResponse(drr);
		}
		
		
		private static void DoDataLoadExample(string fileName) 
		{
			
			string data = System.IO.File.ReadAllText(fileName);
			
			// POST the data.
			byte[] bytes = System.Text.Encoding.UTF8.GetBytes(data);

			WebRequest req = BuildWebRequest("/rs/upload/?username="+ userName + "&parsername=DefaultParser", "POST", bytes);
			string result = ExecuteHttpRequest<string>(req);
			
			if (result != null) {
				// <response status="Job is initalizing." jobID="2006" intStatus="202"/>
				XmlDocument doc = new XmlDocument();
				doc.LoadXml(result);
				XmlNode root = doc.DocumentElement;
				
				long jobId = long.Parse(root.Attributes.GetNamedItem("jobID").Value);
				int status = int.Parse(root.Attributes.GetNamedItem("intStatus").Value);
				Console.WriteLine("JobID: {0}\nintStatus: {1}", jobId, status);
				
				req = BuildWebRequest("/rs/upload/jobreport/" + jobId, "GET", null);
				
				root = null;
				int ct = 0;
				// Check the status.
				// Loop to GET the status of the job until completed.
				/*
				 <JobResultImpl>
					<Messages><msgs type="INFO"><text>Loaded 3 values for 3 relcols.</text></msgs></Messages>
					<status><code>300</code><message>Job completed successfully.</message></status>
				 </JobResultImpl>
				 */
				while (status < 300 && ct < 20) {
					Thread.Sleep(500);
					result = ExecuteHttpRequest<string>(req);
					if (result == null) 
						break;
					if (result.Trim().Length > 0) {
						doc.LoadXml(result);
						root = doc.DocumentElement;
						status = int.Parse(root.SelectSingleNode("//status/code").InnerText);
						Console.WriteLine("JobID: {0}\nintStatus: {1}", jobId, status);				
					}
					ct++;
				}
				
				if (root != null) 
				{
					Console.WriteLine("Status: {0}", root.SelectSingleNode("//status/message").InnerText);
					XmlNode msgNode = root.SelectSingleNode("//Messages/msgs");
					if (msgNode != null) 
					{
						Console.WriteLine("Msg Type: {0}", msgNode.Attributes.GetNamedItem("type").Value);
						Console.WriteLine("Msg: {0}", msgNode.InnerText);
					}
				}
			}
		}
		
		private static void ProcessDataRequestResponse(DataRequestResponse drr)
		{
			Console.WriteLine("DataRequest Status: {0} {1}", drr.status, drr.statusMsg);
			List<Report> reports = drr.reports;
			foreach (Report report in reports)
			{
				Console.WriteLine();
				List<ReportBlock> blocks = report.reportBlocks;
				foreach (ReportBlock block in blocks)
				{
					
					List<System.DateTime> dates = block.rowDates;

					// d1r1c1, d1r1c2, d1r2c1, d1r2c2, d2r1c1, d2r1c2, d2r2c1, d2r2c2
					List<Double> values = block.values;
					
					int numRels = block.numRows/dates.Count;
					int numDates = dates.Count;
					int numCols = block.numCols;
					int numRows = block.numRows;

					for(int ii=0; ii<values.Count; ii++)
					{
						if (ii % (numRels*numCols) == 0) {
							Console.Write("\n{0}\t", dates[ii/(numRels*numCols)].ToString("yyyy.MM.dd"));
						}

						Console.Write("{0}\t", values[ii]);
					}

					Console.WriteLine("\nRows: {0}", numRows);
					Console.WriteLine("Rels: {0}", numRels);
					Console.WriteLine("Cols: {0}", numCols);
					Console.WriteLine("Dates: {0}",  dates.Count);
					Console.WriteLine("Vals: {0}",  values.Count);
					
				}
			}
			

		}

		private static T ExecuteHttpRequest<T> (WebRequest req)
		{
			T r = default(T);
			
			Console.WriteLine(req.Headers["Authorization"]);
			
			try {
				Console.WriteLine("{0} {1}", req.Method, req.RequestUri.ToString());
				
				// HTTP request is made.
				WebResponse response = req.GetResponse();
				
				StreamReader streamReader = new StreamReader(response.GetResponseStream());
				
				if (typeof(T) == typeof(string))
				{
					r = (T)Convert.ChangeType(streamReader.ReadToEnd(), typeof(T));
				} else 
				{
					XmlSerializer serializer = new XmlSerializer(typeof(T));
				
					// All data is read off the stream and deserialized into an object of type T.
					r = (T) serializer.Deserialize(streamReader);
				}
				
			} catch (WebException e) {
				Console.WriteLine(e.ToString());
				WebResponse response = e.Response;
				if (response != null) 
				{
					HttpWebResponse httpResponse = (HttpWebResponse)response;
					Console.WriteLine("Error: {0}", httpResponse.StatusCode);
					using (var streamReader = new StreamReader(response.GetResponseStream()))
						Console.WriteLine(streamReader.ReadToEnd());
				}
			}
			
			return r;

		}

		/******************************
		 * 
		 * HELPER METHODS
		 * 
		 ******************************/
		private static string FormatXmlString(string xmlString)
		{
			System.Xml.Linq.XElement element = System.Xml.Linq.XElement.Parse(xmlString);
			return element.ToString();
		}
		
		private static string PromptForCreds(string service) {
			if (creds == null) 
			{
			Console.Write("Please enter your username for [{0}]: ", service);
			string u = Console.ReadLine();
			Console.Write("Please enter password for [{0}@{1}]: ", u, service);
			string p = ReadPassword();
			creds = String.Format("{0}:{1}", u, p);
			}
			
			return creds;
		}
		
		public static string ReadPassword() {
			Stack<string> pass = new Stack<string>();
			
			for (ConsoleKeyInfo consKeyInfo = Console.ReadKey(true);
			     consKeyInfo.Key != ConsoleKey.Enter; consKeyInfo = Console.ReadKey(true))
			{
				if (consKeyInfo.Key == ConsoleKey.Backspace)
				{
					try
					{
						Console.SetCursorPosition(Console.CursorLeft - 1, Console.CursorTop);
						Console.Write(" ");
						Console.SetCursorPosition(Console.CursorLeft - 1, Console.CursorTop);
						pass.Pop();
					}
					catch (InvalidOperationException ex)
					{
						/* Nothing to delete, go back to previous position */
						Console.SetCursorPosition(Console.CursorLeft + 1, Console.CursorTop);
					}
				}
				else {
					Console.Write("*");
					pass.Push(consKeyInfo.KeyChar.ToString());
				}
			}
			String[] password = pass.ToArray();
			Array.Reverse(password);
			Console.WriteLine();
			return string.Join(string.Empty, password);
		}

		public static string GetCreds(string service)
		{
//			String s = "jDoe@lim.com:-secret-";
			String s = PromptForCreds(service);
			userName = s.Split(':')[0];
			
			return s;
		}
		
	}
	
}