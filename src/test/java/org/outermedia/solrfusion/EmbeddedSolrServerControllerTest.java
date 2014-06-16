package org.outermedia.solrfusion;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.outermedia.solrfusion.adapter.SearchServerAdapterIfc;
import org.outermedia.solrfusion.adapter.solr.EmbeddedSolrAdapter;
import org.outermedia.solrfusion.configuration.Configuration;
import org.outermedia.solrfusion.configuration.ResponseRendererType;
import org.outermedia.solrfusion.configuration.SearchServerConfig;
import org.outermedia.solrfusion.mapper.ResponseMapperIfc;
import org.outermedia.solrfusion.response.ResponseRendererIfc;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Created by ballmann on 6/6/14.
 */
@SuppressWarnings("unchecked")
public class EmbeddedSolrServerControllerTest extends SolrServerDualTestBase
{
    protected TestHelper helper;

    @Mock
    ResponseRendererIfc testRenderer;

    ByteArrayInputStream testResponse;

    @Mock
    SearchServerAdapterIfc testAdapter;

    EmbeddedSolrAdapter testAdapter9000;

    EmbeddedSolrAdapter testAdapter9002;

    Configuration cfg;

    @Mock
    private SearchServerConfig testSearchConfig;

    @Before
    public void fillSolr() throws IOException, SolrServerException {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", String.valueOf(1));
        document.addField("title", String.valueOf("abc"));
        document.addField("author", String.valueOf("Shakespeare"));
        firstServer.add(document);
        firstTestServer.commitLastDocs();

        document = new SolrInputDocument();
        document.addField("id", String.valueOf(1));
        document.addField("titleVT_eng", String.valueOf("abc"));
        document.addField("author", String.valueOf("Shakespeare"));
        secondTestServer.getServer().add(document);
        secondTestServer.commitLastDocs();
    }

    @After
    public void cleanSolr() throws IOException, SolrServerException {
        firstServer.deleteByQuery("*:*");
    }

    @Before
    public void setup() throws IOException, ParserConfigurationException, JAXBException, SAXException
    {
        helper = new TestHelper();
        MockitoAnnotations.initMocks(this);
        cfg = null;
    }

    @Test
    public void testQueryWithMultipleServersAndResponseDocuments()
            throws IOException, ParserConfigurationException, SAXException, JAXBException,
            InvocationTargetException, IllegalAccessException, URISyntaxException
    {
        testMultipleServers();
        verify(testAdapter9000,times(1)).sendQuery("title:abc");
        verify(testAdapter9002,times(1)).sendQuery("titleVT_eng:abc");
    }

    @Test
    public void testQueryWithMultipleServersButNoResponseDocuments()
            throws IOException, ParserConfigurationException, SAXException, JAXBException,
            InvocationTargetException, IllegalAccessException, URISyntaxException
    {
        String xml = testMultipleServers();

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "<lst name=\"responseHeader\">\n" +
                "  <int name=\"status\">0</int>\n" +
                "  <int name=\"QTime\">0</int>\n" +
                "  <lst name=\"params\">\n" +
                "    <str name=\"indent\">on</str>\n" +
                "    <str name=\"start\">0</str>\n" +
                "    <str name=\"q\"><![CDATA[title:abc]]></str>\n" +
                "    <str name=\"version\">2.2</str>\n" +
                "    <str name=\"rows\">2</str>\n" +
                "  </lst>\n" +
                "</lst>\n" +
                "<result name=\"response\" numFound=\"2\" start=\"0\">\n" +
                "  <doc>\n" +
                "    <str name=\"id\"><![CDATA[Bibliothek 9000#1]]></str>\n" +
                "    <arr name=\"title\">\n" +
                "      <str><![CDATA[abc]]></str>\n" +
                "    </arr>\n" +
                "  </doc>\n" +
                "  <doc>\n" +
                "    <str name=\"id\"><![CDATA[Bibliothek 9002#1]]></str>\n" +
                "    <str name=\"title\"><![CDATA[abc]]></str>\n" +
                "  </doc>\n" +
                "</result>\n" +
                "</response>";

        Assert.assertEquals("Found different xml response", expected, xml.trim());
        verify(testAdapter9000,times(1)).sendQuery("title:abc");
        verify(testAdapter9002,times(1)).sendQuery("titleVT_eng:abc");
    }

    protected String testMultipleServers()
            throws IOException, ParserConfigurationException, SAXException, JAXBException,
            InvocationTargetException, IllegalAccessException, URISyntaxException
    {
        cfg = helper
                .readFusionSchemaWithoutValidation("test-fusion-schema-embedder-solr-adapter.xml");
        ResponseMapperIfc testResponseMapper = cfg.getResponseMapper();
        // the mapping is very incomplete, so ignore all unmapped fields
        testResponseMapper.ignoreMissingMappings();
        Configuration spyCfg = spy(cfg);
        when(spyCfg.getResponseMapper()).thenReturn(testResponseMapper);

        List<SearchServerConfig> searchServerConfigs = spyCfg.getSearchServerConfigs().getSearchServerConfigs();
        SearchServerConfig searchServerConfig9000 = spy(searchServerConfigs.get(0));
        SearchServerConfig searchServerConfig9002 = spy(searchServerConfigs.get(1));
        searchServerConfigs.clear();

        searchServerConfigs.add(searchServerConfig9000);
        testAdapter9000 = (EmbeddedSolrAdapter) spy(searchServerConfig9000.getInstance());
        when(searchServerConfig9000.getInstance()).thenReturn(testAdapter9000);
        testAdapter9000.setTestServer(firstTestServer);

        searchServerConfigs.add(searchServerConfig9002);
        testAdapter9002 = (EmbeddedSolrAdapter) spy(searchServerConfig9002.getInstance());
        when(searchServerConfig9002.getInstance()).thenReturn(testAdapter9002);
        testAdapter9002.setTestServer(secondTestServer);

        FusionController fc = new FusionController(spyCfg);
        FusionRequest fusionRequest = new FusionRequest();
        fusionRequest.setQuery("title:abc");
        fusionRequest.setResponseType(ResponseRendererType.XML);
        FusionResponse fusionResponse = new FusionResponse();
        fc.process(fusionRequest, fusionResponse);
        Assert.assertTrue("Expected no processing error", fusionResponse.isOk());

        String result = fusionResponse.getResponseAsString();
        Assert.assertNotNull("Expected XML result, but got nothing", result);
        // System.out.println("RESPONSE " + result);
        return result;
    }

}