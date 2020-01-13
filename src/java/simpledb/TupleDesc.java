package simpledb;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /**
     * A help class to facilitate organizing the information of each field
     * */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         * */
        public final Type fieldType;

        /**
         * The name of the field
         * */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
        public boolean equals(Object o){
            if(o==null||o.getClass()!=getClass()) return false;
            if(!(o instanceof TDItem)) return false;
            TDItem td=(TDItem) o;
            return Objects.equals(this.fieldName,td.fieldName) && Objects.equals(this.fieldType,td.fieldType);
        }
    }
    private final TDItem[] items;
    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */

    public Iterator<TDItem> iterator() {
        // some code goes here
        return Arrays.asList(items).iterator();
    }

    private static final long serialVersionUID = 1L;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // some code goes here
        items=new TDItem[typeAr.length];
        for (int i=0;i<typeAr.length;i++){
            TDItem tp=new TDItem(typeAr[i],fieldAr[i]);
            items[i]=tp;
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     *
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        // some code goes here
        items=new TDItem[typeAr.length];
        for (int i=0;i<typeAr.length;i++){
            TDItem tp=new TDItem(typeAr[i],"");
            items[i]=tp;
        }

    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        // some code goes here
        return items.length;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        // some code goes here
        if (i>=items.length||i<0){
            throw new NoSuchElementException();
        }
        return items[i].toString();
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        // some code goes here
        if (i>=items.length||i<0){
            throw new NoSuchElementException();
        }
        return items[i].fieldType;
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        // some code goes here
        int i=0;
        while(i!=items.length){
            if(items[i].fieldName.equals(name)){
                return i;
            }
            i++;
        }
        throw new NoSuchElementException();
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
        // some code goes here
        int size=0;
        for(int i=0;i<numFields();i++){
            size+=getFieldType(i).getLen();
        }
        return size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     *
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        // some code goes here
        Type[] m=new Type[td1.numFields()+td2.numFields()];
        String[] n=new String[td1.numFields()+td2.numFields()];
        int td3len=td1.items.length+td2.items.length;
        for(int i=0; i<td3len;i++){
            if(i-td1.items.length<0){
                m[i]=td1.items[i].fieldType;
                n[i]=td1.items[i].fieldName;
            }
            else{
                m[i]=td2.items[i-td1.items.length].fieldType;
                n[i]=td2.items[i-td1.items.length].fieldName;
            }

        }


        TupleDesc result=new TupleDesc(m,n);
        return result;
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i.
     *
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {
        // some code goes here
        if(o==null||o.getClass()!=getClass()) return false;
        if(!(o instanceof TupleDesc)) return false;
        else {
            TupleDesc tp=(TupleDesc) o;
            return Arrays.equals(tp.items,this.items);
        }

    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     *
     * @return String describing this descriptor.
     */
    public String toString() {
        // some code goes here
        String result="";
        for(int i=0;i<items.length;i++){
            if(i==items.length-1){
                result+= items[i].fieldType+"("+items[i].fieldName+")";
            }
            else{
                result+= items[i].fieldType+"("+items[i].fieldName+")"+",";
            }
        }
        return result;
    }
}
