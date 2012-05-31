/*
 * <response status="Job is initalizing." jobID="2006" intStatus="202"/>
 */
using System;

namespace DataRequestExample
{
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Xml", "2.0.50727.3082")]
    [System.Xml.Serialization.XmlRootAttribute("response", Namespace = "", IsNullable = false)]
    public partial class JobResponse
    {

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public string status { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public int intStatus { get; set; }

        [System.Xml.Serialization.XmlAttributeAttribute()]
        public long jobId { get; set; }
    }

}
