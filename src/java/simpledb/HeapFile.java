package simpledb;

import javax.xml.crypto.Data;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see simpledb.HeapPage#HeapPage
 */
public class HeapFile implements DbFile {

    private File f;
    private TupleDesc td;


    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.f = f;
        this.td = td;

    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return this.f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        int id=this.f.getAbsoluteFile().hashCode();
        return id;
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return this.td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        int tableId = pid.getTableId();
        if (tableId != this.getId()) {
            return null;
        }
        byte[] data = new byte[BufferPool.getPageSize()];
        int position = pid.getPageNumber()*BufferPool.getPageSize();
        try{
            Page page = new HeapPage((HeapPageId) pid, HeapPage.createEmptyPageData());
            RandomAccessFile raf=new RandomAccessFile(this.f,"r");
            raf.seek(position);
            raf.read(data);
            HeapPageId hid = new HeapPageId(tableId, pid.getPageNumber());
            if (pid.getPageNumber() == numPages()) {

                writePage(page);
                return page;
            }
            return new HeapPage(hid, data);
        }
        catch (IOException e){

        }
        return null;

    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
        RandomAccessFile randomAccessFile;
        randomAccessFile = new RandomAccessFile(f, "rw");
        randomAccessFile.seek(BufferPool.getPageSize() * page.getId().getPageNumber());
        randomAccessFile.write(page.getPageData());
        randomAccessFile.close();

    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        return (int) f.length() / BufferPool.getPageSize();
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        for (int i=0;i<numPages();i++){
            HeapPageId now=new HeapPageId(getId(),i);
            HeapPage op= null;

                op = (HeapPage) Database.getBufferPool().getPage(tid,now, Permissions.READ_ONLY);

            if(op.getNumEmptySlots()>0){
                    op = (HeapPage) Database.getBufferPool().getPage(tid, now, Permissions.READ_WRITE);

                op.insertTuple(t);
                op.markDirty(true,tid);
                return new ArrayList<Page>(Arrays.asList(op));
            }


        }

        HeapPageId pid = new HeapPageId(getId(), numPages());
        HeapPage page = null;
        page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
        page.insertTuple(t);
        page.markDirty(true,tid);
        return new ArrayList<Page>(Arrays.asList(page));

        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {

        // some code goes here
        if (t.getRecordId() == null) {
            throw new DbException("Heap file delete tuple, record id is null");
        }
        PageId pageId = t.getRecordId().getPageId();
        HeapPage page = null;
        page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
        page.deleteTuple(t);
        page.markDirty(true,tid);
        return new ArrayList<Page>(Arrays.asList(page));
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new DbFileIterator() {
            int currentPageNo;
            Iterator<Tuple> tupleItr;

            @Override
            public void open() throws DbException, TransactionAbortedException {
                currentPageNo = 0;
                PageId pid = new HeapPageId(getId(), 0);
                HeapPage page = null;

                    page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);

                this.tupleItr =  page.iterator();
            }
            @Override
            public boolean hasNext() throws DbException, TransactionAbortedException {
                if (this.tupleItr == null) {
                    return false;
                }

                while (!tupleItr.hasNext()) {
                    currentPageNo++;
                    if (currentPageNo >= numPages()) {
                        return false;
                    }
                    PageId pid=new HeapPageId(getId(),currentPageNo);
                    HeapPage page= null;
                    page = (HeapPage) Database.getBufferPool().getPage(tid,pid, Permissions.READ_ONLY);
                    tupleItr = page.iterator();
                }
                return true;

            }

            @Override
            public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return this.tupleItr.next();
            }

            @Override
            public void rewind() throws DbException, TransactionAbortedException {
                open();
            }
            @Override
            public void close() {
                this.tupleItr = null;
            }
        };
    }

}

