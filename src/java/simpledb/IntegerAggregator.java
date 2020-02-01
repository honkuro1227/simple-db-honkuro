package simpledb;
import java.util.HashMap;
import java.util.ArrayList;
/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     *
     * @param gbfield
     * the 0-based index of the group-by field in the tuple, or
     * NO_GROUPING if there is no grouping
     * @param gbfieldtype
     * the type of the group by field (e.g., Type.INT_TYPE), or null
     * if there is no grouping
     * @param afield
     * the 0-based index of the aggregate field in the tuple
     * @param what
     * the aggregation operator
     */
    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;
    private HashMap<Field, Integer> map;
    private HashMap<Field, Integer> countTime;
    private String Name;

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.what = what;
        this.gbfieldtype = gbfieldtype;
        this.gbfield = gbfield;
        this.afield = afield;
        this.map=new HashMap<Field,Integer>();
        this.countTime=new HashMap<Field,Integer>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
//        MIN, MAX, SUM, AVG, COUNT,

        Field groupField;
        IntField aggregate = (IntField) tup.getField(afield);
        if (gbfield == NO_GROUPING) {
            groupField = new IntField(0);
            Name=null;
        } else {
            groupField = tup.getField(gbfield);
            Name= tup.getTupleDesc().getFieldName(gbfield);
        }
        //System.out.println(groupField+aggregate.toString());

        if (!map.containsKey(groupField)) {
            map.put(groupField, aggregate.getValue());
            countTime.merge(groupField, 1, Integer::sum);

        } else {

            if (what.name() == "MAX") {
                map.put(groupField, Math.max(map.get(groupField), aggregate.getValue()));
            }
            if (what.name() == "SUM") {
                map.put(groupField, map.get(groupField) + aggregate.getValue());
            }
            if (what.name() == "MIN") {
                map.put(groupField, Math.min(map.get(groupField), aggregate.getValue()));
            }
            if (what.name() == "COUNT") {
                map.merge(groupField, 1, Integer::sum);
            }
            if (what.name() == "AVG") {
                //System.out.println(aggregate.getValue());
                countTime.merge(groupField, 1, Integer::sum);
                map.put(groupField, map.get(groupField) + aggregate.getValue());
            }


        }

    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     * if using group, or a single (aggregateVal) if no grouping. The
     * aggregateVal is determined by the type of aggregate specified in
     * the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        System.out.print(map.keySet());
        System.out.print(map.values()+"\n");
        System.out.print(countTime.keySet());
        System.out.print(countTime.values()+"\n");
        TupleDesc tupleDesc;
        ArrayList<Tuple> tuples;
        if(gbfield==NO_GROUPING){
            tupleDesc = new TupleDesc(
                    new Type[]{gbfieldtype, Type.INT_TYPE},
                    new String[]{Name, what.toString()});
        }
        else {
            tupleDesc = new TupleDesc(
                    new Type[]{Type.INT_TYPE},
                    new String[]{what.toString()});
        }
        tuples = new ArrayList<Tuple>();
        for (Field group : map.keySet()) {
            Tuple tuple = new Tuple(tupleDesc);
            if (gbfield!=NO_GROUPING) {
                tuple.setField(0, group);
            }
            if(gbfield!=NO_GROUPING){
            }
            else{
            }
            tuples.add(tuple);
        }


        return new TupleIterator(tupleDesc, tuples);
    }
}