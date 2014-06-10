package org.outermedia.solrfusion.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.outermedia.solrfusion.mapper.Term;
import org.outermedia.solrfusion.types.ScriptEnv;

import javax.xml.bind.annotation.*;
import java.util.List;


/**
 * Data holder class to store one field mapping configuration.
 *
 * @author ballmann
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fieldMapping", namespace = "http://solrfusion.outermedia.org/configuration/", propOrder =
        {
                "operations"
        })
@Getter
@Setter
@ToString
public class FieldMapping
{
    @XmlAttribute(name = "name")
    private String searchServersName;

    @XmlAttribute(name = "fusion-name")
    private String fusionName;

    @XmlElements(value =
            {
                    @XmlElement(name = "add", type = AddOperation.class, namespace = "http://solrfusion.outermedia.org/configuration/"),
                    @XmlElement(name = "drop", type = DropOperation.class, namespace = "http://solrfusion.outermedia.org/configuration/"),
                    @XmlElement(name = "change", type = ChangeOperation.class, namespace = "http://solrfusion.outermedia.org/configuration/")
            })
    private List<Operation> operations;

    public boolean applicableToFusionField(String fusionFieldName)
    {
        // TODO implement wildcard ("*") and regexp matching
        return fusionFieldName.equals(this.fusionName);
    }

    public void applyQueryMappings(Term term, ScriptEnv env)
    {
        ScriptEnv newEnv = new ScriptEnv(env);
        newEnv.setBinding(ScriptEnv.ENV_FUSION_FIELD, fusionName);
        newEnv.setBinding(ScriptEnv.ENV_SEARCH_SERVER_FIELD, searchServersName);
        newEnv.setBinding(ScriptEnv.ENV_FUSION_FIELD_DECLARATION, term.getFusionField());
        // initialize term with mapped name and original fusion value
        // TODO implement wildcard ("*") and regexp matching for term.setSearchServerFieldName() call
        term.setSearchServerFieldName(searchServersName);
        term.setSearchServerFieldValue(term.getFusionFieldValue());
        term.setWasMapped(true);
        if (operations != null)
        {
            for (Operation o : operations)
            {
                o.applyAllQueryOperations(term, newEnv);
            }
        }
    }

    public boolean applicableToSearchServerField(String searchServerFieldName)
    {
        // TODO implement wildcard ("*") and regexp matching
        return searchServerFieldName.equals(this.searchServersName);
    }

    public void applyResponseMappings(List<Term> terms, ScriptEnv env, FusionField fusionField)
    {
        for (Term t : terms)
        {
            applyResponseMappings(t, env, fusionField);
        }
    }

    public void applyResponseMappings(Term term, ScriptEnv env, FusionField fusionField)
    {
        term.setFusionField(fusionField);
        ScriptEnv newEnv = new ScriptEnv(env);
        newEnv.setBinding(ScriptEnv.ENV_FUSION_FIELD, fusionName);
        newEnv.setBinding(ScriptEnv.ENV_SEARCH_SERVER_FIELD, searchServersName);
        newEnv.setBinding(ScriptEnv.ENV_FUSION_FIELD_DECLARATION, term.getFusionField());
        // initialize term with mapped name and original search server value
        // TODO implement wildcard ("*") and regexp matching for term.setSearchServerFieldName() call
        term.setFusionFieldName(fusionName);
        term.setFusionFieldValue(term.getSearchServerFieldValue());
        term.setWasMapped(true);
        if (operations != null)
        {
            for (Operation o : operations)
            {
                o.applyAllResponseOperations(term, newEnv);
            }
        }
    }
}
