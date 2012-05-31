using System.Collections.Generic;
using System.Xml.Schema;
using System.Xml.Serialization;

namespace DataRequestExample
{
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Xml", "2.0.50727.3082")]
    [System.Xml.Serialization.XmlRootAttribute("DataRequestResponse", Namespace = "", IsNullable = false)]
    public partial class DataRequestResponse
    {

        private List<Report> _reports;

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public System.DateTime endTime { get; set; }

        [System.Xml.Serialization.XmlIgnoreAttribute()]
        public bool endTimeSpecified { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public long id { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public System.DateTime startTime { get; set; }

        [System.Xml.Serialization.XmlIgnoreAttribute()]
        public bool startTimeSpecified { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute("status")]
        public Status status { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public string statusMsg { get; set; }


        public DataRequestResponse()
        {
            _reports = new List<Report>();
        }

        [XmlElement("Reports", Form = XmlSchemaForm.Unqualified)]
        public List<Report> reports
        {
            get
            {
                return _reports;
            }
            set
            {
                _reports = value;
            }
        }

        //[XmlElementAttribute("Reports", Form = XmlSchemaForm.Unqualified)]
        [XmlIgnore]
        public Report report
        {
            get
            {
                if (_reports.Count > 0)
                {
                    return _reports[0];
                }
                return new Report();
            }
        }
    }
}

namespace DataRequestExample
{
    using System;
    using System.Diagnostics;
    using System.Xml.Serialization;
    using System.Collections;
    using System.ComponentModel;
    using System.Collections.Generic;


    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Xml", "2.0.50727.3082")]
    [XmlRootAttribute("Reports", Namespace = "", IsNullable = false)]
    public class Report
    {

        private List<ReportBlock> reportBlocksField;

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public string title { get; set; }


        public Report()
        {
            this.reportBlocksField = new List<ReportBlock>();
        }

        [System.Xml.Serialization.XmlElementAttribute("ReportBlocks", Form = System.Xml.Schema.XmlSchemaForm.Unqualified)]
        public List<ReportBlock> reportBlocks
        {
            get
            {
                return this.reportBlocksField;
            }
            set
            {
                this.reportBlocksField = value;
            }
        }
    }

    public partial class ReportBlock
    {

        private List<string> attributeHeadingsField;

        private List<int> columnHeadingIndicesField;

        private List<string> columnHeadingsField;

        private List<System.DateTime> rowDatesField;

        private List<System.DateTime> rowTimesField;

        private List<ReportSummaryBlock> summaryReportField;

        private List<double> valuesField;

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public int numAttributes { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public int numCols { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public int numRows { get; set; }


        public ReportBlock()
        {
            this.valuesField = new List<double>();
            this.summaryReportField = new List<ReportSummaryBlock>();
            this.rowTimesField = new List<System.DateTime>();
            this.rowDatesField = new List<System.DateTime>();
            this.columnHeadingsField = new List<string>();
            this.columnHeadingIndicesField = new List<int>();
            this.attributeHeadingsField = new List<string>();
        }

        [System.Xml.Serialization.XmlElementAttribute("AttributeHeadings", Form = System.Xml.Schema.XmlSchemaForm.Unqualified)]
        public List<string> attributeHeadings
        {
            get
            {
                return this.attributeHeadingsField;
            }
            set
            {
                this.attributeHeadingsField = value;
            }
        }

        [System.Xml.Serialization.XmlElementAttribute("ColumnHeadingIndices", Form = System.Xml.Schema.XmlSchemaForm.Unqualified)]
        public List<int> columnHeadingIndices
        {
            get
            {
                return this.columnHeadingIndicesField;
            }
            set
            {
                this.columnHeadingIndicesField = value;
            }
        }

        [System.Xml.Serialization.XmlElementAttribute("ColumnHeadings", Form = System.Xml.Schema.XmlSchemaForm.Unqualified)]
        public List<string> columnHeadings
        {
            get
            {
                return this.columnHeadingsField;
            }
            set
            {
                this.columnHeadingsField = value;
            }
        }

        [System.Xml.Serialization.XmlElementAttribute("RowDates", Form = System.Xml.Schema.XmlSchemaForm.Unqualified)]
        public List<System.DateTime> rowDates
        {
            get
            {
                return this.rowDatesField;
            }
            set
            {
                this.rowDatesField = value;
            }
        }

        [System.Xml.Serialization.XmlElementAttribute("RowTimes", Form = System.Xml.Schema.XmlSchemaForm.Unqualified)]
        public List<System.DateTime> rowTimes
        {
            get
            {
                return this.rowTimesField;
            }
            set
            {
                this.rowTimesField = value;
            }
        }

        [System.Xml.Serialization.XmlElementAttribute("SummaryReport", Form = System.Xml.Schema.XmlSchemaForm.Unqualified)]
        public List<ReportSummaryBlock> summaryReport
        {
            get
            {
                return this.summaryReportField;
            }
            set
            {
                this.summaryReportField = value;
            }
        }

        [System.Xml.Serialization.XmlElementAttribute("Values", Form = System.Xml.Schema.XmlSchemaForm.Unqualified)]
        public List<double> values
        {
            get
            {
                return this.valuesField;
            }
            set
            {
                this.valuesField = value;
            }
        }
    }

    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Xml", "2.0.50727.3082")]
    [System.Xml.Serialization.XmlRootAttribute("ReportSummaryBlock", Namespace = "", IsNullable = false)]
    public partial class ReportSummaryBlock
    {

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double avg { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double avgNegative { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double avgPositive { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double highest { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double lowest { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double pctNegative { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double pctPositive { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double stdDeviation { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double sum { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double variance { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public double zStat { get; set; }

    }

    public enum Status
    {
        [XmlEnum("100")]
        COMPLETE = 100,
        [XmlEnum("110")]
        QUERYERROR = 110,
        [XmlEnum("120")]
        DATAERROR = 120,
        [XmlEnum("140")]
        NOTENTITLED = 140,
        [XmlEnum("200")]
        INCOMPLETE = 200,
        [XmlEnum("300")]
        FAILED = 300,
    }
}

namespace DataRequestExample
{
    using System;
    using System.Diagnostics;
    using System.Xml.Serialization;
    using System.Collections;
    using System.ComponentModel;
    using System.Collections.Generic;

    // <response status="Job is initalizing." jobID="2006" intStatus="202"/>

    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Xml", "2.0.50727.3082")]
    [XmlRootAttribute("response", Namespace = "", IsNullable = false)]
    public class Response
    {

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public string status { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public long jobID { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public int intStatus { get; set; }
    }
}
