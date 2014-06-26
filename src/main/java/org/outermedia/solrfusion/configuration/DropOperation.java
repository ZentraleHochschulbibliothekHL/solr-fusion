package org.outermedia.solrfusion.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.outermedia.solrfusion.mapper.Term;
import org.outermedia.solrfusion.types.ScriptEnv;

import javax.xml.bind.UnmarshalException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * Data holder to store drop operation configurations.
 *
 * @author ballmann
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dropOperation", namespace = "http://solrfusion.outermedia.org/configuration/", propOrder =
        {
                "targets"
        })
@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
public class DropOperation extends Operation
{
    @Override
    public void applyAllQueryOperations(Term term, ScriptEnv env)
    {
        List<Target> queryTargets = getQueryTargets();
        if (!queryTargets.isEmpty())
        {
            term.setRemoved(true);
            term.setSearchServerFieldValue(null);
        }
    }

    @Override
    public void applyAllResponseOperations(Term term, ScriptEnv env)
    {
        List<Target> responseTargets = getResponseTargets();
        if (!responseTargets.isEmpty())
        {
            term.setRemoved(true);
            term.setFusionFieldValue(null);
        }
    }

    @Override
    public void check(FieldMapping fieldMapping) throws UnmarshalException
    {
        String msg = null;

        // public void applyAllQueryOperations(Term term, ScriptEnv env)
        if (fieldMapping.isFusionFieldOnlyMapping())
        {
            List<Response> responseTargets = getResponseOnlyTargets();
            if (responseTargets.size() > 0)
            {
                msg = "Invalid configuration: It is impossible to remove a fusion field from a search server's " +
                        "response (<om:field fusion-name=\"...\"><om:drop><om:response/>). Use <om:query/> instead or " +
                        "change the fusion-name attribute to name. Please fix the fusion schema:\n" + fieldMapping;
                log.error(msg);
            }
            List<Target> queryTargets = getQueryTargets();
            if (queryTargets.isEmpty())
            {
                msg = "Invalid configuration: Found <om:drop> without <om:query> or <om:query-response> target:\n" +
                        fieldMapping;
                log.error(msg);
            }
        }

        // public void applyAllResponseOperations(Term term, ScriptEnv env)
        if (fieldMapping.isSearchServerFieldOnlyMapping())
        {
            List<Query> queries = getQueryOnlyTargets();
            if (queries.size() > 0)
            {
                msg = "Invalid configuration: It is impossible to remove a search server field " +
                        "from a fusion query (<om:field name=\"...\"><om:drop><om:query/>). Use <om:response/> instead " +
                        "or change the name attribute to fusion-name. Please fix the fusion schema:\n" + fieldMapping;
                log.error(msg);
            }
            List<Target> targets = getResponseTargets();
            if (targets.isEmpty())
            {
                msg = "Invalid configuration: Found <om:drop> without <om:response> or <om:query-response> target:\n" +
                        fieldMapping;
                log.error(msg);
            }
        }
        if (msg != null)
        {
            throw new UnmarshalException(msg);
        }
    }
}
