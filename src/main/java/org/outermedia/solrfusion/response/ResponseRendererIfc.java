package org.outermedia.solrfusion.response;

import org.outermedia.solrfusion.FusionRequest;
import org.outermedia.solrfusion.FusionResponse;
import org.outermedia.solrfusion.adapter.SearchServerResponseInfo;
import org.outermedia.solrfusion.configuration.Configuration;
import org.outermedia.solrfusion.configuration.Initiable;
import org.outermedia.solrfusion.configuration.ResponseRendererFactory;
import org.outermedia.solrfusion.response.parser.Document;

/**
 * Transforms a Solr search result into a transport format.
 *
 * @author ballmann
 */

public interface ResponseRendererIfc extends Initiable<ResponseRendererFactory>
{
    /**
     *
     * @param configuration     the SolrFusion schema
     * @param docStream         the document stream to render
     * @param request           the current SolrFusion request
     * @param fusionResponse    the current SolrFusion response
     * @return an perhaps empty string
     */
    public String getResponseString(Configuration configuration,
        ClosableIterator<Document, SearchServerResponseInfo> docStream, FusionRequest request,
        FusionResponse fusionResponse);
}
