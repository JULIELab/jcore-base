package de.julielab.jcore.consumer.es;

import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.PreanalyzedFieldValue;
import de.julielab.jcore.consumer.es.preanalyzed.RawToken;

import java.util.ArrayList;
import java.util.List;

public class ArrayFieldValue extends ArrayList<IFieldValue> implements
        IFieldValue {

    /**
     *
     */
    private static final long serialVersionUID = -2649494049423945160L;

    public <T extends IFieldValue> ArrayFieldValue(List<T> fieldValues) {
        for (T fieldValue : fieldValues)
            add(fieldValue);
    }

    public ArrayFieldValue(Object... values) {
        for (Object v : values) {
            if (v instanceof IFieldValue) {
                add((IFieldValue) v);
            }
            else {
                add(new RawToken(v));
            }
        }
    }

    public static ArrayFieldValue of(Object... values) {
        return new ArrayFieldValue(values);
    }

    public ArrayFieldValue() {
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.ARRAY;
    }

    /**
     * Adds <tt>fieldValue</tt> to this array. If <tt>fieldValue</tt> is an
     * array itself, its elements will be added to this array. This way a
     * multidimensional array is avoided.
     *
     * @param fieldValue
     */
    public void addFlattened(IFieldValue fieldValue) {
        if (null == fieldValue)
            return;
        if (ArrayFieldValue.class.equals(fieldValue.getClass()))
            addAll((ArrayFieldValue) fieldValue);
        else if (RawToken.class.equals(fieldValue.getClass()))
            add(fieldValue);
        else if (PreanalyzedFieldValue.class.equals(fieldValue.getClass()))
            add(fieldValue);
        else
            throw new IllegalArgumentException("FieldValue class "
                    + fieldValue.getClass() + " is currently not supported.");

    }

    public ArrayFieldValue flatten() {
        List<RawToken> rawTokens = new ArrayList<>();
        collectRawTokens(this, rawTokens);
        return new ArrayFieldValue(rawTokens);
    }

    /**
     * <p>Traverses the input array recursively, adding all the values of the RawTokens in the input array.</p>
     * @param input
     * @param values
     */
    private void collectRawTokens(ArrayFieldValue input, List<RawToken> values) {
        for (IFieldValue fv : input) {
            if (fv instanceof ArrayFieldValue)
                collectRawTokens((ArrayFieldValue) fv, values);
            else if (fv instanceof  RawToken)
                values.add((RawToken) fv);
            else
                throw new IllegalArgumentException("Unsupported field value type " + fv.getClass());
        }
    }

}
