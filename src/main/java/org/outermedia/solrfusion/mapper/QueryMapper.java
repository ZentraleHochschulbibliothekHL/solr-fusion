package org.outermedia.solrfusion.mapper;

import lombok.extern.slf4j.Slf4j;
import org.outermedia.solrfusion.configuration.Configuration;
import org.outermedia.solrfusion.configuration.FieldMapping;
import org.outermedia.solrfusion.configuration.QueryMapperFactory;
import org.outermedia.solrfusion.configuration.SearchServerConfig;
import org.outermedia.solrfusion.query.QueryVisitor;
import org.outermedia.solrfusion.query.parser.*;
import org.outermedia.solrfusion.types.ScriptEnv;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Map a fusion query to a solr request.
 * <p/>
 * Created by ballmann on 03.06.14.
 */
@Slf4j
public class QueryMapper implements QueryVisitor, QueryMapperIfc
{
    private final static String NO_MAPPING_THROW_EXCEPTION = "error";
    private final static String NO_MAPPING_DELETE = "delete";


    private SearchServerConfig serverConfig;

    private String noMappingPolicy = NO_MAPPING_DELETE;

    /**
     * Only factory creates instances.
     */
    protected QueryMapper()
    {
    }

    /**
     * Map a query to a certain search server (serverConfig).
     *
     * @param serverConfig the currently used server's configuration
     * @param query        the query to map to process
     * @param env          the environment needed by the scripts which transform values
     */
    public void mapQuery(Configuration config, SearchServerConfig serverConfig, Query query, ScriptEnv env)
    {
        this.serverConfig = serverConfig;
        env.setConfiguration(config);
        ScriptEnv newEnv = new ScriptEnv(env);
        newEnv.setSearchServerConfig(serverConfig);
        query.accept(this, newEnv);
    }

    @Override
    public void init(QueryMapperFactory config) throws InvocationTargetException, IllegalAccessException
    {
        // NOP
    }

    public static class Factory
    {
        public static QueryMapperIfc getInstance()
        {
            return new QueryMapper();
        }
    }

    // ---- Visitor methods --------------------------------------------------------------------------------------------

    @Override
    public void visitQuery(TermQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, t.getBoostValue(), t);
    }

    protected boolean visitQuery(Term t, ScriptEnv env, Float boost, TermQuery tq)
    {
        String fusionFieldName = t.getFusionFieldName();
        List<FieldMapping> mappings = serverConfig.findAllMappingsForFusionField(fusionFieldName);
        if (mappings.isEmpty())
        {
            if (noMappingPolicy.equals(NO_MAPPING_THROW_EXCEPTION))
            {
                throw new MissingFusionFieldMapping("Found no mapping for fusion field '" + fusionFieldName + "'");
            }
            else if (noMappingPolicy.equals(NO_MAPPING_DELETE))
            {
                t.setRemoved(true);
                log.warn("Found no mapping for fusion field '{}'. Deleting query part.", fusionFieldName);
            }
        }
        ScriptEnv newEnv = new ScriptEnv(env);
        newEnv.setBinding(ScriptEnv.ENV_IN_TERM_QUERY_PART, tq);
        for (FieldMapping m : mappings)
        {
            m.applyQueryMappings(t, newEnv);
        }
        t.setProcessed(true);
        return true;
    }

    @Override
    public void visitQuery(BooleanQuery t, ScriptEnv env)
    {
        t.visitQueryClauses(this, env);
    }

    @Override
    public void visitQuery(FuzzyQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }

    @Override
    public void visitQuery(MatchAllDocsQuery t, ScriptEnv env)
    {
        // NOP
    }

    @Override
    public void visitQuery(PhraseQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }

    @Override
    public void visitQuery(PrefixQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }

    @Override
    public void visitQuery(WildcardQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }

    @Override
    public void visitQuery(BooleanClause booleanClause, ScriptEnv env)
    {
        booleanClause.accept(this, env);
    }

    @Override
    public void visitQuery(IntRangeQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }

    @Override
    public void visitQuery(LongRangeQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }

    @Override
    public void visitQuery(FloatRangeQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }

    @Override
    public void visitQuery(DoubleRangeQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }

    @Override
    public void visitQuery(DateRangeQuery t, ScriptEnv env)
    {
        visitQuery(t.getTerm(), env, null, t);
    }
}
