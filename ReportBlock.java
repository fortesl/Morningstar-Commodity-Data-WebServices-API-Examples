/**
 * 
 */
package webclient.examples;

/**
 * @author lfortes
 *
 */
public class ReportBlock {
	private int numCols;
	private int numRows;
	private String[] RowDates;
	private String[] ColumnHeadings;
	private double[] Values;
	private int lastDateIdx, lastValueIdx, lastColumnHeadingIdx;
	
	public ReportBlock(int cols, int rows)
	{
		numCols = cols;
		numRows = rows;
		
		RowDates = new String[numRows];
		Values = new double[numRows * numCols]; 
		ColumnHeadings = new String[numCols];
		
		lastDateIdx = lastValueIdx = lastColumnHeadingIdx = 0;
	}
	
	public void addValue(double v)
	{
			Values[lastValueIdx++] = v;
	}
	
	public void addRowDate(String date)
	{
		RowDates[lastDateIdx++] = date;
	}

	public void addColumnHeading(String heading)
	{
		ColumnHeadings[lastColumnHeadingIdx++] = heading;
	}
	
	public String[] getRowDates()
	{
		return RowDates;
	}
	
	public double[] getValues()
	{
		return Values;
	}
	
	public String[] getColumnHeadings()
	{
		return ColumnHeadings;
	}

	public int getNumRows()
	{
		return numRows;
	}

	public int getNumCols()
	{
		return numCols;
	}
	
}
