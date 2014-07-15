package org.outermedia.solrfusion.adapter.solr;

import junit.framework.Assert;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.outermedia.solrfusion.query.SolrFusionRequestParams;
import org.outermedia.solrfusion.TestHelper;
import org.outermedia.solrfusion.adapter.SearchServerAdapterIfc;
import org.outermedia.solrfusion.adapter.SearchServerResponseException;
import org.outermedia.solrfusion.configuration.Configuration;
import org.outermedia.solrfusion.configuration.SearchServerConfig;
import org.outermedia.solrfusion.configuration.Util;
import org.outermedia.solrfusion.response.parser.XmlResponse;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.mockito.Mockito.*;
import static org.outermedia.solrfusion.query.SolrFusionRequestParams.*;

/**
 * Created by stephan on 11.06.14.
 */
public class DefaultSolrAdapterTest
{
    protected TestHelper helper;

    @Before
    public void setup()
    {
        helper = new TestHelper();
    }

    @Test @Ignore
    public void testHttpClientGet() throws IOException, URISyntaxException
    {
        HttpClient client = HttpClientBuilder.create().build();
        URI uri = new URI("http", null, "www.outermedia.de", 80, "/", "q=bla", null);
        HttpGet request = new HttpGet(uri);
        HttpResponse response = client.execute(request);

        Header[] header = response.getHeaders("Server");
        Assert.assertTrue("Server is Ubuntu", header[0].getValue().contains("Ubuntu"));

        // Get the response
        HttpEntity entity = response.getEntity();
        Assert.assertEquals("Content-type is utf8 hmtl", "text/html; charset=utf-8",
            entity.getContentType().getValue());

        String content = new Scanner(response.getEntity().getContent(), "UTF-8").useDelimiter("\\A").next();
        System.out.println(content);

    }

    @Test
    public void testHttpClientUriTools() throws IOException, URISyntaxException
    {
        String url = "http://141.39.229.20:9002/te/select?q=23";
        URIBuilder ub = new URIBuilder(url);

        Assert.assertEquals("", "[q=23]", ub.getQueryParams().toString());

        ub.setParameter("q", "*:*");

        Assert.assertEquals("", "[q=*:*]", ub.getQueryParams().toString());
    }

    @Test @Ignore
    public void testDefaultSolrAdapter()
        throws FileNotFoundException, ParserConfigurationException, SAXException, JAXBException
    {

        Configuration cfg = helper.readFusionSchemaWithoutValidation("test-solr-adapter-fusion-schema.xml");
        List<SearchServerConfig> configuredSearchServers = cfg.getConfigurationOfSearchServers();

        for (SearchServerConfig searchServerConfig : configuredSearchServers)
        {
            try
            {
                SearchServerAdapterIfc adapter = searchServerConfig.getInstance();
                Map<String, String> params = new HashMap<>();
                String qParam = SolrFusionRequestParams.QUERY.getRequestParamName();
                params.put(qParam, "shakespeare");
                InputStream is = adapter.sendQuery(params, 4000);

                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                XmlResponse xmlResponse = (new Util()).unmarshal(XmlResponse.class, "", br, null);

                Assert.assertTrue("Expected a shakespeare in a library", xmlResponse.getDocuments().size() != 0);
                Assert.assertEquals("Expected ten shakespeares on first page", 10, xmlResponse.getDocuments().size());

            }
            catch (Exception e)
            {
                System.out.println("Caught exception while communicating with server " + e.toString());
            }
        }
    }

    @Test
    public void testErrorCase() throws URISyntaxException, IOException
    {
        DefaultSolrAdapter adapter = spy(DefaultSolrAdapter.Factory.getInstance());
        SearchServerConfig sc = new SearchServerConfig();
        sc.setUrl("http://localhost");
        adapter.init(sc);
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        HttpGet request = mock(HttpGet.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine sl = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(adapter.newHttpClient()).thenReturn(client);
        doReturn(request).when(adapter).newHttpGet(any(URIBuilder.class));
        when(client.execute(request)).thenReturn(response);
        when(response.getStatusLine()).thenReturn(sl);
        when(sl.getStatusCode()).thenReturn(400);
        when(sl.getReasonPhrase()).thenReturn("Bad Query");
        when(response.getEntity()).thenReturn(entity);

        // without response
        when(entity.getContent()).thenReturn(null);
        Map<String, String> params = new HashMap<>();
        params.put(QUERY.getRequestParamName(), "*:*");
        params.put(SORT.getRequestParamName(), "score desc");
        try
        {
            adapter.sendQuery(params, 3000);
            Assert.fail("Expected SearchServerResponseException for http status 400");
        }
        catch (SearchServerResponseException se)
        {
            String msg = se.getMessage();
            Assert.assertEquals("Expected other error message", "ERROR 400: Bad Query", msg);
        }

        // with response
        when(entity.getContent()).thenReturn(new StringBufferInputStream("Bad Content"));
        try
        {
            adapter.sendQuery(params, 3000);
            Assert.fail("Expected SearchServerResponseException for http status 400");
        }
        catch (SearchServerResponseException se)
        {
            String msg = se.getMessage();
            Assert.assertEquals("Expected other error message", "ERROR 400: Bad Query", msg);
            Assert.assertNotNull("Response should be set", se.getHttpContent());
        }
    }

    @Test
    public void testHttpClientParamBuilding()
        throws URISyntaxException, FileNotFoundException, ParserConfigurationException, SAXException, JAXBException
    {
        DefaultSolrAdapter adapter = DefaultSolrAdapter.Factory.getInstance();
        SearchServerConfig serverConfig = mock(SearchServerConfig.class);
        doReturn("http://unit.test.com/").when(serverConfig).getUrl();
        adapter.init(serverConfig);
        Map<String, String> params = new HashMap<>();

        params.put(QUERY.getRequestParamName(), "*:*");
        params.put(FILTER_QUERY.getRequestParamName(), "title:a");
        params.put(WRITER_TYPE.getRequestParamName(), "json");
        params.put(START.getRequestParamName(), "5");
        params.put(PAGE_SIZE.getRequestParamName(), "12");
        params.put(SORT.getRequestParamName(), "title asc");
        params.put(FIELDS_TO_RETURN.getRequestParamName(), "*,score, title");

        String ub = adapter.buildHttpClientParams(params).build().toString();
        System.out.println(ub);
        Assert.assertEquals("Expected other solr query url",
            "http://unit.test.com/?q=*%3A*&fq=title%3Aa&wt=json&start=5&rows=12&sort=title+asc&fl=*%2Cscore%2C+title", ub);
    }

    @Test
    public void testMergeField()
    {
        DefaultSolrAdapter adapter = DefaultSolrAdapter.Factory.getInstance();
        String s = adapter.mergeField("score", "");
        Assert.assertEquals("Expected other merge result", "score", s);

        s = adapter.mergeField("score", "id");
        Assert.assertEquals("Expected other merge result", "id score", s);

        s = adapter.mergeField("score", "score");
        Assert.assertEquals("Expected other merge result", "score", s);

        s = adapter.mergeField("score", "id score");
        Assert.assertEquals("Expected other merge result", "id score", s);

        s = adapter.mergeField("score", "score id");
        Assert.assertEquals("Expected other merge result", "score id", s);

        s = adapter.mergeField("score", "title score id");
        Assert.assertEquals("Expected other merge result", "title score id", s);
    }
}