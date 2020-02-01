package simpledb;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    private final int gbfield;
    private final Type gbfieldtype;
    private final int afield;
    private final Op what;
    private final HashMap<Field, Integer> map;
    private String NameofGroupField;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what)
            throws IllegalArgumentException{
        if (what != Op.COUNT) {
            throw new IllegalArgumentException();
        }
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.map = new HashMap<Field, Integer>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        if(gbfield!=NO_GROUPING){
            NameofGroupField = tup.getTupleDesc().getFieldName(gbfield);
            Field groupField = tup.getField(gbfield);
            map.merge(groupField,1,Integer::sum);
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */

    public OpIterator iterator() {
        if (gbfield != NO_GROUPING) {
            TupleDesc tupleDesc = new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE}, new String[]{NameofGroupField, what.toString()});
            ArrayList<Tuple> tuples = new ArrayList<Tuple>();
            for (Field group : map.keySet()) {
                Tuple tuple = new Tuple(tupleDesc);
                tuple.setField(0, group);
                tuple.setField(1, new IntField(map.get(group)));
                tuples.add(tuple);
            }
            return new TupleIterator(tupleDesc, tuples);
        } else {
            TupleDesc tupleDesc = new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{what.toString()});
            Tuple tuple = new Tuple(tupleDesc);
            ArrayList<Tuple> tuples = new ArrayList<Tuple>();
            tuples.add(tuple);
            return new TupleIterator(tupleDesc, tuples);
        }
    }

}