package org.outermedia.solrfusion.types;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.outermedia.solrfusion.configuration.Configuration;
import org.outermedia.solrfusion.configuration.Util;
import org.outermedia.solrfusion.mapper.ResponseMapper;
import org.outermedia.solrfusion.mapper.Term;
import org.outermedia.solrfusion.response.parser.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by ballmann on 6/19/14.
 */
public class BshFileTest extends AbstractTypeTest
{
    @Test
    public void testConfigParsing() throws IOException, SAXException, ParserConfigurationException
    {
        String xml = docOpen + "<file>target/test-classes/test-bsh-file.bsh</file>" + docClose;

        Util util = new Util();
        Element elem = util.parseXml(xml);
        BshFile bshType = Mockito.spy(new BshFile());
        bshType.passArguments(util.filterElements(elem.getChildNodes()), util);
        Mockito.verify(bshType, Mockito.times(1)).logBadConfiguration(Mockito.eq(true), Mockito.anyList());

        String code = bshType.getCode();
        String expected = FileUtils.readFileToString(new File("target/test-classes/test-bsh-file.bsh"));
        Assert.assertEquals("Parsing of configuration failed.", expected, code);
    }

    @Test
    public void testMissingConfig() throws IOException, SAXException, ParserConfigurationException
    {
        String xml = docOpen + docClose;

        Util util = new Util();
        Element elem = util.parseXml(xml);
        BshFile bshType = Mockito.spy(new BshFile());
        bshType.passArguments(util.filterElements(elem.getChildNodes()), util);
        Mockito.verify(bshType, Mockito.times(1)).logBadConfiguration(Mockito.eq(false), Mockito.anyList());

        String code = bshType.getCode();
        Assert.assertNull("Handling of missing configuration failed.", code);
    }

    @Test
    public void testMapping() throws FileNotFoundException, ParserConfigurationException, SAXException, JAXBException
    {
        Configuration cfg = helper.readFusionSchemaWithoutValidation("test-script-types-fusion-schema.xml");
        ResponseMapper rm = ResponseMapper.Factory.getInstance();
        Document doc = buildResponseDocument();

        buildResponseField(doc, "Titel", "Ein kurzer Weg");
        buildResponseField(doc, "Autor", "Willi Schiller");
        buildResponseField(doc, "id", "132");
        Term sourceField = buildResponseField(doc, "f2", "something2");

        ScriptEnv env = new ScriptEnv();
        rm.mapResponse(cfg, cfg.getSearchServerConfigs().getSearchServerConfigs().get(0), doc, env);
        // System.out.println(sourceField.toString());
        org.junit.Assert.assertEquals("Found wrong field name mapping", "today2", sourceField.getFusionFieldName());
        org.junit.Assert.assertEquals("Found wrong field value mapping", Arrays.asList("something2 at 2014-07-19"),
                sourceField.getFusionFieldValue());
    }
}
