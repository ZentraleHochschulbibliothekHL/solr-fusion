package org.outermedia.solrfusion.mapper;

import lombok.extern.slf4j.Slf4j;
import org.outermedia.solrfusion.IdGeneratorIfc;
import org.outermedia.solrfusion.ScoreCorrectorIfc;
import org.outermedia.solrfusion.configuration.Configuration;
import org.outermedia.solrfusion.configuration.FieldMapping;
import org.outermedia.solrfusion.configuration.FusionField;
import org.outermedia.solrfusion.configuration.SearchServerConfig;
import org.outermedia.solrfusion.response.parser.Document;
import org.outermedia.solrfusion.response.parser.FieldVisitor;
import org.outermedia.solrfusion.response.parser.SolrMultiValuedField;
import org.outermedia.solrfusion.response.parser.SolrSingleValuedField;
import org.outermedia.solrfusion.types.ScriptEnv;

import java.util.List;

/**
 * Created by ballmann on 04.06.14.
 */
@Slf4j
public class ResponseMapper implements FieldVisitor
{
    protected static final String DOC_FIELD_NAME_SCORE = "score";
    private SearchServerConfig serverConfig;
    private Document doc;

    /**
     * Map a response of a certain search server (serverConfig) to the fusion fields.
     *
     * @param config       the whole configuration
     * @param serverConfig the currently used server's configuration
     * @param doc          one response document to process
     * @param env          the environment needed by the scripts which transform values
     */
    public void mapResponse(Configuration config, SearchServerConfig serverConfig, Document doc, ScriptEnv env)
    {
        this.serverConfig = serverConfig;
        this.doc = doc;
        env.setConfiguration(config);
        doc.accept(this, env);
        setFusionDocId(config, doc);
        correctScore(doc);
    }

    protected void correctScore(Document doc)
    {
        Term scoreTerm = doc.getFieldTermByName(DOC_FIELD_NAME_SCORE);
        if (scoreTerm != null)
        {
            ScoreCorrectorIfc scoreCorrector = serverConfig.getScoreCorrector();
            // if mapped use this value instead of search server's value
            String searchServerScoreStr = scoreTerm.getFusionFieldValue();
            if (searchServerScoreStr == null)
            {
                searchServerScoreStr = scoreTerm.getSearchServerFieldValue();
            }
            try
            {
                double searchServerScore = Double.parseDouble(searchServerScoreStr);
                double newScore = scoreCorrector.applyCorrection(searchServerScore);
                scoreTerm.setFusionFieldName(DOC_FIELD_NAME_SCORE);
                scoreTerm.setFusionFieldValue(String.valueOf(newScore));
            }
            catch (Exception e)
            {
                log.warn("Can't parse double value '{}'. score is not corrected and not set.", searchServerScoreStr, e);
            }
        }
        else
        {
            log.warn("Can't correct score in documents, because document contains no value (any more).");
        }
    }

    protected List<FieldMapping> getFieldMappings(String searchServerFieldName)
    {
        List<FieldMapping> mappings = serverConfig.findAllMappingsForSearchServerField(searchServerFieldName);
        if (mappings.isEmpty())
        {
            throw new MissingSearchServerFieldMapping("\"Found no mapping for fusion field '\" " +
                    "+ searchServerFieldName + \"'\"");
        }
        return mappings;
    }

    protected FusionField getFusionField(ScriptEnv env, FieldMapping m)
    {
        FusionField fusionField = env.getConfiguration().findFieldByName(m.getFusionName());
        if (fusionField == null)
        {
            throw new UndeclaredFusionField("Didn't find field '" + m.getFusionName()
                    + "' in fusion schema. Please define it their.");
        }
        return fusionField;
    }

    protected void setFusionDocId(Configuration config, Document doc)
    {
        IdGeneratorIfc idGenerator = config.getIdGenerator();
        Term idTerm = doc.getFieldTermByName(serverConfig.getIdFieldName());
        if (idTerm == null || idTerm.getSearchServerFieldValue() == null)
        {
            throw new RuntimeException("Found no id (" + serverConfig.getIdFieldName() + ") in response of server '"
                    + serverConfig.getSearchServerName() + "'");
        }
        String id = idGenerator.computeId(serverConfig.getSearchServerName(), idTerm.getSearchServerFieldValue());
        idTerm.setFusionFieldName(idGenerator.getFusionIdField());
        idTerm.setFusionFieldValue(id);
    }

    // ---- Visitor methods --------------------------------------------------------------------------------------------

    @Override
    public boolean visitField(SolrSingleValuedField sf, ScriptEnv env)
    {
        List<FieldMapping> mappings = getFieldMappings(sf.getFieldName());
        Term t = sf.getTerm();
        for (FieldMapping m : mappings)
        {
            m.applyResponseMappings(t, env, getFusionField(env, m));
        }
        // always continue visiting
        return true;
    }

    @Override
    public boolean visitField(SolrMultiValuedField sf, ScriptEnv env)
    {
        List<FieldMapping> mappings = getFieldMappings(sf.getFieldName());
        List<Term> terms = sf.getTerms();
        for (FieldMapping m : mappings)
        {
            m.applyResponseMappings(terms, env, getFusionField(env, m));
        }
        // always continue visiting
        return true;
    }

}
