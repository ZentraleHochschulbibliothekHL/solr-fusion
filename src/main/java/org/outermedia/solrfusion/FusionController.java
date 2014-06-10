package org.outermedia.solrfusion;

import lombok.extern.slf4j.Slf4j;
import org.outermedia.solrfusion.adapter.SearchServerAdapterIfc;
import org.outermedia.solrfusion.configuration.Configuration;
import org.outermedia.solrfusion.configuration.SearchServerConfig;
import org.outermedia.solrfusion.mapper.QueryMapper;
import org.outermedia.solrfusion.mapper.ResetQueryState;
import org.outermedia.solrfusion.mapper.SearchServerQueryBuilder;
import org.outermedia.solrfusion.query.QueryParserIfc;
import org.outermedia.solrfusion.query.parser.Query;
import org.outermedia.solrfusion.response.ClosableIterator;
import org.outermedia.solrfusion.response.ResponseRendererIfc;
import org.outermedia.solrfusion.response.parser.Document;
import org.outermedia.solrfusion.types.ScriptEnv;

import java.util.List;
import java.util.Map;

/**
 * Created by ballmann on 04.06.14.
 */
@Slf4j
public class FusionController
{
    private Configuration configuration;
    private ResetQueryState queryResetter;
    private QueryMapper queryMapper;

    public FusionController(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public void process(FusionRequest fusionRequest, FusionResponse fusionResponse)
    {
        queryResetter = getNewResetQueryState();
        queryMapper = getNewQueryMapper();

        Query query = parseQuery(fusionRequest);
        if (query == null)
        {
            fusionResponse.setResponseForQueryParseError();
        }
        else
        {
            List<SearchServerConfig> configuredSearchServers = configuration.getConfigurationOfSearchServers();
            if (configuredSearchServers == null || configuredSearchServers.isEmpty())
            {
                fusionResponse.setResponseForNoSearchServerConfiguredError();
            }
            else
            {
                ResponseConsolidatorIfc consolidator = configuration.getResponseConsolidator();
                requestAllSearchServers(query, configuredSearchServers, consolidator);
                if (consolidator.numberOfResponseStreams() < configuration.getDisasterLimit())
                {
                    fusionResponse.setResponseForTooLessServerAnsweredError(configuration.getDisasterMessage());
                }
                else
                {
                    ResponseRendererIfc responseRenderer = configuration.getResponseRendererByType(fusionRequest.getResponseType());
                    if (responseRenderer == null)
                    {
                        fusionResponse.setResponseForMissingResponseRendererError(fusionRequest.getResponseType());
                    }
                    else
                    {
                        ClosableIterator<Document> response = consolidator.getResponseStream();
                        fusionResponse.setOkResponse(responseRenderer.getResponseString(response));
                    }
                }
                consolidator.reset();
            }
        }
    }

    protected void requestAllSearchServers(Query query, List<SearchServerConfig> configuredSearchServers,
                                           ResponseConsolidatorIfc consolidator)
    {
        ScriptEnv env = getNewScriptEnv();
        for (SearchServerConfig searchServerConfig : configuredSearchServers)
        {
            if (mapQuery(query, env, searchServerConfig))
            {
                sendAndReceive(query, consolidator, searchServerConfig);
            }
        }
    }

    protected ScriptEnv getNewScriptEnv()
    {
        return new ScriptEnv();
    }

    protected QueryMapper getNewQueryMapper()
    {
        return new QueryMapper();
    }

    protected ResetQueryState getNewResetQueryState()
    {
        return new ResetQueryState();
    }

    protected void sendAndReceive(Query query, ResponseConsolidatorIfc consolidator, SearchServerConfig searchServerConfig)
    {
        SearchServerQueryBuilder queryBuilder = getNewSearchServerQueryBuilder();
        SearchServerAdapterIfc adapter = searchServerConfig.getImplementation();
        String searchServerQueryStr = queryBuilder.buildQueryString(query);
        try
        {
            ClosableIterator<Document> docIterator = adapter.sendQuery(searchServerQueryStr);
            if (docIterator != null)
            {
                consolidator.addResultStream(docIterator);
            }
        }
        catch (Exception e)
        {
            log.error("Caught exception while communicating with server {}", searchServerConfig.getSearchServerName(), e);
        }
    }

    protected SearchServerQueryBuilder getNewSearchServerQueryBuilder()
    {
        return new SearchServerQueryBuilder();
    }

    protected boolean mapQuery(Query query, ScriptEnv env, SearchServerConfig searchServerConfig)
    {
        boolean result = true;
        try
        {
            queryMapper.mapQuery(searchServerConfig, query, env);
        }
        catch (Exception e)
        {
            log.error("Caught exception while mapping fusion query to server {}", searchServerConfig.getSearchServerName(), e);
            result = false;
        }
        finally
        {
            queryResetter.reset(query);
        }

        return result;
    }

    protected Query parseQuery(FusionRequest fusionRequest)
    {
        Map<String, Float> boosts = fusionRequest.getBoosts();
        String query = fusionRequest.getQuery();
        QueryParserIfc queryParser = configuration.getQueryParser();
        Query queryObj = null;
        try
        {
            queryObj = queryParser.parse(configuration, boosts, query);
        }
        catch (Exception e)
        {
            log.error("Parsing of query {} failed.", query, e);
        }

        return queryObj;
    }
}
