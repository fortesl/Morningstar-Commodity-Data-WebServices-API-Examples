/**
 * 
 */
package webclient.examples;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lfortes
 *
 */
public class Report {
	private String title;
	private List<ReportBlock> ReportBlocks;
		
	/**
	 * @param value
	 */
	public Report(String _title) {
		title = _title;
		ReportBlocks = new ArrayList<ReportBlock>();
	}

	public String getTitle()
	{
		return title;
	}
	
	public List<ReportBlock> getReportBlocks()
	{
		return ReportBlocks;
	}
	
	public void addReportBlock(ReportBlock rb)
	{
		ReportBlocks.add(rb);
	}
}
