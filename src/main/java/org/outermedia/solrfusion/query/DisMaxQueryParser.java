package org.outermedia.solrfusion.query;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.outermedia.solrfusion.configuration.Configuration;
import org.outermedia.solrfusion.configuration.QueryParserFactory;
import org.outermedia.solrfusion.query.dismaxparser.DismaxQueryParser;
import org.outermedia.solrfusion.query.parser.ParseException;
import org.outermedia.solrfusion.query.parser.Query;
import org.outermedia.solrfusion.query.parser.Operator;

import java.util.Locale;
import java.util.Map;

/**
 * A common solr query parser.
 *
 * @author ballmann
 */

@ToString
@Slf4j
@Getter
@Setter
public class DisMaxQueryParser implements QueryParserIfc
{

    /**
     * Factory creates instances only.
     */
    protected DisMaxQueryParser()
    {
    }

    public static class Factory
    {
        public static DisMaxQueryParser getInstance()
        {
            return new DisMaxQueryParser();
        }
    }

    @Override
    public void init(QueryParserFactory config)
    {
        // NOP

    }

    @Override
    public Query parse(Configuration config, Map<String, Float> boosts, String queryString, Locale locale,
        Boolean allTermsAreProcessed) throws ParseException
    {
        Query result = null;
        if (queryString != null && queryString.trim().length() > 0)
        {
            String defaultOpStr = config.getDefaultOperator();
            Operator defaultOp = Operator.AND;
            try
            {
                defaultOp = Operator.valueOf(defaultOpStr);
            }
            catch (Exception e)
            {
                log.error("Found illegal default operator '{}'. Expected either 'or' or 'and'. Using {}.", defaultOpStr,
                    defaultOp, e);
            }
            DismaxQueryParser parser = new DismaxQueryParser(config.getDefaultSearchField(), config, boosts, defaultOp,
                allTermsAreProcessed);
            parser.setLocale(locale);
            result = parser.parse(queryString);
        }
        return result;
    }
}