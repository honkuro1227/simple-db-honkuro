package simpledb;

import java.io.Serializable;
import java.util.Objects;

/** Unique identifier for HeapPage objects. */
public class HeapPageId implements PageId, Serializable {

    /**
     * Constructor. Create a page id structure for a specific page of a
     * specific table.
     *
     * @param tableId The table that is being referenced
     * @param pgNo The page number in that table.
     */
    private int PageNo;
    private int Tableid;
    public HeapPageId(int tableId, int pgNo) {
        // some code goes here
        PageNo=pgNo;
        Tableid=tableId;
    }

    /** @return the table associated with this PageId */
    public int getTableId() {
        // some code goes here
        return Tableid;
    }

    /**
     * @return the page number in the table getTableId() associated with
     *   this PageId
     */
    public int getPageNumber() {
        // some code goes here
        return PageNo;
    }

    /**
     * @return a hash code for this page, represented by the concatenation of
     *   the table number and the page number (needed if a PageId is used as a
     *   key in a hash table in the BufferPool, for example.)
     * @see BufferPool
     */
    public int hashCode() {
        // some code goes here
        return this.getTableId()+this.getPageNumber();
    }

    /**
     * Compares one PageId to another.
     *
     * @param o The object to compare against (must be a PageId)
     * @return true if the objects are equal (e.g., page numbers and table
     *   ids are the same)
     */
    public boolean equals(Object o) {
        // some code goes here
        if(o==null||o.getClass()!=getClass())return false;
        if(o instanceof HeapPageId){
            HeapPageId cp= (HeapPageId) o;
            return Objects.equals(this.getTableId(),cp.getTableId())&& Objects.equals(this.getPageNumber(),cp.getPageNumber());
        }
        return false;
    }
    /**
     *  Return a representation of this object as an array of
     *  integers, for writing to disk.  Size of returned array must contain
     *  number of integers that corresponds to number of args to one of the
     *  constructors.
     */
    public int[] serialize() {
        int data[] = new int[2];

        data[0] = getTableId();
        data[1] = getPageNumber();

        return data;
    }

}
