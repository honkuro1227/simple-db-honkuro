package simpledb;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;
    private static int pageSize = DEFAULT_PAGE_SIZE;
    private Page[] pages;
    private ConcurrentHashMap<PageId,Page> cache;

    /** Default number of pages passed to the constructor. This is used by
     other classes. BufferPool should use the numPages argument to the
     constructor instead. */
    public static final int DEFAULT_PAGES = 50;

    //lab3
    private final LockManager lockManager;
    private final Object LOCK;
    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        pages=new Page [numPages] ;
        cache=new ConcurrentHashMap<PageId,Page>();
        //lab3
        lockManager = new LockManager();
        LOCK = new Object();
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException  {
        // some code goes here
        boolean result;
        if (perm == Permissions.READ_ONLY) {
            result=lockManager.grantRlock(tid, pid);
        }
        else{
            result=lockManager.grantWlock(tid, pid);
        }
        while (!result) {

            if (lockManager.deadlockOccurred(tid, pid)) {
                throw new TransactionAbortedException();
            }
            try{
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            result = (perm == Permissions.READ_ONLY) ? lockManager.grantWlock(tid, pid)
                    : lockManager.grantRlock(tid, pid);
        }
        if(this.cache.containsKey(pid)){
            return cache.get(pid);
        }
        if(cache.size()>=this.pages.length){
            evictPage();
        }

            DbFile dbf= Database.getCatalog().getDatabaseFile(pid.getTableId());
            Page page = dbf.readPage(pid);
            cache.put(pid, page);
            return page;

    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
        //part1
        if (!lockManager.unlock(tid, pid)) {
            //pid does not locked by any transaction
            //or tid  dose not lock the page pid
            throw new IllegalArgumentException();
        }
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        transactionComplete(tid, true);
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        //part1
        return lockManager.getLockProfile(tid, p) != null;

    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        lockManager.releaseTransactionLocks(tid);
        if (commit==true) {
            for (Page page : cache.values()) {
//                if(page.isDirty()!=null&&page.isDirty().equals(tid)){
                    flushPages(tid);
//                    Database.getLogFile().logWrite(tid, page.getBeforeImage(), page);
                    page.setBeforeImage();
//                }
            }
        }
        else{
            for (Page page : cache.values()) {
//                if(page.isDirty()!=null&&page.isDirty().equals(tid)){
                    cache.put(page.getId(),page.getBeforeImage());
//                }
            }
        }
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1

        DbFile file = Database.getCatalog().getDatabaseFile(tableId);
        ArrayList<Page> pages = file.insertTuple(tid, t);
        pages.forEach (page -> {page.markDirty(true,tid);
            cache.put(page.getId(), page);
        });


    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        DbFile file = Database.getCatalog().getDatabaseFile(t.getRecordId().getPageId().getTableId());
        ArrayList<Page> pages = file.deleteTuple(tid, t);
        pages.forEach (page ->page.markDirty(true, tid));
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for (PageId pid : cache.keySet()) {
            flushPage(pid);
        }
    }

    /** Remove the specific page id from the buffer pool.
     Needed by the recovery manager to ensure that the
     buffer pool doesn't keep a rolled back page in its
     cache.

     Also used by B+ tree files to ensure that deleted pages
     are removed from the cache so they can be reused safely
     */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
        cache.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        HeapPage dirty_page = (HeapPage) cache.get(pid);
        HeapFile table = (HeapFile) Database.getCatalog().getDatabaseFile(pid.getTableId());
        TransactionId dirtier = dirty_page.isDirty();
        if (dirtier != null){
            Database.getLogFile().logWrite(dirtier, dirty_page.getBeforeImage(), dirty_page);
            Database.getLogFile().force();
        }
        table.writePage(dirty_page);
        dirty_page.markDirty(false, null);
    }
    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        for (Page page :cache.values()){
            if(page.isDirty()!=null && page.isDirty().equals(tid)){
                flushPage(page.getId());
            }
        }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1
        // No steal policy
        /**
         * In particular, it must never evict a dirty page.
         * If your eviction policy prefers a dirty page for eviction,
         * you will have to find a way to evict an alternative page.
         * In the case where all pages in the buffer pool are dirty,
         * you should throw a DbException.
         */
        boolean isPageDirty =true;
        int dirtyPageCount=0;
        for (PageId key : cache.keySet()) {
            if (cache.get(key).isDirty()!=null){
                dirtyPageCount++;
            }
        }
        if(dirtyPageCount>=this.pages.length){
            throw new DbException("All pages are dirty, cannot evict");
        }
        for (PageId key : cache.keySet()) {
//flush all page
                try{
                    flushPage(key);
                    cache.remove(key);
                }
                catch (IOException e){
                    e.printStackTrace();
                }

        }
    }

}
