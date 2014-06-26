package org.outermedia.solrfusion.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.outermedia.solrfusion.mapper.Term;
import org.outermedia.solrfusion.mapper.UndeclaredFusionField;
import org.outermedia.solrfusion.types.ScriptEnv;

import javax.xml.bind.UnmarshalException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Data holder to store change operation configurations.
 *
 * @author ballmann
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "changeOperation", namespace = "http://solrfusion.outermedia.org/configuration/", propOrder =
        {
                "targets"
        })
@Getter
@Setter
@ToString(callSuper = true)
public class ChangeOperation extends Operation
{
    @Override
    public void applyAllResponseOperations(Term term, ScriptEnv env)
    {
        if (term.getFusionField() == null)
        {
            throw new UndeclaredFusionField("Didn't find field '" + term.getFusionFieldName()
                    + "' in fusion schema. Please define it there.");
        }
        super.applyAllResponseOperations(term, env);
    }

    @Override
    protected void check(FieldMapping fieldMapping) throws UnmarshalException
    {
        // NOP
    }

    @Override
    public void applyAllQueryOperations(Term term, ScriptEnv env)
    {
        if (term.getFusionField() == null)
        {
            throw new UndeclaredFusionField("Didn't find field '" + term.getFusionFieldName()
                    + "' in fusion schema. Please define it ther.");
        }
        super.applyAllQueryOperations(term, env);
    }
}
